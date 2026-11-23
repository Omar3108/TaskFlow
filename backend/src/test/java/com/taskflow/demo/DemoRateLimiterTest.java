package com.taskflow.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class DemoRateLimiterTest {

    /** A clock the test moves by hand. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-22T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final DemoRateLimiter limiter =
            new DemoRateLimiter(new DemoProperties(true, Duration.ofHours(24), 3, 100), clock);

    @Test
    void allowsUpToTheLimitPerAddress() {
        assertThat(limiter.tryAcquire("1.1.1.1")).isTrue();
        assertThat(limiter.tryAcquire("1.1.1.1")).isTrue();
        assertThat(limiter.tryAcquire("1.1.1.1")).isTrue();
        assertThat(limiter.tryAcquire("1.1.1.1")).isFalse();
        assertThat(limiter.tryAcquire("2.2.2.2")).isTrue();
    }

    @Test
    void theWindowSlides() {
        limiter.tryAcquire("1.1.1.1");
        clock.advance(Duration.ofMinutes(30));
        limiter.tryAcquire("1.1.1.1");
        limiter.tryAcquire("1.1.1.1");
        assertThat(limiter.tryAcquire("1.1.1.1")).isFalse();

        // An hour after the first attempt, exactly one slot is free again.
        clock.advance(Duration.ofMinutes(31));
        assertThat(limiter.tryAcquire("1.1.1.1")).isTrue();
        assertThat(limiter.tryAcquire("1.1.1.1")).isFalse();
    }

    @Test
    void rejectedAttemptsDoNotExtendTheBlock() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("1.1.1.1");
        }
        for (int i = 0; i < 10; i++) {
            assertThat(limiter.tryAcquire("1.1.1.1")).isFalse();
        }

        clock.advance(Duration.ofMinutes(61));
        assertThat(limiter.tryAcquire("1.1.1.1")).isTrue();
    }
}
