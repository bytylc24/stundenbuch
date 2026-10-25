package stundenbuch.anzeige;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Gemeinsame Formate für Terminal, Bericht und Export, damit überall dasselbe steht.
 */
public final class Format {

    // Österreichisches Format: Komma als Dezimaltrennzeichen
    public static final Locale DEUTSCH = Locale.forLanguageTag("de-AT");

    // 05.10.2026, mit Wochentag "Mo., 05.10.2026" und Monat "Oktober 2026"
    public static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter TAG = DateTimeFormatter.ofPattern("EE, dd.MM.yyyy", DEUTSCH);
    public static final DateTimeFormatter MONAT = DateTimeFormatter.ofPattern("MMMM yyyy", DEUTSCH);

    private Format() {
    }

    /** Stunden mit Komma, z. B. "4,5 h". */
    public static String stunden(double stunden) {
        return zahl(stunden) + " h";
    }

    /**
     * Nur die Zahl: für Tabellen, deren Überschrift schon "Stunden" sagt.
     * Eine Nachkommastelle ("4,5"), nur bei Viertelstunden und Minuten zwei ("4,25"),
     * damit 8:15-12:30 nicht zu 4,3 gerundet wird.
     */
    public static String zahl(double stunden) {
        String text = String.format(DEUTSCH, "%.2f", stunden);
        return text.endsWith("0") ? text.substring(0, text.length() - 1) : text;
    }
}
