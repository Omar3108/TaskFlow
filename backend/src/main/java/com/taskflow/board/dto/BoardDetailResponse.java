package com.taskflow.board.dto;

import com.taskflow.card.dto.CardSummaryResponse;
import com.taskflow.workspace.WorkspaceRole;
import java.util.List;
import java.util.UUID;

/**
 * Everything the kanban screen needs in one response: the board, its columns in order and the
 * cards of each column in order.
 *
 * @param role the role of the user who made the request, so the client knows which actions
 *             to offer
 */
public record BoardDetailResponse(
        UUID id, UUID workspaceId, String name, String description, WorkspaceRole role, List<ColumnWithCards> columns) {

    public record ColumnWithCards(UUID id, String name, double position, List<CardSummaryResponse> cards) {
    }
}
