package com.taskflow.common.ordering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PositionCalculatorTest {

    @Test
    void firstItemOfAnEmptyListGetsOneStep() {
        assertThat(PositionCalculator.append(null)).isEqualTo(PositionCalculator.STEP);
    }

    @Test
    void appendedItemGoesOneStepAfterTheLast() {
        assertThat(PositionCalculator.append(2048.0)).isEqualTo(2048.0 + PositionCalculator.STEP);
        assertThat(PositionCalculator.append(0.5)).isGreaterThan(0.5);
    }

    /** Specification of between() and isExhausted(). */
    @Nested
    class Between {

        @Test
        void emptyListBehavesLikeAppend() {
            assertThat(PositionCalculator.between(null, null)).isEqualTo(PositionCalculator.append(null));
        }

        @Test
        void startOfTheListIsBelowTheFirstItem() {
            assertThat(PositionCalculator.between(null, 1024.0)).isLessThan(1024.0);
            assertThat(PositionCalculator.between(null, 0.001)).isLessThan(0.001);
        }

        @Test
        void endOfTheListBehavesLikeAppend() {
            assertThat(PositionCalculator.between(3072.0, null)).isEqualTo(PositionCalculator.append(3072.0));
        }

        @Test
        void middleIsStrictlyBetweenTheNeighbours() {
            assertThat(PositionCalculator.between(1024.0, 2048.0)).isEqualTo(1536.0);
            assertThat(PositionCalculator.between(1.0, 1.5)).isGreaterThan(1.0).isLessThan(1.5);
        }

        @Test
        void neighboursInTheWrongOrderAreRejected() {
            assertThatThrownBy(() -> PositionCalculator.between(2048.0, 1024.0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> PositionCalculator.between(1024.0, 1024.0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void aNormalGapIsNotExhausted() {
            assertThat(PositionCalculator.isExhausted(1024.0, 2048.0)).isFalse();
        }

        @Test
        void adjacentDoublesAreExhausted() {
            // Math.nextUp gives the very next representable double: nothing fits in between.
            assertThat(PositionCalculator.isExhausted(1024.0, Math.nextUp(1024.0))).isTrue();
        }
    }
}
