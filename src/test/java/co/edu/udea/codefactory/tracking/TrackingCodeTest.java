package co.edu.udea.codefactory.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class TrackingCodeTest {
    @Test void screenshotExampleAndNormalization() {
        assertEquals("LOG-29047381-8", TrackingCode.create(29047381));
        assertEquals("LOG-29047381-8", TrackingCode.normalize(" log290473818 "));
        assertEquals("LOG-00000001-8", TrackingCode.create(1));
    }

    @Test void invalidCheckDigitAndShape() {
        assertThrows(InvalidTrackingCodeException.class, () -> TrackingCode.normalize("LOG-29047381-7"));
        assertThrows(InvalidTrackingCodeException.class, () -> TrackingCode.normalize("ABC-29047381-8"));
        assertThrows(InvalidTrackingCodeException.class, () -> TrackingCode.normalize("LOG-38472915-5"));
        assertThrows(InvalidTrackingCodeException.class, () -> TrackingCode.normalize("L-OG290473818"));
        assertThrows(IllegalArgumentException.class, () -> TrackingCode.create(100_000_000));
    }
}
