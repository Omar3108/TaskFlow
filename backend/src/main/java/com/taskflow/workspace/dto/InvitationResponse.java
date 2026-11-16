package com.taskflow.workspace.dto;

import com.taskflow.workspace.InvitationStatus;
import com.taskflow.workspace.WorkspaceRole;
import java.time.Instant;
import java.util.UUID;

/** An invitation as seen by the admins of the workspace that sent it. */
public record InvitationResponse(
        UUID id,
        String email,
        WorkspaceRole role,
        InvitationStatus status,
        String invitedByName,
        Instant expiresAt,
        Instant createdAt) {
}
