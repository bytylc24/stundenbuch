package stundenbuch;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Gemeinsame Formate für Terminal, Bericht und Export, damit überall dasselbe steht.
 */
final class Format {

    // Österreichisches Format: Komma als Dezimaltrennzeichen
    static final Locale DEUTSCH = Locale.forLanguageTag("de-AT");

    // 05.10.2026, mit Wochentag "Mo., 05.10.2026" und Monat "Oktober 2026"
    static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    static final DateTimeFormatter TAG = DateTimeFormatter.ofPattern("EE, dd.MM.yyyy", DEUTSCH);
    static final DateTimeFormatter MONAT = DateTimeFormatter.ofPattern("MMMM yyyy", DEUTSCH);

    private Format() {
    }

    /** Stunden mit Komma und einer Nachkommastelle, z. B. "4,5 h". */
    static String stunden(double stunden) {
        return String.format(DEUTSCH, "%.1f h", stunden);
    }

    /** Nur die Zahl, z. B. "4,5": für Tabellen, deren Überschrift schon "Stunden" sagt. */
    static String zahl(double stunden) {
        return String.format(DEUTSCH, "%.1f", stunden);
    }
}
