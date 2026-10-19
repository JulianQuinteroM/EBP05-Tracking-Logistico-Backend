package co.edu.udea.codefactory.tracking;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class PublicTrackingRateLimiterTest {
    @Test void blocksAfterTenFailuresAndUnlocksAfterFifteenMinutes() {
        MutableClock clock = new MutableClock();
        PublicTrackingRateLimiter limiter = new PublicTrackingRateLimiter(clock);
        for (int i = 0; i < 9; i++) {
            limiter.notFound("cliente-a");
            assertDoesNotThrow(() -> limiter.check("cliente-a"));
        }
        limiter.notFound("cliente-a");
        assertThrows(TooManyRequestsException.class, () -> limiter.check("cliente-a"));
        assertDoesNotThrow(() -> limiter.check("cliente-b"));
        clock.instant = clock.instant.plusSeconds(15 * 60 + 1);
        assertDoesNotThrow(() -> limiter.check("cliente-a"));
    }

    private static final class MutableClock extends Clock {
        Instant instant = Instant.parse("2026-10-19T10:00:00Z");
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
