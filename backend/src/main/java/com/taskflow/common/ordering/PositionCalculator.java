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

    /**
     * Smallest gap worth splitting. Positions are normally a few thousand to a few million, where
     * a double still resolves differences of about 1e-10, so 1e-6 leaves a comfortable margin
     * (about 30 consecutive halvings of a STEP gap) before a rebalance.
     */
    public static final double MIN_GAP = 1e-6;

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
        if (previous == null) {
            // "next - STEP" rather than "next / 2": halving only works while positions are
            // positive and keeps shrinking the room before the first item, whereas a fixed
            // step leaves the same room every time (positions may become negative, which a
            // DOUBLE PRECISION column accepts) and can never run out of precision.
            return next == null ? append(null) : next - STEP;
        }
        if (next == null) {
            return append(previous);
        }
        if (previous >= next) {
            throw new IllegalArgumentException(
                    "Neighbours must be in ascending order: previous=" + previous + ", next=" + next);
        }
        return previous + (next - previous) / 2;
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
        // Both conditions on purpose. The exact test (midpoint collides with an end) is the
        // hard limit: past it the order would silently break. The MIN_GAP threshold rebalances
        // long before that, while the position still has plenty of precision, so a rebalance
        // never happens in the middle of a pathological state. It also catches the zero gap
        // left by two concurrent moves into the same gap (equal positions).
        double midpoint = previous + (next - previous) / 2;
        return next - previous < MIN_GAP || midpoint <= previous || midpoint >= next;
    }
}
