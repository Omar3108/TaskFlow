package com.taskflow.board.dto;

import java.time.Instant;
import java.util.UUID;

public record BoardResponse(
        UUID id, UUID workspaceId, String name, String description, Instant createdAt, Instant updatedAt) {
}
