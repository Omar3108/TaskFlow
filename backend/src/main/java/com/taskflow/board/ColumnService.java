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
     * Moves a column between two neighbours of the same board. Only this row is updated.
     *
     * <p>Rebalancing (for a gap that can no longer be split) is added in phase 3b, mirroring the
     * card implementation.
     */
    @Transactional
    public ColumnResponse move(UUID columnId, UUID userId, MoveColumnRequest request) {
        BoardColumn column = boardAccess.requireColumn(columnId, userId, WorkspaceRole.ADMIN);
        UUID boardId = column.getBoard().getId();

        Double previous = neighbourPosition(request.previousColumnId(), columnId, boardId);
        Double next = neighbourPosition(request.nextColumnId(), columnId, boardId);
        if (previous != null && next != null && previous >= next) {
            throw new BadRequestException("The previous column must come before the next column");
        }

        column.setPosition(PositionCalculator.between(previous, next));
        return mapper.toColumnResponse(column);
    }

    /** Position of a neighbour, after checking that it is a different column of the same board. */
    private Double neighbourPosition(UUID neighbourId, UUID movedColumnId, UUID boardId) {
        if (neighbourId == null) {
            return null;
        }
        if (neighbourId.equals(movedColumnId)) {
            throw new BadRequestException("A column cannot be its own neighbour");
        }
        return columnRepository.findById(neighbourId)
                .filter(neighbour -> neighbour.getBoard().getId().equals(boardId))
                .map(BoardColumn::getPosition)
                // Same answer for "does not exist" and "belongs to another board": the caller
                // may not be allowed to know about that other board.
                .orElseThrow(() -> new BadRequestException("The neighbour columns must belong to the same board"));
    }
}
