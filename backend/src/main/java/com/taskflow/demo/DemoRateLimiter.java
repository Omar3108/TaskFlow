package com.taskflow.demo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Sliding-window limit of demo sandboxes per client address: at most N in the last hour.
 *
 * <p>Kept in memory, which is enough for the single instance this app runs on. With several
 * instances each would count on its own, and the counters would have to move to a shared
 * store such as Redis. It is a brake against abuse, not a security boundary: the expiry and the
 * global cap in DemoService bound what any client can do anyway.
 */
@Component
public class DemoRateLimiter {

    private static final Duration WINDOW = Duration.ofHours(1);

    private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();
    private final DemoProperties properties;
    private final Clock clock;

    public DemoRateLimiter(DemoProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /** Records an attempt and says whether it is allowed. */
    public boolean tryAcquire(String clientAddress) {
        Instant now = clock.instant();
        Instant windowStart = now.minus(WINDOW);
        boolean[] allowed = {false};
        // compute() runs atomically per key, so two requests from the same address cannot both
        // slip through when only one slot is left.
        attempts.compute(clientAddress, (key, times) -> {
            Deque<Instant> recent = times == null ? new ArrayDeque<>() : times;
            while (!recent.isEmpty() && recent.peekFirst().isBefore(windowStart)) {
                recent.pollFirst();
            }
            if (recent.size() < properties.maxPerIpPerHour()) {
                recent.addLast(now);
                allowed[0] = true;
            }
            return recent;
        });
        // Forget addresses that have been quiet for a full window, so the map cannot grow forever.
        attempts.entrySet().removeIf(entry -> entry.getValue().isEmpty()
                || entry.getValue().peekLast().isBefore(windowStart));
        return allowed[0];
    }
}
