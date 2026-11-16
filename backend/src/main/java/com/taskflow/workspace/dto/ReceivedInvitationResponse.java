package com.taskflow.workspace.dto;

import com.taskflow.workspace.WorkspaceRole;
import java.time.Instant;
import java.util.UUID;

/** An invitation as seen by the person who was invited. */
public record ReceivedInvitationResponse(
        UUID id,
        UUID workspaceId,
        String workspaceName,
        WorkspaceRole role,
        String invitedByName,
        Instant expiresAt) {
}
