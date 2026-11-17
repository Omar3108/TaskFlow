package com.taskflow.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param defaultColumns whether the board starts with "To do", "In progress" and "Done".
 *                       Optional: null means yes, so a client that does not know the field gets
 *                       a ready-to-use board.
 */
public record CreateBoardRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 500) String description,
        Boolean defaultColumns) {

    public boolean wantsDefaultColumns() {
        return defaultColumns == null || defaultColumns;
    }
}
