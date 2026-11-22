package com.taskflow.comment.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * @param author null when the author's account no longer exists
 * @param edited true once the text has been changed after posting
 */
public record CommentResponse(
        UUID id, UUID cardId, CommentAuthorResponse author, String body, boolean edited, Instant createdAt, Instant updatedAt) {

    public record CommentAuthorResponse(UUID id, String displayName) {
    }
}
