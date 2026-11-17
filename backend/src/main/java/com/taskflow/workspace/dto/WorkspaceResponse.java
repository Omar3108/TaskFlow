package com.taskflow.workspace.dto;

import com.taskflow.workspace.WorkspaceRole;
import java.time.Instant;
import java.util.UUID;

/**
 * @param role the role of the user who made the request, so the client knows which actions
 *             to offer
 */
public record WorkspaceResponse(UUID id, String name, WorkspaceRole role, Instant createdAt) {
}
