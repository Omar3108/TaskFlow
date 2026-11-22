package com.taskflow.card;

import com.taskflow.label.Label;
import java.util.UUID;

/** One (card, label) pair of a board, read in bulk to fill the card summaries. */
public record CardLabelRow(UUID cardId, Label label) {
}
