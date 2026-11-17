package com.taskflow.board.dto;

import java.util.UUID;

public record ColumnResponse(UUID id, UUID boardId, String name, double position) {
}
