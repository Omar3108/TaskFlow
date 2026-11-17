package com.taskflow.card;

import com.taskflow.board.BoardAccessService;
import com.taskflow.board.BoardColumn;
import com.taskflow.board.BoardColumnRepository;
import com.taskflow.card.dto.CardResponse;
import com.taskflow.card.dto.CreateCardRequest;
import com.taskflow.card.dto.MoveCardRequest;
import com.taskflow.card.dto.UpdateCardRequest;
import com.taskflow.common.error.BadRequestException;
import com.taskflow.common.error.ConflictException;
import com.taskflow.common.error.ResourceNotFoundException;
import com.taskflow.common.ordering.PositionCalculator;
import com.taskflow.user.User;
import com.taskflow.user.UserRepository;
import com.taskflow.workspace.WorkspaceAccessService;
import com.taskflow.workspace.WorkspaceRole;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardService {

    private static final String CARD_NOT_FOUND = "Card not found";

    private static final String INVALID_TARGET = "The target column must belong to the same board as the card";
    private static final String INVALID_NEIGHBOURS =
            "The neighbouring cards must be cards of the target column, in order, and not the moved card";

    private final CardRepository cardRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final UserRepository userRepository;
    private final BoardAccessService boardAccess;
    private final WorkspaceAccessService workspaceAccess;
    private final CardMapper mapper;

    public CardService(
            CardRepository cardRepository,
            BoardColumnRepository boardColumnRepository,
            UserRepository userRepository,
            BoardAccessService boardAccess,
            WorkspaceAccessService workspaceAccess,
            CardMapper mapper) {
        this.cardRepository = cardRepository;
        this.boardColumnRepository = boardColumnRepository;
        this.userRepository = userRepository;
        this.boardAccess = boardAccess;
        this.workspaceAccess = workspaceAccess;
        this.mapper = mapper;
    }

    /** New cards go to the bottom of the column. Any member can create cards. */
    @Transactional
    public CardResponse create(UUID columnId, UUID userId, CreateCardRequest request) {
        BoardColumn column = boardAccess.requireColumn(columnId, userId, WorkspaceRole.MEMBER);
        UUID workspaceId = BoardAccessService.workspaceIdOf(column);

        double position = PositionCalculator.append(cardRepository.findMaxPositionByColumnId(columnId));
        // getReferenceById: a proxy holding only the id, enough to set the foreign key.
        Card card = new Card(column, request.title().trim(), position, userRepository.getReferenceById(userId));
        card.setDescription(normalize(request.description()));
        card.setDueDate(request.dueDate());
        card.setAssignee(resolveAssignee(request.assigneeId(), workspaceId));

        return mapper.toResponse(cardRepository.save(card));
    }

    @Transactional(readOnly = true)
    public CardResponse get(UUID cardId, UUID userId) {
        return mapper.toResponse(requireCard(cardId, userId));
    }

    @Transactional
    public CardResponse update(UUID cardId, UUID userId, UpdateCardRequest request) {
        Card card = requireCard(cardId, userId);

        // Check the version the client read, by hand. Hibernate only compares the version it
        // loaded a few lines above with the row, so it would miss a change made while the user
        // had the card open in the browser; only the version the client sends back reveals it.
        // The remaining race (two requests passing this check together) is caught by @Version at
        // flush time and ends in the same 409 (see GlobalExceptionHandler).
        if (request.version() != card.getVersion()) {
            throw new ConflictException("The card was modified by someone else. Reload it and try again.");
        }

        card.setTitle(request.title().trim());
        card.setDescription(normalize(request.description()));
        card.setDueDate(request.dueDate());
        card.setAssignee(resolveAssignee(request.assigneeId(), BoardAccessService.workspaceIdOf(card.getColumn())));

        // Flush now so @PreUpdate (and later the version increment) is reflected in the response.
        return mapper.toResponse(cardRepository.saveAndFlush(card));
    }

    @Transactional
    public void delete(UUID cardId, UUID userId) {
        cardRepository.delete(requireCard(cardId, userId));
    }

    /**
     * Moves a card within its column or to another column of the same board.
     *
     * <p>The client says where the card was dropped (target column + neighbours); the position
     * is computed here.
     */
    @Transactional
    public CardResponse move(UUID cardId, UUID userId, MoveCardRequest request) {
        Card card = requireCard(cardId, userId);

        UUID boardId = card.getColumn().getBoard().getId();
        BoardColumn target = boardColumnRepository.findById(request.columnId())
                .filter(column -> column.getBoard().getId().equals(boardId))
                .orElseThrow(() -> new BadRequestException(INVALID_TARGET));
        UUID targetId = target.getId();

        // Never trust ids just because they exist: a neighbour must be a card of the target
        // column. A column of the same board is always in the caller's workspace, so a foreign
        // id gets the same generic answer as a nonexistent one.
        Card previous = resolveNeighbour(request.previousCardId(), cardId, targetId);
        Card next = resolveNeighbour(request.nextCardId(), cardId, targetId);

        // Dropped where it already is (same column, already strictly between the neighbours):
        // nothing to write. A write would only touch updated_at and add noise.
        if (card.getColumn().getId().equals(targetId)
                && (previous == null || previous.getPosition() < card.getPosition())
                && (next == null || card.getPosition() < next.getPosition())) {
            return mapper.toResponse(card);
        }

        if (previous != null && next != null) {
            if (previous.getPosition() > next.getPosition()) {
                throw new BadRequestException(INVALID_NEIGHBOURS);
            }
            if (PositionCalculator.isExhausted(previous.getPosition(), next.getPosition())) {
                // Equal positions (two concurrent moves into the same gap) land here too.
                // The rebalance renumbers the column and clears the persistence context, so
                // everything is read again afterwards.
                cardRepository.rebalanceColumn(targetId, PositionCalculator.STEP);
                card = cardRepository.findByIdWithBoard(cardId).orElseThrow();
                previous = cardRepository.findById(previous.getId()).orElseThrow();
                next = cardRepository.findById(next.getId()).orElseThrow();
                target = boardColumnRepository.getReferenceById(targetId);
                // Equal positions are ordered by id, so only now can the real order be checked.
                if (previous.getPosition() >= next.getPosition()) {
                    throw new BadRequestException(INVALID_NEIGHBOURS);
                }
            }
        }

        double position = PositionCalculator.between(
                previous == null ? null : previous.getPosition(), next == null ? null : next.getPosition());
        card.setColumn(target);
        card.setPosition(position);

        // Versioning: column, position and updated_at are excluded from the version (see Card),
        // so a move leaves it unchanged. Bumping it would make the user who has the card open
        // get a 409 on save although the content did not change. It is safe because edits use
        // @DynamicUpdate and never write the position back. Two concurrent moves of the same
        // card are last-write-wins, which is what a drag and drop should do. If two users drop
        // different cards into the same gap both succeed with equal positions; reads order by
        // (position, id), so the result is deterministic and the next move into that gap
        // rebalances the column.
        return mapper.toResponse(cardRepository.saveAndFlush(card));
    }

    private Card resolveNeighbour(UUID neighbourId, UUID movedCardId, UUID targetColumnId) {
        if (neighbourId == null) {
            return null;
        }
        return cardRepository.findById(neighbourId)
                .filter(neighbour -> !neighbour.getId().equals(movedCardId))
                .filter(neighbour -> neighbour.getColumn().getId().equals(targetColumnId))
                .orElseThrow(() -> new BadRequestException(INVALID_NEIGHBOURS));
    }

    /**
     * Loads a card and checks that the caller is a member of its workspace. Every card
     * operation is open to plain members, so there is no minimum-role parameter.
     */
    private Card requireCard(UUID cardId, UUID userId) {
        Card card = cardRepository.findByIdWithBoard(cardId)
                .orElseThrow(() -> new ResourceNotFoundException(CARD_NOT_FOUND));
        workspaceAccess.requireRole(
                BoardAccessService.workspaceIdOf(card.getColumn()), userId, WorkspaceRole.MEMBER, CARD_NOT_FOUND);
        return card;
    }

    /** A card can only be assigned to a member of the workspace it belongs to. */
    private User resolveAssignee(UUID assigneeId, UUID workspaceId) {
        if (assigneeId == null) {
            return null;
        }
        if (!workspaceAccess.isMember(workspaceId, assigneeId)) {
            // Also covers ids that do not exist at all, with the same answer.
            throw new BadRequestException("The assignee must be a member of the workspace");
        }
        // A real SELECT (not a proxy): the response includes the name and e-mail of the assignee.
        return userRepository.findById(assigneeId)
                .orElseThrow(() -> new BadRequestException("The assignee must be a member of the workspace"));
    }

    private static String normalize(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
