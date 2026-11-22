package com.taskflow.label.dto;

import com.taskflow.label.LabelColor;
import java.util.UUID;

public record LabelResponse(UUID id, String name, LabelColor color) {
}
