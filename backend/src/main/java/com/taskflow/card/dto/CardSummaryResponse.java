package com.taskflow.card.dto;

import java.time.LocalDate;
import java.util.UUID;

/** A card as shown on the board. The description is left out to keep the board response small. */
public record CardSummaryResponse(
        UUID id, UUID columnId, String title, double position, AssigneeResponse assignee, LocalDate dueDate, long version) {
}
