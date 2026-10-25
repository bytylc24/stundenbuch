package stundenbuch.bericht;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Eine Tabelle mit Überschrift, Spalten und Zeilen. Nur Daten: Terminal, Word und CSV
 * zeichnen sie jeweils selbst. Weil alle dieselbe Tabelle bekommen, sehen alle gleich aus.
 */
public class Tabelle {

    /** NORMAL = Datenzeile, GRUPPE = Zwischenüberschrift (z. B. ein Monat), dazu zwei Arten Summen. */
    public enum Art { NORMAL, GRUPPE, ZWISCHENSUMME, SUMME }

    /** maxBreite: ab so vielen Zeichen wird im Terminal umgebrochen (0 = nie). */
    public record Spalte(String name, boolean rechts, int maxBreite) {
    }

    /** block: Zeilen mit derselben Zahl (größer 0) hintereinander gehören zusammen, z. B. ein Tag (0 = keiner). */
    public record Zeile(Art art, List<String> zellen, int block) {

        public Zeile(Art art, List<String> zellen) {
            this(art, zellen, 0);
        }
    }

    private final String titel;
    private final List<Spalte> spalten = new ArrayList<>();
    private final List<Zeile> zeilen = new ArrayList<>();

    public Tabelle(String titel) {
        this.titel = titel;
    }

    public Tabelle spalte(String name) {
        return spalte(name, false, 0);
    }

    /** Rechtsbündige Spalte für Zahlen. */
    public Tabelle zahlenSpalte(String name) {
        return spalte(name, true, 0);
    }

    public Tabelle spalte(String name, boolean rechts, int maxBreite) {
        spalten.add(new Spalte(name, rechts, maxBreite));
        return this;
    }

    public void zeile(String... zellen) {
        zeilen.add(new Zeile(Art.NORMAL, List.of(zellen)));
    }

    /** Datenzeile, die mit den Nachbarzeilen gleicher Blocknummer zusammengehört (wird gemeinsam umrandet). */
    public void zeile(int block, String... zellen) {
        zeilen.add(new Zeile(Art.NORMAL, List.of(zellen), block));
    }

    public void gruppe(String text) {
        zeilen.add(new Zeile(Art.GRUPPE, List.of(text)));
    }

    /** Erste Zelle ist die Beschriftung, leere Zellen danach werden mit ihr zusammengefasst. */
    public void zwischensumme(String... zellen) {
        zeilen.add(new Zeile(Art.ZWISCHENSUMME, List.of(zellen)));
    }

    public void summe(String... zellen) {
        zeilen.add(new Zeile(Art.SUMME, List.of(zellen)));
    }

    /**
     * Über wie viele Spalten die erste Zelle reicht: bei GRUPPE alle, bei Summen
     * bis zur ersten Spalte mit Inhalt, sonst nur eine.
     */
    public int spanne(Zeile z) {
        if (z.art() == Art.NORMAL) {
            return 1;
        }
        if (z.art() == Art.GRUPPE) {
            return spalten.size();
        }
        int s = 1;
        while (s < spalten.size() && z.zellen().get(s).isEmpty()) {
            s++;
        }
        return s;
    }

    public String getTitel() {
        return titel;
    }

    public List<Spalte> getSpalten() {
        return Collections.unmodifiableList(spalten);
    }

    public List<Zeile> getZeilen() {
        return Collections.unmodifiableList(zeilen);
    }
}
