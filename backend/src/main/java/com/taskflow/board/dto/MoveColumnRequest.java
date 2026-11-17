package com.taskflow.board.dto;

import java.util.UUID;

/**
 * Where the column was dropped, expressed as its new neighbours. The client never sends a
 * position: the server computes it, so a buggy or malicious client cannot corrupt the order.
 *
 * @param previousColumnId the column that will sit right before, or null when moving to the start
 * @param nextColumnId     the column that will sit right after, or null when moving to the end
 */
public record MoveColumnRequest(UUID previousColumnId, UUID nextColumnId) {
}
