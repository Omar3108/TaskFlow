package com.taskflow.card.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Where the card was dropped: the target column and the card's new neighbours there. The
 * server computes the position from the neighbours.
 *
 * @param columnId       target column (the current one when reordering within a column)
 * @param previousCardId the card that will sit right before, or null when moving to the top
 * @param nextCardId     the card that will sit right after, or null when moving to the bottom
 */
public record MoveCardRequest(@NotNull UUID columnId, UUID previousCardId, UUID nextCardId) {
}
