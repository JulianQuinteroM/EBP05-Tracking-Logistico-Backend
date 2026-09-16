package co.edu.udea.codefactory.tracking;

import java.util.Locale;
import java.util.regex.Pattern;

public final class TrackingCode {
    private static final Pattern FORMATTED = Pattern.compile("LOG-[0-9]{8}-[0-9]");
    private static final Pattern COMPACT = Pattern.compile("LOG[0-9]{9}");

    private TrackingCode() {}

    public static String create(long serial) {
        if (serial < 1 || serial > 99_999_999) throw new IllegalArgumentException("Serie de seguimiento agotada");
        String digits = String.format(Locale.ROOT, "%08d", serial);
        return "LOG-" + digits + "-" + luhnDigit(digits);
    }

    public static String normalize(String input) {
        if (input == null) throw new InvalidTrackingCodeException();
        String upper = input.strip().toUpperCase(Locale.ROOT);
        if (!FORMATTED.matcher(upper).matches() && !COMPACT.matcher(upper).matches())
            throw new InvalidTrackingCodeException();
        String raw = upper.replace("-", "");
        String digits = raw.substring(3, 11);
        if (raw.charAt(11) - '0' != luhnDigit(digits)) throw new InvalidTrackingCodeException();
        return "LOG-" + digits + "-" + raw.charAt(11);
    }

    static int luhnDigit(String eightDigits) {
        int sum = 0;
        for (int i = 0; i < eightDigits.length(); i++) {
            int value = eightDigits.charAt(i) - '0';
            // Para un cuerpo de ocho dígitos, el dígito situado justo antes
            // del verificador se duplica al evaluar el número completo.
            if (i % 2 == 1) {
                value *= 2;
                if (value > 9) value -= 9;
            }
            sum += value;
        }
        return (10 - sum % 10) % 10;
    }
}
