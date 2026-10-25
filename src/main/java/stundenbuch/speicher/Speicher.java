package stundenbuch.speicher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import stundenbuch.modell.Eintrag;
import stundenbuch.modell.Mitarbeiter;
import stundenbuch.modell.Projekt;
import stundenbuch.modell.Zeiterfassung;

/**
 * Liest und schreibt die drei CSV-Dateien (Trennzeichen ;, UTF-8).
 * eintraege.csv hat eine Kopfzeile, die die Spalten erklärt.
 * Redet nicht mit dem Benutzer: Fehler beim Speichern werden als Exception weitergegeben.
 */
public class Speicher {

    private static final String MITARBEITER_DATEI = "mitarbeiter.csv";
    private static final String PROJEKTE_DATEI = "projekte.csv";
    private static final String EINTRAEGE_DATEI = "eintraege.csv";

    // Beginnt bewusst mit "Nr" und nicht mit "ID": Excel hält Dateien, die mit "ID" anfangen, für SYLK-Dateien
    // Von und Bis (Uhrzeit) stehen hinten, damit ältere Dateien mit 6 Spalten weiter ladbar bleiben
    private static final String EINTRAEGE_KOPFZEILE = "Nr;Datum;Mitarbeiter;ProjektNr;Stunden;Beschreibung;Von;Bis";

    private static final int SICHERUNGEN_BEHALTEN = 14;

    private final Path ordner;

    // Was beim Laden nicht geklappt hat. Damit wir nie eine unvollständig geladene Datei überschreiben.
    private int uebersprungen;
    private boolean nichtLesbar;

    /**
     * Speichert im Ordner "Stundenbuch-Daten" auf dem Desktop. Bewusst nicht "Stundenbuch",
     * weil der App-Ordner aus der ZIP so heißt: Daten und App sollen nie im selben Ordner landen.
     */
    public Speicher() {
        this.ordner = findeDesktop().resolve("Stundenbuch-Daten").toAbsolutePath();
    }

    /**
     * Sucht den Desktop. Ist die OneDrive-Sicherung aktiv, wurde er nach OneDrive verschoben
     * und C:\Users\Name\Desktop gibt es nicht mehr.
     */
    private static Path findeDesktop() {
        Path home = Path.of(System.getProperty("user.home")).toAbsolutePath();
        Path desktop = home.resolve("Desktop");
        if (Files.isDirectory(desktop)) {
            return desktop;
        }
        // OneDrive nur nehmen, wenn es im Benutzerordner liegt. Sonst würde ein Probelauf
        // mit -Duser.home=testdaten in den echten OneDrive-Ordner schreiben.
        String oneDrive = System.getenv("OneDrive");
        if (oneDrive != null) {
            Path oneDriveDesktop = Path.of(oneDrive, "Desktop").toAbsolutePath();
            if (oneDriveDesktop.startsWith(home) && Files.isDirectory(oneDriveDesktop)) {
                return oneDriveDesktop;
            }
        }
        return desktop; // wird beim Speichern angelegt
    }

    public Path getOrdner() {
        return ordner;
    }

    /** Unterordner für gespeicherte Berichte (.docx, .csv), getrennt von den Daten. */
    public Path getBerichtOrdner() {
        return ordner.resolve("Berichte");
    }

    // ---------- Speichern ----------

    public void speichern(Zeiterfassung z) throws IOException {
        if (nichtLesbar) {
            // Sonst würde "nicht lesbar" wie "leer" aussehen und die echte Datei mit nichts überschrieben
            throw new IOException("Beim Start war eine Datei nicht lesbar (in Excel offen?). "
                    + "Zum Schutz deiner Daten wird nichts gespeichert. Datei schließen und das Programm neu starten.");
        }
        Files.createDirectories(ordner);

        List<String> zeilen = new ArrayList<>();
        for (Mitarbeiter m : z.getMitarbeiter()) {
            zeilen.add(m.getKennung() + ";" + escape(m.getName()));
        }
        List<String> mitarbeiterZeilen = zeilen;

        zeilen = new ArrayList<>();
        for (Projekt p : z.getProjekte()) {
            zeilen.add(p.getId() + ";" + escape(p.getOrt()) + ";" + escape(p.getKunde()));
        }
        List<String> projektZeilen = zeilen;

        zeilen = new ArrayList<>();
        zeilen.add(EINTRAEGE_KOPFZEILE);
        for (Eintrag e : z.getEintraege()) {
            // Datum als ISO (2026-10-05), Stunden mit Punkt (Double.toString ist unabhängig von der Sprache)
            zeilen.add(e.getId() + ";" + e.getDatum() + ";" + e.getMitarbeiter().getKennung() + ";"
                    + e.getProjekt().getId() + ";" + e.getStunden() + ";" + escape(e.getBeschreibung()) + ";"
                    + (e.getVon() == null ? "" : e.getVon()) + ";" + (e.getBis() == null ? "" : e.getBis()));
        }

        // Erst alle drei in Hilfsdateien schreiben, dann austauschen: Geht beim Schreiben etwas schief
        // (z. B. Datei in Excel offen), bleiben die alten Dateien vollständig und zusammenpassend.
        schreibeHilfsdatei(MITARBEITER_DATEI, mitarbeiterZeilen);
        schreibeHilfsdatei(PROJEKTE_DATEI, projektZeilen);
        schreibeHilfsdatei(EINTRAEGE_DATEI, zeilen);
        for (String datei : List.of(MITARBEITER_DATEI, PROJEKTE_DATEI, EINTRAEGE_DATEI)) {
            Files.move(ordner.resolve(datei + ".neu"), ordner.resolve(datei), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void schreibeHilfsdatei(String datei, List<String> zeilen) throws IOException {
        Files.write(ordner.resolve(datei + ".neu"), zeilen, StandardCharsets.UTF_8);
    }

    // ---------- Sicherung ----------

    /**
     * Kopiert die drei Dateien beim ersten Start des Tages nach Sicherung/JJJJ-MM-TT.
     * Läuft vor dem Laden, damit die Kopie sicher den Stand von vor allen Änderungen zeigt.
     * Es bleiben die letzten 14 Tage. Fehler dabei sind egal: Eine Sicherung darf den Start nie verhindern.
     */
    private void sicherungMachen() {
        try {
            Path ziel = ordner.resolve("Sicherung").resolve(LocalDate.now().toString());
            if (Files.exists(ziel) || !Files.exists(ordner.resolve(EINTRAEGE_DATEI))) {
                return;
            }
            Files.createDirectories(ziel);
            for (String datei : List.of(MITARBEITER_DATEI, PROJEKTE_DATEI, EINTRAEGE_DATEI)) {
                if (Files.exists(ordner.resolve(datei))) {
                    Files.copy(ordner.resolve(datei), ziel.resolve(datei));
                }
            }
            try (var tage = Files.list(ordner.resolve("Sicherung"))) {
                List<Path> alle = tage.sorted().toList();
                for (Path alt : alle.subList(0, Math.max(0, alle.size() - SICHERUNGEN_BEHALTEN))) {
                    try (var dateien = Files.list(alt)) {
                        for (Path f : dateien.toList()) {
                            Files.delete(f);
                        }
                    }
                    Files.delete(alt);
                }
            }
        } catch (IOException | RuntimeException ex) {
            // keine Sicherung, aber das Programm startet trotzdem
        }
    }

    // ---------- Laden ----------

    /**
     * Gibt einen Warntext zurück, wenn beim Laden etwas nicht gestimmt hat, sonst null.
     * Gilt für den letzten Aufruf von laden().
     */
    public String getLadeWarnung() {
        if (nichtLesbar) {
            return "WARNUNG: Eine Datei konnte nicht gelesen werden (in Excel offen?). Es wird nichts gespeichert. "
                    + "Datei schließen und das Programm neu starten.";
        }
        if (uebersprungen > 0) {
            return "WARNUNG: " + uebersprungen + " Zeile(n) in den Daten konnten nicht geladen werden "
                    + "(Original in " + ordner.resolve("Sicherung") + ").";
        }
        return null;
    }

    /** Lädt alle Daten. Fehlende Dateien ergeben leere Listen, kaputte Zeilen werden übersprungen und gezählt. */
    public Zeiterfassung laden() {
        sicherungMachen();
        uebersprungen = 0;
        nichtLesbar = false;
        Zeiterfassung z = new Zeiterfassung();

        // Reihenfolge wichtig: Einträge brauchen schon geladene Mitarbeiter und Projekte
        for (String[] teile : lese(MITARBEITER_DATEI, 2)) {
            try {
                z.ladeMitarbeiter(idAusKennung(teile[0]), unescape(teile[1]));
            } catch (RuntimeException ex) {
                uebersprungen++; // kaputte Zeile überspringen
            }
        }

        for (String[] teile : lese(PROJEKTE_DATEI, 3)) {
            try {
                z.ladeProjekt(Integer.parseInt(teile[0]), unescape(teile[1]), unescape(teile[2]));
            } catch (RuntimeException ex) {
                uebersprungen++;
            }
        }

        // 6 Spalten (ältere Dateien, ohne Uhrzeit) oder 8 Spalten (mit Von und Bis)
        for (String[] teile : lese(EINTRAEGE_DATEI, 6)) {
            if (teile[0].equals("Nr")) {
                continue; // Kopfzeile ist kein Eintrag (egal ob alte oder neue)
            }
            try {
                // Aus Kennung und Projektnummer wieder die echten Objekte machen
                Mitarbeiter m = z.findeMitarbeiter(idAusKennung(teile[2]));
                Projekt p = z.findeProjekt(Integer.parseInt(teile[3]));
                if (m == null || p == null) {
                    uebersprungen++; // verweist auf etwas, das es nicht gibt
                    continue;
                }
                LocalTime von = null;
                LocalTime bis = null;
                if (teile.length >= 8 && !teile[6].isEmpty() && !teile[7].isEmpty()) {
                    von = LocalTime.parse(teile[6]);
                    bis = LocalTime.parse(teile[7]);
                }
                z.ladeEintrag(Integer.parseInt(teile[0]), LocalDate.parse(teile[1]), m, p,
                        Double.parseDouble(teile[4]), unescape(teile[5]), von, bis);
            } catch (RuntimeException ex) {
                uebersprungen++;
            }
        }
        return z;
    }

    /**
     * Liest eine Datei und gibt die Zeilen mit mindestens so vielen Spalten zurück.
     * Leere Zeilen sind egal, alle anderen mit falscher Spaltenanzahl werden gezählt.
     */
    private List<String[]> lese(String datei, int mindestSpalten) {
        List<String[]> ergebnis = new ArrayList<>();
        Path pfad = ordner.resolve(datei);
        if (!Files.exists(pfad)) {
            return ergebnis; // erster Start
        }
        try {
            for (String zeile : Files.readAllLines(pfad, StandardCharsets.UTF_8)) {
                if (zeile.isBlank()) {
                    continue;
                }
                String[] teile = zeile.split(";", -1);
                if (teile.length >= mindestSpalten) {
                    ergebnis.add(teile);
                } else {
                    uebersprungen++;
                }
            }
        } catch (IOException ex) {
            // Datei da, aber nicht lesbar: NICHT so tun, als wäre sie leer (sonst wird sie später überschrieben)
            nichtLesbar = true;
        }
        return ergebnis;
    }

    /** Macht aus "AB_01" die Zahl 1. Ältere Dateien enthalten nur "1", das geht auch. */
    static int idAusKennung(String text) {
        if (text.startsWith(Mitarbeiter.KENNUNG_VORSILBE)) {
            text = text.substring(Mitarbeiter.KENNUNG_VORSILBE.length());
        }
        return Integer.parseInt(text);
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
