package com.taskflow.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body for both creating and renaming a workspace. */
public record WorkspaceRequest(
        @NotBlank @Size(max = 80) String name) {
}
