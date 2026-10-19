package co.edu.udea.codefactory.tracking;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PublicTrackingRateLimiter {
    private static final Duration WINDOW = Duration.ofMinutes(10);
    private static final Duration BLOCK = Duration.ofMinutes(15);
    private final Map<String, Attempts> byAddress = new HashMap<>();
    private final Clock clock;

    public PublicTrackingRateLimiter(Clock clock) { this.clock = clock; }

    public synchronized void check(String address) {
        Attempts attempts = byAddress.get(address);
        if (attempts == null) return;
        Instant now = clock.instant();
        if (attempts.blockedUntil != null && attempts.blockedUntil.isAfter(now))
            throw new TooManyRequestsException();
        if (attempts.blockedUntil != null) byAddress.remove(address);
    }

    public synchronized void notFound(String address) {
        Instant now = clock.instant();
        Attempts attempts = byAddress.computeIfAbsent(address, ignored -> new Attempts());
        while (!attempts.failures.isEmpty() && !attempts.failures.peekFirst().isAfter(now.minus(WINDOW)))
            attempts.failures.removeFirst();
        attempts.failures.addLast(now);
        if (attempts.failures.size() >= 10) attempts.blockedUntil = now.plus(BLOCK);
    }

    public synchronized void found(String address) { byAddress.remove(address); }

    private static final class Attempts {
        final ArrayDeque<Instant> failures = new ArrayDeque<>();
        Instant blockedUntil;
    }
}
