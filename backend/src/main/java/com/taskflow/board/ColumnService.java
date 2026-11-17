package com.taskflow.board;

import com.taskflow.board.dto.ColumnRequest;
import com.taskflow.board.dto.ColumnResponse;
import com.taskflow.board.dto.MoveColumnRequest;
import com.taskflow.common.error.BadRequestException;
import com.taskflow.common.ordering.PositionCalculator;
import com.taskflow.workspace.WorkspaceRole;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ColumnService {

    private static final String WRONG_ORDER = "The previous column must come before the next column";

    private final BoardColumnRepository columnRepository;
    private final BoardAccessService boardAccess;
    private final BoardMapper mapper;

    public ColumnService(
            BoardColumnRepository columnRepository, BoardAccessService boardAccess, BoardMapper mapper) {
        this.columnRepository = columnRepository;
        this.boardAccess = boardAccess;
        this.mapper = mapper;
    }

    /** New columns go to the end of the board. */
    @Transactional
    public ColumnResponse create(UUID boardId, UUID userId, ColumnRequest request) {
        Board board = boardAccess.requireBoard(boardId, userId, WorkspaceRole.ADMIN).board();
        double position = PositionCalculator.append(columnRepository.findMaxPositionByBoardId(boardId));
        BoardColumn column = columnRepository.save(new BoardColumn(board, request.name().trim(), position));
        return mapper.toColumnResponse(column);
    }

    @Transactional
    public ColumnResponse rename(UUID columnId, UUID userId, ColumnRequest request) {
        BoardColumn column = boardAccess.requireColumn(columnId, userId, WorkspaceRole.ADMIN);
        column.setName(request.name().trim());
        return mapper.toColumnResponse(column);
    }

    /** The cards of the column are removed by ON DELETE CASCADE in the database. */
    @Transactional
    public void delete(UUID columnId, UUID userId) {
        BoardColumn column = boardAccess.requireColumn(columnId, userId, WorkspaceRole.ADMIN);
        columnRepository.delete(column);
    }

    /**
     * Moves a column between two neighbours of the same board. Normally only this row is
     * updated; when the gap between the neighbours can no longer be split, the positions of the
     * whole board are rewritten first.
     */
    @Transactional
    public ColumnResponse move(UUID columnId, UUID userId, MoveColumnRequest request) {
        BoardColumn column = boardAccess.requireColumn(columnId, userId, WorkspaceRole.ADMIN);
        UUID boardId = column.getBoard().getId();

        BoardColumn previous = resolveNeighbour(request.previousColumnId(), columnId, boardId);
        BoardColumn next = resolveNeighbour(request.nextColumnId(), columnId, boardId);

        if (previous != null && next != null) {
            if (previous.getPosition() > next.getPosition()) {
                throw new BadRequestException(WRONG_ORDER);
            }
            if (PositionCalculator.isExhausted(previous.getPosition(), next.getPosition())) {
                // The rebalance is a bulk statement that clears the persistence context, so
                // the three columns are read again with their new positions.
                columnRepository.rebalanceBoard(boardId, PositionCalculator.STEP);
                column = columnRepository.findById(columnId).orElseThrow();
                previous = columnRepository.findById(previous.getId()).orElseThrow();
                next = columnRepository.findById(next.getId()).orElseThrow();
                // Columns with equal positions are ordered by id, so only now is the real
                // order known.
                if (previous.getPosition() >= next.getPosition()) {
                    throw new BadRequestException(WRONG_ORDER);
                }
            }
        }

        column.setPosition(PositionCalculator.between(
                previous == null ? null : previous.getPosition(), next == null ? null : next.getPosition()));
        return mapper.toColumnResponse(column);
    }

    /** A neighbour, after checking that it is a different column of the same board. */
    private BoardColumn resolveNeighbour(UUID neighbourId, UUID movedColumnId, UUID boardId) {
        if (neighbourId == null) {
            return null;
        }
        if (neighbourId.equals(movedColumnId)) {
            throw new BadRequestException("A column cannot be its own neighbour");
        }
        return columnRepository.findById(neighbourId)
                .filter(neighbour -> neighbour.getBoard().getId().equals(boardId))
                // Same answer for "does not exist" and "belongs to another board": the caller
                // may not be allowed to know about that other board.
                .orElseThrow(() -> new BadRequestException("The neighbour columns must belong to the same board"));
    }
}
