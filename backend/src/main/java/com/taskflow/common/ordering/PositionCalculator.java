package com.taskflow.common.ordering;

/**
 * Fractional indexing for ordered lists (columns of a board, cards of a column).
 *
 * <p>Each item stores a {@code double} position and lists are read with {@code ORDER BY
 * position}. Placing an item between two neighbours gives it a value between theirs, so a move
 * writes one row instead of renumbering every item after it.
 */
public final class PositionCalculator {

    /** Distance left between consecutive items when appending or rebalancing. */
    public static final double STEP = 1024.0;

    private PositionCalculator() {
    }

    /**
     * Position for an item added at the end of a list.
     *
     * @param lastPosition the highest position in the list, or {@code null} if it is empty
     */
    public static double append(Double lastPosition) {
        return lastPosition == null ? STEP : lastPosition + STEP;
    }

    /**
     * Position for an item dropped between two neighbours.
     *
     * @param previous position of the item that will sit right before, or {@code null} when the
     *                 item goes to the start of the list
     * @param next     position of the item that will sit right after, or {@code null} when the
     *                 item goes to the end of the list
     */
    public static double between(Double previous, Double next) {
        // TODO(Omar): four cases.
        //  - both null     -> the list is empty: same answer as append(null).
        //  - previous null -> going to the start: a value below `next`. Both next / 2 (keeps
        //                     positions positive) and next - STEP are valid; pick one and be
        //                     able to explain why.
        //  - next null     -> going to the end: same answer as append(previous).
        //  - both present  -> the midpoint. Reject previous >= next with an
        //                     IllegalArgumentException: it means the caller passed neighbours
        //                     that are not adjacent in that order.
        throw new UnsupportedOperationException("PositionCalculator.between is not implemented yet");
    }

    /**
     * Whether the gap between two neighbours is too small to be split again.
     *
     * <p>A double has 52 bits of mantissa, so after roughly 50 consecutive splits of the same
     * gap the midpoint equals one of the ends and the order would silently break. When this
     * returns {@code true} the caller must rebalance the list (rewrite every position as STEP,
     * 2*STEP, 3*STEP...) before computing the new position.
     */
    public static boolean isExhausted(double previous, double next) {
        // TODO(Omar): decide the rule. Two options to weigh:
        //  - exact: the midpoint equals `previous` or `next` (rebalances as late as possible).
        //  - threshold: next - previous < some MIN_GAP constant (rebalances earlier, simpler to
        //    reason about and to test).
        throw new UnsupportedOperationException("PositionCalculator.isExhausted is not implemented yet");
    }
}
