package com.taskflow.board;

import com.taskflow.common.error.ResourceNotFoundException;
import com.taskflow.workspace.WorkspaceAccessService;
import com.taskflow.workspace.WorkspaceMember;
import com.taskflow.workspace.WorkspaceRole;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a board or a column and checks, in the same step, that the caller may use it.
 *
 * <p>Authorization always follows the chain resource → workspace → caller's role. This class
 * only walks the first half of the chain; the decision itself stays in
 * {@link WorkspaceAccessService}.
 */
@Service
public class BoardAccessService {

    private static final String BOARD_NOT_FOUND = "Board not found";
    private static final String COLUMN_NOT_FOUND = "Column not found";

    private final BoardRepository boardRepository;
    private final BoardColumnRepository columnRepository;
    private final WorkspaceAccessService workspaceAccess;

    public BoardAccessService(
            BoardRepository boardRepository,
            BoardColumnRepository columnRepository,
            WorkspaceAccessService workspaceAccess) {
        this.boardRepository = boardRepository;
        this.columnRepository = columnRepository;
        this.workspaceAccess = workspaceAccess;
    }

    /** A board the caller can use, plus the caller's membership (its role is part of responses). */
    public record BoardAccess(Board board, WorkspaceMember member) {
    }

    @Transactional(readOnly = true)
    public BoardAccess requireBoard(UUID boardId, UUID userId, WorkspaceRole minimumRole) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResourceNotFoundException(BOARD_NOT_FOUND));
        // getWorkspace() is a lazy proxy; asking a proxy for its id does not hit the database.
        WorkspaceMember member =
                workspaceAccess.requireRole(board.getWorkspace().getId(), userId, minimumRole, BOARD_NOT_FOUND);
        return new BoardAccess(board, member);
    }

    @Transactional(readOnly = true)
    public BoardColumn requireColumn(UUID columnId, UUID userId, WorkspaceRole minimumRole) {
        BoardColumn column = columnRepository.findByIdWithBoard(columnId)
                .orElseThrow(() -> new ResourceNotFoundException(COLUMN_NOT_FOUND));
        workspaceAccess.requireRole(workspaceIdOf(column), userId, minimumRole, COLUMN_NOT_FOUND);
        return column;
    }

    /** Requires the column's board to be loaded already (see {@code findByIdWithBoard}). */
    public static UUID workspaceIdOf(BoardColumn column) {
        return column.getBoard().getWorkspace().getId();
    }
}
