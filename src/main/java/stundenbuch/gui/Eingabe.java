package stundenbuch.gui;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Liest Texteingaben (Uhrzeit, Pause, Stunden, Datum). Gibt bei ungültiger Eingabe null zurück. */
final class Eingabe {

    // Erlaubt 05.10.2026 und 5.10.2026; STRICT lehnt Daten wie 31.02.2026 ab
    static final DateTimeFormatter DATUM =
            DateTimeFormatter.ofPattern("d.M.uuuu").withResolverStyle(ResolverStyle.STRICT);

    private static final Pattern ZEIT = Pattern.compile("(\\d{1,2})(?:[:.](\\d{2}))?");
    // 30, 30 min, 0,5h, 1h, 0:30
    private static final Pattern PAUSE =
            Pattern.compile("(?:(\\d{1,2}):(\\d{2}))|(?:(\\d+(?:[.,]\\d+)?)\\s*(min|m|h|std)?)");

    private Eingabe() {
    }

    static LocalDate datum(String text) {
        try {
            return LocalDate.parse(text.trim(), DATUM);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    /** 9:00, 9.00 oder 9. */
    static LocalTime zeit(String text) {
        Matcher m = ZEIT.matcher(text.trim());
        if (!m.matches()) {
            return null;
        }
        int stunde = Integer.parseInt(m.group(1));
        int minute = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
        if (stunde > 23 || minute > 59) {
            return null;
        }
        return LocalTime.of(stunde, minute);
    }

    /**
     * Pause in Minuten. Eine Zahl allein sind Minuten, mit h oder Komma Stunden (0,5h),
     * mit Doppelpunkt h:mm (0:30). Leer = 0.
     */
    static Integer pauseMinuten(String text) {
        String s = text.trim().toLowerCase();
        if (s.isEmpty()) {
            return 0;
        }
        Matcher m = PAUSE.matcher(s);
        if (!m.matches()) {
            return null;
        }
        double minuten;
        if (m.group(1) != null) {
            minuten = Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
        } else {
            double zahl = Double.parseDouble(m.group(3).replace(',', '.'));
            String einheit = m.group(4);
            boolean stunden = "h".equals(einheit) || "std".equals(einheit)
                    || (einheit == null && !m.group(3).matches("\\d+"));
            minuten = stunden ? zahl * 60 : zahl;
        }
        return (int) Math.round(minuten);
    }

    /** Stunden mit Komma oder Punkt. */
    static Double stunden(String text) {
        try {
            double d = Double.parseDouble(text.trim().replace(',', '.'));
            // Diese Prüfung lehnt auch "NaN" und "Infinity" ab
            return d > 0 && d <= 24 ? d : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
