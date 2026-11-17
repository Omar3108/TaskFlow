package com.taskflow.card.dto;

import java.util.UUID;

/** The few user fields a card shows about its assignee. */
public record AssigneeResponse(UUID id, String displayName, String email) {
}
