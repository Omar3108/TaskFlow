package com.taskflow.card.dto;

import com.taskflow.label.dto.LabelResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * @param labels  in alphabetical order
 * @param version optimistic-locking counter. The client sends it back when editing the card so
 *                the server can tell whether somebody else changed it in the meantime.
 */
public record CardResponse(
        UUID id,
        UUID boardId,
        UUID columnId,
        String title,
        String description,
        double position,
        AssigneeResponse assignee,
        LocalDate dueDate,
        List<LabelResponse> labels,
        long version,
        Instant createdAt,
        Instant updatedAt) {
}
