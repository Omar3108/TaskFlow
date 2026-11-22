package com.taskflow.label.dto;

import com.taskflow.label.LabelColor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body for both creating and updating a label. */
public record LabelRequest(
        @NotBlank @Size(max = 30) String name,
        @NotNull LabelColor color) {
}
