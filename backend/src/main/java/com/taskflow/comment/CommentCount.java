package com.taskflow.comment;

import java.util.UUID;

/** Number of comments of one card, read in bulk for the board view. */
public record CommentCount(UUID cardId, long count) {
}
