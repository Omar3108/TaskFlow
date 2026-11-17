package com.taskflow.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body for both creating and renaming a column. */
public record ColumnRequest(
        @NotBlank @Size(max = 60) String name) {
}
