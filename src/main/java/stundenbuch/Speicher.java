package stundenbuch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Liest und schreibt die drei CSV-Dateien (Trennzeichen ;, UTF-8, keine Kopfzeile).
 * Redet nicht mit dem Benutzer: Fehler beim Speichern werden als Exception weitergegeben.
 */
public class Speicher {

    private static final String MITARBEITER_DATEI = "mitarbeiter.csv";
    private static final String PROJEKTE_DATEI = "projekte.csv";
    private static final String EINTRAEGE_DATEI = "eintraege.csv";

    private final Path ordner;

    /** Speichert im Ordner "Stundenbuch" im Benutzerordner. */
    public Speicher() {
        this.ordner = Path.of(System.getProperty("user.home"), "Stundenbuch").toAbsolutePath();
    }

    public Path getOrdner() {
        return ordner;
    }

    // ---------- Speichern ----------

    public void speichern(Zeiterfassung z) throws IOException {
        Files.createDirectories(ordner);

        List<String> zeilen = new ArrayList<>();
        for (Mitarbeiter m : z.getMitarbeiter()) {
            zeilen.add(m.getId() + ";" + escape(m.getName()));
        }
        schreibe(MITARBEITER_DATEI, zeilen);

        zeilen = new ArrayList<>();
        for (Projekt p : z.getProjekte()) {
            zeilen.add(p.getId() + ";" + escape(p.getOrt()) + ";" + escape(p.getKunde()));
        }
        schreibe(PROJEKTE_DATEI, zeilen);

        zeilen = new ArrayList<>();
        for (Eintrag e : z.getEintraege()) {
            // Datum als ISO (2026-10-05), Stunden mit Punkt (Double.toString ist unabhängig von der Sprache)
            zeilen.add(e.getId() + ";" + e.getDatum() + ";" + e.getMitarbeiter().getId() + ";"
                    + e.getProjekt().getId() + ";" + e.getStunden() + ";" + escape(e.getBeschreibung()));
        }
        schreibe(EINTRAEGE_DATEI, zeilen);
    }

    private void schreibe(String datei, List<String> zeilen) throws IOException {
        Files.write(ordner.resolve(datei), zeilen, StandardCharsets.UTF_8);
    }

    // ---------- Laden ----------

    /** Lädt alle Daten. Fehlende Dateien ergeben leere Listen, kaputte Zeilen werden übersprungen. */
    public Zeiterfassung laden() {
        Zeiterfassung z = new Zeiterfassung();

        // Reihenfolge wichtig: Einträge brauchen schon geladene Mitarbeiter und Projekte
        for (String[] teile : lese(MITARBEITER_DATEI, 2)) {
            try {
                z.ladeMitarbeiter(Integer.parseInt(teile[0]), unescape(teile[1]));
            } catch (RuntimeException ex) {
                // kaputte Zeile überspringen
            }
        }

        for (String[] teile : lese(PROJEKTE_DATEI, 3)) {
            try {
                z.ladeProjekt(Integer.parseInt(teile[0]), unescape(teile[1]), unescape(teile[2]));
            } catch (RuntimeException ex) {
                // kaputte Zeile überspringen
            }
        }

        for (String[] teile : lese(EINTRAEGE_DATEI, 6)) {
            try {
                // Aus den IDs wieder die echten Objekte machen
                Mitarbeiter m = z.findeMitarbeiter(Integer.parseInt(teile[2]));
                Projekt p = z.findeProjekt(Integer.parseInt(teile[3]));
                if (m == null || p == null) {
                    continue; // verweist auf etwas, das es nicht gibt
                }
                z.ladeEintrag(Integer.parseInt(teile[0]), LocalDate.parse(teile[1]), m, p,
                        Double.parseDouble(teile[4]), unescape(teile[5]));
            } catch (RuntimeException ex) {
                // kaputte Zeile überspringen
            }
        }
        return z;
    }

    /** Liest eine Datei und gibt nur die Zeilen mit der richtigen Spaltenanzahl zurück. */
    private List<String[]> lese(String datei, int spalten) {
        List<String[]> ergebnis = new ArrayList<>();
        Path pfad = ordner.resolve(datei);
        if (!Files.exists(pfad)) {
            return ergebnis; // erster Start
        }
        try {
            for (String zeile : Files.readAllLines(pfad, StandardCharsets.UTF_8)) {
                String[] teile = zeile.split(";", -1);
                if (teile.length == spalten) {
                    ergebnis.add(teile);
                }
            }
        } catch (IOException ex) {
            // Datei nicht lesbar: so tun, als wäre sie leer
        }
        return ergebnis;
    }

    // ---------- Escaping ----------

    /** Macht aus \ ein \\ und aus ; ein \s, damit ; die Spalten nicht zerstört. */
    static String escape(String text) {
        return text.replace("\\", "\\\\").replace(";", "\\s");
    }

    /** Wandelt \\ und \s wieder zurück. */
    static String unescape(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                char naechstes = text.charAt(i + 1);
                if (naechstes == 's') {
                    sb.append(';');
                    i++;
                    continue;
                }
                if (naechstes == '\\') {
                    sb.append('\\');
                    i++;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
