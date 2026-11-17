package com.taskflow.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for both creating and updating a board. An update sends every editable field: a null
 * description clears it.
 */
public record BoardRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 500) String description) {
}
