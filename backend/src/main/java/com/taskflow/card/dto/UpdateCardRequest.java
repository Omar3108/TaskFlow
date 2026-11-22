package com.taskflow.card.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * An edit sends every editable field: a null description, assignee or due date clears it, and
 * a missing or empty label list removes every label.
 *
 * @param labelIds labels of the board of the card; any other id is rejected with 400
 * @param version  the version the client read before editing; a stale value is rejected with 409
 */
public record UpdateCardRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String description,
        UUID assigneeId,
        LocalDate dueDate,
        @Size(max = 20) List<UUID> labelIds,
        @NotNull Long version) {
}
