package com.taskflow.board;

import com.taskflow.board.BoardAccessService.BoardAccess;
import com.taskflow.board.dto.BoardDetailResponse;
import com.taskflow.board.dto.BoardDetailResponse.ColumnWithCards;
import com.taskflow.board.dto.BoardRequest;
import com.taskflow.board.dto.BoardResponse;
import com.taskflow.board.dto.CreateBoardRequest;
import com.taskflow.card.Card;
import com.taskflow.card.CardFilter;
import com.taskflow.card.CardLabelRow;
import com.taskflow.card.CardMapper;
import com.taskflow.card.CardSpecifications;
import com.taskflow.comment.CommentCount;
import com.taskflow.comment.CommentRepository;
import com.taskflow.label.Label;
import com.taskflow.label.LabelMapper;
import com.taskflow.card.CardRepository;
import com.taskflow.card.dto.CardSummaryResponse;
import com.taskflow.common.ordering.PositionCalculator;
import com.taskflow.workspace.WorkspaceAccessService;
import com.taskflow.workspace.WorkspaceMember;
import com.taskflow.workspace.WorkspaceRole;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardService {

    /** Columns a new board starts with, unless the client asks for an empty board. */
    static final List<String> DEFAULT_COLUMNS = List.of("To do", "In progress", "Done");

    private final BoardRepository boardRepository;
    private final BoardColumnRepository columnRepository;
    private final CardRepository cardRepository;
    private final WorkspaceAccessService workspaceAccess;
    private final BoardAccessService boardAccess;
    private final BoardMapper boardMapper;
    private final CardMapper cardMapper;
    private final LabelMapper labelMapper;
    private final CommentRepository commentRepository;
    private final Clock clock;

    public BoardService(
            BoardRepository boardRepository,
            BoardColumnRepository columnRepository,
            CardRepository cardRepository,
            WorkspaceAccessService workspaceAccess,
            BoardAccessService boardAccess,
            BoardMapper boardMapper,
            CardMapper cardMapper,
            LabelMapper labelMapper,
            CommentRepository commentRepository,
            Clock clock) {
        this.boardRepository = boardRepository;
        this.columnRepository = columnRepository;
        this.cardRepository = cardRepository;
        this.workspaceAccess = workspaceAccess;
        this.boardAccess = boardAccess;
        this.boardMapper = boardMapper;
        this.cardMapper = cardMapper;
        this.labelMapper = labelMapper;
        this.commentRepository = commentRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<BoardResponse> list(UUID workspaceId, UUID userId) {
        workspaceAccess.requireMember(workspaceId, userId);
        return boardRepository.findAllByWorkspaceIdOrderByCreatedAtAscIdAsc(workspaceId).stream()
                .map(boardMapper::toBoardResponse)
                .toList();
    }

    @Transactional
    public BoardResponse create(UUID workspaceId, UUID userId, CreateBoardRequest request) {
        WorkspaceMember admin = workspaceAccess.requireRole(workspaceId, userId, WorkspaceRole.ADMIN);
        Board board = boardRepository.save(
                new Board(admin.getWorkspace(), request.name().trim(), normalize(request.description())));
        if (request.wantsDefaultColumns()) {
            for (int i = 0; i < DEFAULT_COLUMNS.size(); i++) {
                // Same positions a client would get by adding the columns one by one.
                double position = (i + 1) * PositionCalculator.STEP;
                columnRepository.save(new BoardColumn(board, DEFAULT_COLUMNS.get(i), position));
            }
        }
        return boardMapper.toBoardResponse(board);
    }

    /**
     * The whole board in a fixed number of queries (board, membership, columns, cards, labels,
     * comment counts), however many columns and cards it has. Cards are grouped by column in
     * memory. With filters, every column is still returned, but only the matching cards.
     */
    @Transactional(readOnly = true)
    public BoardDetailResponse get(UUID boardId, UUID userId, CardFilter filter) {
        BoardAccess access = boardAccess.requireBoard(boardId, userId, WorkspaceRole.MEMBER);
        Board board = access.board();

        Map<UUID, List<Label>> labelsByCard = cardRepository.findLabelsByBoardId(boardId).stream()
                .collect(Collectors.groupingBy(
                        CardLabelRow::cardId, Collectors.mapping(CardLabelRow::label, Collectors.toList())));
        Map<UUID, Long> commentCounts = commentRepository.countByBoardId(boardId).stream()
                .collect(Collectors.toMap(CommentCount::cardId, CommentCount::count));

        List<Card> cards = cardRepository.findAll(
                CardSpecifications.forBoard(boardId, filter, LocalDate.now(clock)),
                Sort.by("position", "id"));

        // groupingBy keeps the encounter order inside each group, so cards stay sorted.
        Map<UUID, List<CardSummaryResponse>> cardsByColumn = cards.stream()
                .map(card -> new CardSummaryResponse(
                        card.getId(),
                        card.getColumn().getId(),
                        card.getTitle(),
                        card.getPosition(),
                        card.getAssignee() == null ? null : cardMapper.toAssignee(card.getAssignee()),
                        card.getDueDate(),
                        labelMapper.toSortedResponses(labelsByCard.getOrDefault(card.getId(), List.of())),
                        commentCounts.getOrDefault(card.getId(), 0L),
                        card.getVersion()))
                .collect(Collectors.groupingBy(CardSummaryResponse::columnId));

        List<ColumnWithCards> columns = columnRepository.findAllByBoardIdOrderByPositionAscIdAsc(boardId).stream()
                .map(column -> new ColumnWithCards(
                        column.getId(),
                        column.getName(),
                        column.getPosition(),
                        cardsByColumn.getOrDefault(column.getId(), List.of())))
                .toList();

        return new BoardDetailResponse(
                board.getId(),
                board.getWorkspace().getId(),
                board.getName(),
                board.getDescription(),
                access.member().getRole(),
                columns);
    }

    @Transactional
    public BoardResponse update(UUID boardId, UUID userId, BoardRequest request) {
        Board board = boardAccess.requireBoard(boardId, userId, WorkspaceRole.ADMIN).board();
        board.setName(request.name().trim());
        board.setDescription(normalize(request.description()));
        // Flush now so @PreUpdate has run and the response carries the new updatedAt.
        return boardMapper.toBoardResponse(boardRepository.saveAndFlush(board));
    }

    /** Columns and cards are removed by ON DELETE CASCADE in the database. */
    @Transactional
    public void delete(UUID boardId, UUID userId) {
        Board board = boardAccess.requireBoard(boardId, userId, WorkspaceRole.ADMIN).board();
        boardRepository.delete(board);
    }

    /** Blank optional text is stored as NULL, so "empty" has a single representation. */
    private static String normalize(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
