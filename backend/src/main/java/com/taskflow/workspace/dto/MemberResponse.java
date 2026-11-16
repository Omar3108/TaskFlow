package com.taskflow.workspace.dto;

import com.taskflow.workspace.WorkspaceRole;
import java.time.Instant;
import java.util.UUID;

public record MemberResponse(UUID userId, String email, String displayName, WorkspaceRole role, Instant joinedAt) {
}
