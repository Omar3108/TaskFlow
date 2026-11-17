package com.taskflow.workspace.dto;

import com.taskflow.workspace.WorkspaceRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(@NotNull WorkspaceRole role) {
}
