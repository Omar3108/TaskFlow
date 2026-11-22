package com.taskflow.card;

import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Optional filters of the board view, bound from query parameters
 * ({@code ?q=login&assigneeId=...&labelId=...&due=OVERDUE}). Every filter that is present must
 * match (AND); absent filters match everything.
 *
 * @param q          case-insensitive text searched in the title and the description
 * @param unassigned true for cards without an assignee
 */
public record CardFilter(
        @Size(max = 100) String q, UUID assigneeId, Boolean unassigned, UUID labelId, DueFilter due) {

    public static CardFilter none() {
        return new CardFilter(null, null, null, null, null);
    }
}
