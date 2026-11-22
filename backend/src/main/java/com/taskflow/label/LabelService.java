package com.taskflow.label;

import com.taskflow.board.Board;
import com.taskflow.board.BoardAccessService;
import com.taskflow.common.error.BadRequestException;
import com.taskflow.common.error.ConflictException;
import com.taskflow.common.error.ResourceNotFoundException;
import com.taskflow.label.dto.LabelRequest;
import com.taskflow.label.dto.LabelResponse;
import com.taskflow.workspace.WorkspaceAccessService;
import com.taskflow.workspace.WorkspaceRole;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LabelService {

    private static final String LABEL_NOT_FOUND = "Label not found";
    private static final String DUPLICATE_NAME = "This board already has a label with that name";

    private final LabelRepository labelRepository;
    private final BoardAccessService boardAccess;
    private final WorkspaceAccessService workspaceAccess;
    private final LabelMapper mapper;

    public LabelService(
            LabelRepository labelRepository,
            BoardAccessService boardAccess,
            WorkspaceAccessService workspaceAccess,
            LabelMapper mapper) {
        this.labelRepository = labelRepository;
        this.boardAccess = boardAccess;
        this.workspaceAccess = workspaceAccess;
        this.mapper = mapper;
    }

    /** Every member sees the labels: they are needed to label cards and to filter. */
    @Transactional(readOnly = true)
    public List<LabelResponse> list(UUID boardId, UUID userId) {
        boardAccess.requireBoard(boardId, userId, WorkspaceRole.MEMBER);
        return labelRepository.findAllByBoardIdAlphabetically(boardId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public LabelResponse create(UUID boardId, UUID userId, LabelRequest request) {
        Board board = boardAccess.requireBoard(boardId, userId, WorkspaceRole.ADMIN).board();
        String name = request.name().trim();
        // Checked here for a clear message; the unique index still catches a concurrent
        // duplicate, which GlobalExceptionHandler turns into a generic 409.
        if (labelRepository.existsByBoardIdAndNameIgnoreCase(boardId, name)) {
            throw new ConflictException(DUPLICATE_NAME);
        }
        return mapper.toResponse(labelRepository.save(new Label(board, name, request.color())));
    }

    @Transactional
    public LabelResponse update(UUID labelId, UUID userId, LabelRequest request) {
        Label label = requireLabel(labelId, userId, WorkspaceRole.ADMIN);
        String name = request.name().trim();
        if (labelRepository.existsByBoardIdAndNameIgnoreCaseAndIdNot(label.getBoard().getId(), name, labelId)) {
            throw new ConflictException(DUPLICATE_NAME);
        }
        label.setName(name);
        label.setColor(request.color());
        return mapper.toResponse(label);
    }

    /** The label disappears from every card through ON DELETE CASCADE on card_labels. */
    @Transactional
    public void delete(UUID labelId, UUID userId) {
        labelRepository.delete(requireLabel(labelId, userId, WorkspaceRole.ADMIN));
    }

    /**
     * The labels with the given ids, which must all belong to the given board. Used when a card
     * is edited: a label of another board (or an id that does not exist) is rejected with 400,
     * with the same message either way.
     */
    @Transactional(readOnly = true)
    public Set<Label> requireLabelsOfBoard(UUID boardId, Collection<UUID> labelIds) {
        if (labelIds == null || labelIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<UUID> wanted = new HashSet<>(labelIds);
        List<Label> found = labelRepository.findAllByBoardIdAndIdIn(boardId, wanted);
        if (found.size() != wanted.size()) {
            throw new BadRequestException("Every label must belong to the board of the card");
        }
        return new HashSet<>(found);
    }

    private Label requireLabel(UUID labelId, UUID userId, WorkspaceRole minimumRole) {
        Label label = labelRepository.findByIdWithBoard(labelId)
                .orElseThrow(() -> new ResourceNotFoundException(LABEL_NOT_FOUND));
        workspaceAccess.requireRole(label.getBoard().getWorkspace().getId(), userId, minimumRole, LABEL_NOT_FOUND);
        return label;
    }
}
