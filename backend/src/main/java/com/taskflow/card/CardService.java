package com.taskflow.card;

import com.taskflow.board.BoardAccessService;
import com.taskflow.board.BoardColumn;
import com.taskflow.card.dto.CardResponse;
import com.taskflow.card.dto.CreateCardRequest;
import com.taskflow.card.dto.MoveCardRequest;
import com.taskflow.card.dto.UpdateCardRequest;
import com.taskflow.common.error.BadRequestException;
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

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final BoardAccessService boardAccess;
    private final WorkspaceAccessService workspaceAccess;
    private final CardMapper mapper;

    public CardService(
            CardRepository cardRepository,
            UserRepository userRepository,
            BoardAccessService boardAccess,
            WorkspaceAccessService workspaceAccess,
            CardMapper mapper) {
        this.cardRepository = cardRepository;
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

        // TODO(Omar): optimistic locking, step 2 (step 1 is @Version in Card).
        //  Compare request.version() with card.getVersion() here and reject a stale value with
        //  HTTP 409 before changing anything.
        //  Why by hand: Hibernate compares the version it loaded with the row in the database.
        //  This transaction loaded the card a few lines above, so that check only catches a
        //  change that happens during these few milliseconds. The conflict we care about
        //  happened earlier, while the user had the card open in the browser, and only the
        //  version the client sends back can reveal it. (Calling a setter with the client
        //  version would not work either: Hibernate ignores changes to a @Version field of a
        //  managed entity.)
        //  Step 3: the remaining race (two requests that both pass this check) ends in an
        //  ObjectOptimisticLockingFailureException at flush time; map it to 409 in
        //  GlobalExceptionHandler.

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

        // TODO(Omar): the move logic. Suggested steps:
        //  1. Resolve the target column. It must belong to the same board as the card
        //     (card.getColumn().getBoard().getId()); otherwise 400. A card never changes board.
        //  2. Resolve the neighbours (each may be null). Each one must be a card of the target
        //     column and must not be the moved card itself; otherwise 400. Never trust the ids
        //     just because they exist: a neighbour from another workspace would leak its
        //     position and corrupt the order.
        //  3. If both neighbours are present, check they are in the right order and adjacent
        //     enough for the result to make sense (at least previous.position < next.position).
        //  4. If PositionCalculator.isExhausted(previous, next), rebalance the target column:
        //     load its cards in order (cardRepository.findAllByColumnIdOrderByPositionAscIdAsc)
        //     and rewrite positions as STEP, 2 * STEP, ... Then read the neighbour positions
        //     again, because they have just changed.
        //  5. card.setColumn(target); card.setPosition(PositionCalculator.between(...)).
        //  6. Decide and document how a move interacts with @Version. A normal entity update
        //     increments the version, which makes the copy another user has open stale even
        //     though only the position changed. Is that the behaviour you want?
        //  7. Return mapper.toResponse(...) after a flush, like update() does.
        throw new UnsupportedOperationException("Moving cards is not implemented yet");
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
