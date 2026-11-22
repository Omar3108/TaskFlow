package com.taskflow.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body for both posting and editing a comment. */
public record CommentRequest(
        @NotBlank @Size(max = 2000) String body) {
}
