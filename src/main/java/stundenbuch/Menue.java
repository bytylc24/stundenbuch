package stundenbuch;

import java.io.Console;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.function.Function;

/**
 * Ein- und Ausgabe im Terminal. Rechnet nicht und schreibt keine Dateien selbst.
 */
public class Menue {

    // Erlaubt 05.10.2026 und 5.10.2026; STRICT lehnt Daten wie 31.02.2026 ab
    private static final DateTimeFormatter DATUM_FORMAT =
            DateTimeFormatter.ofPattern("d.M.uuuu").withResolverStyle(ResolverStyle.STRICT);

    // Österreichisches Format: Komma als Dezimaltrennzeichen
    private static final Locale DEUTSCH = Locale.forLanguageTag("de-AT");

    // Anzeige: 05.10.2026 und mit Wochentag "Mo., 05.10.2026"
    private static final DateTimeFormatter DATUM_ANZEIGE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TAG_ANZEIGE = DateTimeFormatter.ofPattern("EE, dd.MM.yyyy", DEUTSCH);
    // Monat für die Auswertung, z. B. "Oktober 2026"
    private static final DateTimeFormatter MONAT_ANZEIGE = DateTimeFormatter.ofPattern("MMMM yyyy", DEUTSCH);

    private final Scanner scanner;
    private final Speicher speicher;
    private Zeiterfassung zeiterfassung;

    public Menue(Speicher speicher) {
        this.speicher = speicher;
        // Kodierung der Konsole verwenden, damit Umlaute unter Windows stimmen
        Console c = System.console();
        this.scanner = new Scanner(System.in, c != null ? c.charset() : Charset.defaultCharset());
    }

    /** Wird geworfen, wenn keine Eingabe mehr kommt (Strg+Z oder Ende der Testdatei). */
    private static class EingabeEnde extends RuntimeException {
    }

    public void start() {
        zeiterfassung = speicher.laden();
        System.out.println("Deine Daten liegen in: " + speicher.getOrdner());

        try {
            boolean weiter = true;
            while (weiter) {
                zeigeHauptmenue();
                int auswahl = leseZahl("Auswahl: ");
                switch (auswahl) {
                    case 1 -> eintragErfassen();
                    case 2 -> eintraegeAnzeigen();
                    case 3 -> eintragBearbeiten();
                    case 4 -> eintragLoeschen();
                    case 5 -> auswertungAnzeigen();
                    case 6 -> mitarbeiterVerwalten();
                    case 7 -> projekteVerwalten();
                    case 8 -> hilfeAnzeigen();
                    case 0 -> weiter = false;
                    default -> System.out.println("Bitte eine Zahl von 0 bis 8 eingeben.");
                }
            }
        } catch (EingabeEnde ex) {
            System.out.println();
            System.out.println("Eingabe beendet.");
        }

        speichern();
        System.out.println("Gespeichert. Auf Wiedersehen!");
    }

    private void zeigeHauptmenue() {
        System.out.println();
        System.out.println("===== STUNDENBUCH =====");
        System.out.println("1) Eintrag erfassen");
        System.out.println("2) Arbeitstage anzeigen");
        System.out.println("3) Eintrag bearbeiten");
        System.out.println("4) Eintrag löschen");
        System.out.println("5) Auswertung (Stunden pro Projekt)");
        System.out.println("6) Mitarbeiter verwalten");
        System.out.println("7) Projekte verwalten");
        System.out.println("8) Hilfe");
        System.out.println("0) Speichern und beenden");
    }

    // ---------- Menüpunkte ----------

    private void eintragErfassen() {
        if (zeiterfassung.getMitarbeiter().isEmpty() || zeiterfassung.getProjekte().isEmpty()) {
            System.out.println("Lege zuerst mindestens einen Mitarbeiter (6) und ein Projekt (7) an.");
            return;
        }
        LocalDate datum = leseDatum("Datum (TT.MM.JJJJ, leer = heute): ");
        Mitarbeiter m = waehle("Mitarbeiter", zeiterfassung.getMitarbeiter(), Mitarbeiter::getName);
        if (m == null) {
            System.out.println("Abgebrochen.");
            return;
        }
        Projekt p = waehle("Projekt", zeiterfassung.getProjekte(), Projekt::getName);
        if (p == null) {
            System.out.println("Abgebrochen.");
            return;
        }
        double stunden = leseStunden("Stunden: ");
        String beschreibung = leseText("Was wurde gemacht? ");

        Eintrag e = zeiterfassung.addEintrag(datum, m, p, stunden, beschreibung);
        speichern();
        System.out.println("Gespeichert: Eintrag #" + e.getId() + " (" + e.getDatum().format(DATUM_ANZEIGE)
                + ", " + m.getName() + ", " + p.getName() + ", " + formatStunden(stunden) + ")");
    }

    /** Zeigt alle Einträge gruppiert nach Tag, neuester Tag zuerst, mit Tagessumme. */
    private void eintraegeAnzeigen() {
        List<Eintrag> liste = zeiterfassung.getEintraegeNachDatum();
        if (liste.isEmpty()) {
            System.out.println("Noch keine Einträge.");
            return;
        }
        // Spaltenbreiten nach dem längsten Namen richten
        int breiteM = liste.stream().mapToInt(e -> e.getMitarbeiter().getName().length()).max().orElse(0);
        int breiteP = liste.stream().mapToInt(e -> e.getProjekt().getName().length()).max().orElse(0);

        LocalDate letzterTag = null;
        for (Eintrag e : liste) {
            if (!e.getDatum().equals(letzterTag)) {
                letzterTag = e.getDatum();
                System.out.println();
                System.out.println(letzterTag.format(TAG_ANZEIGE) + "   (gesamt "
                        + formatStunden(zeiterfassung.getStundenAmTag(letzterTag)) + ")");
                System.out.println("-".repeat(60));
            }
            System.out.printf("  %-6s %-" + breiteM + "s  %-" + breiteP + "s  %8s%n",
                    "[#" + e.getId() + "]", e.getMitarbeiter().getName(), e.getProjekt().getName(),
                    formatStunden(e.getStunden()));
            System.out.println("         " + e.getBeschreibung());
        }
    }

    /**
     * Zeigt die Arbeitstage und fragt nach der #-Nummer eines Eintrags.
     * Gibt null zurück bei 0 oder wenn es keine Einträge gibt.
     */
    private Eintrag waehleEintrag() {
        eintraegeAnzeigen();
        if (zeiterfassung.getEintraege().isEmpty()) {
            return null;
        }
        System.out.println();
        while (true) {
            int id = leseZahl("Nummer des Eintrags (#, 0 = abbrechen): ");
            if (id == 0) {
                return null;
            }
            Eintrag e = zeiterfassung.findeEintrag(id);
            if (e != null) {
                return e;
            }
            System.out.println("Es gibt keinen Eintrag #" + id + ".");
        }
    }

    /** Jedes Feld einzeln abfragen; leere Eingabe (bzw. 0 bei Listen) behält den alten Wert. */
    private void eintragBearbeiten() {
        Eintrag e = waehleEintrag();
        if (e == null) {
            return;
        }
        System.out.println("Eintrag #" + e.getId() + " bearbeiten (leere Eingabe = alten Wert behalten)");

        // Erst alles abfragen, dann auf einmal übernehmen: kein halb geänderter Eintrag
        LocalDate datum = leseDatum("Datum (alt: " + e.getDatum().format(DATUM_ANZEIGE) + "): ", e.getDatum());

        System.out.println("Mitarbeiter (alt: " + e.getMitarbeiter().getName() + ")");
        Mitarbeiter m = waehle("Mitarbeiter", zeiterfassung.getMitarbeiter(), Mitarbeiter::getName, "Behalten");
        if (m == null) {
            m = e.getMitarbeiter();
        }

        System.out.println("Projekt (alt: " + e.getProjekt().getName() + ")");
        Projekt p = waehle("Projekt", zeiterfassung.getProjekte(), Projekt::getName, "Behalten");
        if (p == null) {
            p = e.getProjekt();
        }

        double stunden = leseStunden("Stunden (alt: " + String.format(DEUTSCH, "%.1f", e.getStunden()) + "): ",
                e.getStunden());
        String beschreibung = leseText("Was wurde gemacht? (alt: " + e.getBeschreibung() + "): ",
                e.getBeschreibung());

        e.setDatum(datum);
        e.setMitarbeiter(m);
        e.setProjekt(p);
        e.setStunden(stunden);
        e.setBeschreibung(beschreibung);
        speichern();
        System.out.println("Gespeichert: Eintrag #" + e.getId() + " (" + datum.format(DATUM_ANZEIGE) + ", "
                + m.getName() + ", " + p.getName() + ", " + formatStunden(stunden) + ", " + beschreibung + ")");
    }

    private void eintragLoeschen() {
        Eintrag e = waehleEintrag();
        if (e == null) {
            return;
        }
        String frage = "Eintrag #" + e.getId() + " (" + e.getDatum().format(DATUM_ANZEIGE) + ", "
                + e.getMitarbeiter().getName() + ") wirklich löschen?";
        if (!bestaetige(frage)) {
            System.out.println("Nicht gelöscht.");
            return;
        }
        zeiterfassung.loescheEintrag(e);
        speichern();
        System.out.println("Eintrag #" + e.getId() + " gelöscht.");
    }

    /** Tabelle: Zeilen = Projekte, Spalten = Mitarbeiter, mit Gesamt und Summe. */
    private void auswertungAnzeigen() {
        List<Projekt> projekte = zeiterfassung.getProjekte();
        List<Mitarbeiter> mitarbeiter = zeiterfassung.getMitarbeiter();
        if (projekte.isEmpty() || mitarbeiter.isEmpty()) {
            System.out.println("Lege zuerst mindestens einen Mitarbeiter (6) und ein Projekt (7) an.");
            return;
        }

        // 1) Zeitraum wählen: nur Monate mit Einträgen, neueste zuerst
        List<YearMonth> monate = zeiterfassung.getMonateMitEintraegen();
        System.out.println("Zeitraum wählen:");
        System.out.println("  0) Gesamter Zeitraum");
        for (int i = 0; i < monate.size(); i++) {
            System.out.println("  " + (i + 1) + ") " + monate.get(i).format(MONAT_ANZEIGE));
        }
        int nr;
        while (true) {
            nr = leseZahl("Nummer: ");
            if (nr >= 0 && nr <= monate.size()) {
                break;
            }
            System.out.println("Bitte eine Zahl von 0 bis " + monate.size() + " eingeben.");
        }
        YearMonth monat = nr == 0 ? null : monate.get(nr - 1); // null = alle Monate

        // 2) Spaltenbreiten: erste Spalte nach dem längsten Projektnamen,
        //    Zahlenspalten alle gleich breit nach dem längsten Namen bzw. der längsten Zahl
        int breiteProjekt = "Projekt".length();
        for (Projekt p : projekte) {
            breiteProjekt = Math.max(breiteProjekt, p.getName().length());
        }
        int breiteZahl = Math.max("Gesamt".length(), formatStunden(zeiterfassung.getStundenGesamt(monat)).length());
        for (Mitarbeiter m : mitarbeiter) {
            breiteZahl = Math.max(breiteZahl, m.getName().length());
        }
        String formatProjekt = "%-" + breiteProjekt + "s";
        String formatZahl = "  %" + breiteZahl + "s";
        int gesamtBreite = breiteProjekt + (mitarbeiter.size() + 1) * (breiteZahl + 2);

        // 3) Tabelle ausgeben
        System.out.println();
        System.out.println("Auswertung: " + (monat == null ? "Gesamter Zeitraum" : monat.format(MONAT_ANZEIGE)));

        System.out.printf(formatProjekt, "Projekt");
        for (Mitarbeiter m : mitarbeiter) {
            System.out.printf(formatZahl, m.getName());
        }
        System.out.printf(formatZahl + "%n", "Gesamt");
        System.out.println("-".repeat(gesamtBreite));

        for (Projekt p : projekte) {
            System.out.printf(formatProjekt, p.getName());
            for (Mitarbeiter m : mitarbeiter) {
                System.out.printf(formatZahl, formatStunden(zeiterfassung.getStunden(p, m, monat)));
            }
            System.out.printf(formatZahl + "%n", formatStunden(zeiterfassung.getStundenGesamt(p, monat)));
        }

        System.out.println("=".repeat(gesamtBreite));
        System.out.printf(formatProjekt, "Summe");
        for (Mitarbeiter m : mitarbeiter) {
            System.out.printf(formatZahl, formatStunden(zeiterfassung.getStundenGesamt(m, monat)));
        }
        System.out.printf(formatZahl + "%n", formatStunden(zeiterfassung.getStundenGesamt(monat)));
    }

    private void mitarbeiterVerwalten() {
        while (true) {
            System.out.println();
            System.out.println("--- Mitarbeiter ---");
            List<Mitarbeiter> liste = zeiterfassung.getMitarbeiter();
            if (liste.isEmpty()) {
                System.out.println("  (noch keine)");
            }
            int breite = liste.stream().mapToInt(m -> m.getName().length()).max().orElse(0);
            for (Mitarbeiter m : liste) {
                System.out.printf("  %-" + breite + "s  %8s%n",
                        m.getName(), formatStunden(zeiterfassung.getStundenGesamt(m, null)));
            }
            System.out.println("1) Hinzufügen   2) Umbenennen   3) Löschen   0) Zurück");

            switch (leseZahl("Auswahl: ")) {
                case 1 -> mitarbeiterHinzufuegen();
                case 2 -> mitarbeiterUmbenennen();
                case 3 -> mitarbeiterLoeschen();
                case 0 -> {
                    return;
                }
                default -> System.out.println("Bitte eine Zahl von 0 bis 3 eingeben.");
            }
        }
    }

    private void mitarbeiterHinzufuegen() {
        String name = leseText("Name: ");
        if (zeiterfassung.addMitarbeiter(name) == null) {
            System.out.println("Den Namen " + name + " gibt es schon.");
            return;
        }
        speichern();
        System.out.println("Angelegt: " + name);
    }

    private void mitarbeiterUmbenennen() {
        Mitarbeiter m = waehle("Mitarbeiter", zeiterfassung.getMitarbeiter(), Mitarbeiter::getName);
        if (m == null) {
            return;
        }
        String neu = leseText("Neuer Name (alt: " + m.getName() + "): ", m.getName());
        // Nur die Schreibweise des eigenen Namens zu ändern ist erlaubt
        if (!neu.equalsIgnoreCase(m.getName()) && zeiterfassung.istMitarbeiterNameVergeben(neu)) {
            System.out.println("Den Namen " + neu + " gibt es schon.");
            return;
        }
        m.setName(neu);
        speichern();
        System.out.println("Umbenannt in: " + neu);
    }

    private void mitarbeiterLoeschen() {
        Mitarbeiter m = waehle("Mitarbeiter", zeiterfassung.getMitarbeiter(), Mitarbeiter::getName);
        if (m == null) {
            return;
        }
        if (!bestaetige("Mitarbeiter " + m.getName() + " wirklich löschen?")) {
            System.out.println("Nicht gelöscht.");
            return;
        }
        if (!zeiterfassung.loescheMitarbeiter(m)) {
            System.out.println("Geht nicht: " + m.getName() + " hat noch Einträge. Lösche diese zuerst.");
            return;
        }
        speichern();
        System.out.println("Gelöscht: " + m.getName());
    }

    private void projekteVerwalten() {
        while (true) {
            System.out.println();
            System.out.println("--- Projekte ---");
            List<Projekt> liste = zeiterfassung.getProjekte();
            if (liste.isEmpty()) {
                System.out.println("  (noch keine)");
            }
            int breite = liste.stream().mapToInt(p -> p.getName().length()).max().orElse(0);
            for (Projekt p : liste) {
                System.out.printf("  %-" + breite + "s  %8s%n",
                        p.getName(), formatStunden(zeiterfassung.getStundenGesamt(p, null)));
            }
            System.out.println("1) Hinzufügen   2) Umbenennen   3) Löschen   0) Zurück");

            switch (leseZahl("Auswahl: ")) {
                case 1 -> projektHinzufuegen();
                case 2 -> projektUmbenennen();
                case 3 -> projektLoeschen();
                case 0 -> {
                    return;
                }
                default -> System.out.println("Bitte eine Zahl von 0 bis 3 eingeben.");
            }
        }
    }

    private void projektHinzufuegen() {
        String ort = leseText("Ort: ");
        String kunde = leseText("Kunde: ");
        Projekt p = zeiterfassung.addProjekt(ort, kunde);
        if (p == null) {
            System.out.println("Das Projekt " + ort + "/" + kunde + " gibt es schon.");
            return;
        }
        speichern();
        System.out.println("Angelegt: " + p.getName());
    }

    private void projektUmbenennen() {
        Projekt p = waehle("Projekt", zeiterfassung.getProjekte(), Projekt::getName);
        if (p == null) {
            return;
        }
        String ort = leseText("Ort (alt: " + p.getOrt() + "): ", p.getOrt());
        String kunde = leseText("Kunde (alt: " + p.getKunde() + "): ", p.getKunde());
        String neuerName = ort + "/" + kunde;
        if (!neuerName.equalsIgnoreCase(p.getName()) && zeiterfassung.istProjektNameVergeben(ort, kunde)) {
            System.out.println("Das Projekt " + neuerName + " gibt es schon.");
            return;
        }
        p.setOrt(ort);
        p.setKunde(kunde);
        speichern();
        System.out.println("Umbenannt in: " + p.getName());
    }

    private void projektLoeschen() {
        Projekt p = waehle("Projekt", zeiterfassung.getProjekte(), Projekt::getName);
        if (p == null) {
            return;
        }
        if (!bestaetige("Projekt " + p.getName() + " wirklich löschen?")) {
            System.out.println("Nicht gelöscht.");
            return;
        }
        if (!zeiterfassung.loescheProjekt(p)) {
            System.out.println("Geht nicht: " + p.getName() + " hat noch Einträge. Lösche diese zuerst.");
            return;
        }
        speichern();
        System.out.println("Gelöscht: " + p.getName());
    }

    /** Erklärt das ganze Programm: Menüpunkte, Regeln und wo die Daten liegen. */
    private void hilfeAnzeigen() {
        System.out.println("""

                ===== HILFE =====
                Das Stundenbuch erfasst, wer wann an welchem Projekt wie viele Stunden
                gearbeitet hat und was gemacht wurde. Die Stunden werden pro Projekt und
                pro Mitarbeiter zusammengezählt.

                SO FÄNGST DU AN
                  1. Mitarbeiter anlegen (Menüpunkt 6)
                  2. Projekte anlegen (Menüpunkt 7), ein Projekt besteht aus Ort und Kunde
                  3. Einträge erfassen (Menüpunkt 1)

                DIE MENÜPUNKTE
                  1) Eintrag erfassen
                     Datum (TT.MM.JJJJ, leer = heute), Mitarbeiter, Projekt,
                     Stunden (z. B. 4,5, erlaubt: mehr als 0 bis 24) und Beschreibung.
                  2) Arbeitstage anzeigen
                     Alle Einträge nach Tag, neuester zuerst, mit Tagessumme.
                     Die Zahl nach # brauchst du zum Bearbeiten und Löschen.
                  3) Eintrag bearbeiten
                     #-Nummer wählen, dann jedes Feld neu eingeben.
                     Leere Eingabe (bzw. 0 bei Listen) behält den alten Wert.
                  4) Eintrag löschen
                     #-Nummer wählen und mit j bestätigen.
                  5) Auswertung
                     Tabelle Projekte x Mitarbeiter mit Gesamt und Summe.
                     Wähle den gesamten Zeitraum oder einen einzelnen Monat.
                  6) Mitarbeiter verwalten / 7) Projekte verwalten
                     Hinzufügen, umbenennen, löschen. Ein neuer Name gilt auch
                     für alle alten Einträge.
                  0) Speichern und beenden

                REGELN
                  - Doppelte Namen sind nicht erlaubt (Groß-/Kleinschreibung egal).
                  - Wer noch Einträge hat, kann nicht gelöscht werden.
                    Lösche zuerst die Einträge.
                  - In Listen bedeutet 0 immer: abbrechen bzw. behalten.
                  - Jede Änderung wird sofort gespeichert.
                """);
        System.out.println("Deine Daten liegen in: " + speicher.getOrdner());
        System.out.println("(mitarbeiter.csv, projekte.csv, eintraege.csv)");
    }

    // ---------- Formatieren ----------

    /** Stunden mit Komma und einer Nachkommastelle, z. B. "4,5 h". */
    private static String formatStunden(double stunden) {
        return String.format(DEUTSCH, "%.1f h", stunden);
    }

    // ---------- Speichern ----------

    /** Speichert sofort. Bei einem Fehler nur warnen, nicht abstürzen. */
    private void speichern() {
        try {
            speicher.speichern(zeiterfassung);
        } catch (IOException ex) {
            System.out.println("WARNUNG: Speichern fehlgeschlagen: " + ex.getMessage());
        }
    }

    // ---------- Eingabe-Hilfsmethoden ----------

    /** Liest eine Zeile (ohne Leerzeichen am Rand). Wirft EingabeEnde, wenn nichts mehr kommt. */
    private String leseZeile(String text) {
        System.out.print(text);
        if (!scanner.hasNextLine()) {
            throw new EingabeEnde();
        }
        // PowerShell setzt beim Weiterleiten mit | ein unsichtbares BOM (Zeichen U+FEFF) an den Anfang
        return scanner.nextLine().replace(String.valueOf((char) 0xFEFF), "").trim();
    }

    /** Fragt eine Zahl ab, bis eine gültige ganze Zahl eingegeben wird. */
    private int leseZahl(String text) {
        while (true) {
            String eingabe = leseZeile(text);
            try {
                return Integer.parseInt(eingabe);
            } catch (NumberFormatException ex) {
                System.out.println("Bitte eine ganze Zahl eingeben.");
            }
        }
    }

    /** Fragt einen Text ab, der nicht leer sein darf. */
    private String leseText(String text) {
        return leseText(text, null);
    }

    /** Fragt einen Text ab. Leere Eingabe ergibt den Standardwert (null = Eingabe Pflicht). */
    private String leseText(String text, String standard) {
        while (true) {
            String eingabe = leseZeile(text);
            if (!eingabe.isEmpty()) {
                return eingabe;
            }
            if (standard != null) {
                return standard;
            }
            System.out.println("Die Eingabe darf nicht leer sein.");
        }
    }

    /** Fragt Stunden ab: größer 0 bis höchstens 24, Komma oder Punkt erlaubt. */
    private double leseStunden(String text) {
        return leseStunden(text, null);
    }

    /** Wie oben, leere Eingabe ergibt den Standardwert (null = Eingabe Pflicht). */
    private double leseStunden(String text, Double standard) {
        while (true) {
            String eingabe = leseZeile(text);
            if (eingabe.isEmpty() && standard != null) {
                return standard;
            }
            try {
                double stunden = Double.parseDouble(eingabe.replace(',', '.'));
                // Diese Prüfung lehnt auch "NaN" und "Infinity" ab
                if (stunden > 0 && stunden <= 24) {
                    return stunden;
                }
                System.out.println("Die Stunden müssen größer als 0 und höchstens 24 sein.");
            } catch (NumberFormatException ex) {
                System.out.println("Bitte eine Zahl eingeben, z. B. 4,5");
            }
        }
    }

    /** Fragt ein Datum TT.MM.JJJJ ab. Leere Eingabe ergibt heute. */
    private LocalDate leseDatum(String text) {
        return leseDatum(text, LocalDate.now());
    }

    /** Fragt ein Datum TT.MM.JJJJ ab. Leere Eingabe ergibt den Standardwert. */
    private LocalDate leseDatum(String text, LocalDate standard) {
        while (true) {
            String eingabe = leseZeile(text);
            if (eingabe.isEmpty()) {
                return standard;
            }
            try {
                return LocalDate.parse(eingabe, DATUM_FORMAT);
            } catch (DateTimeParseException ex) {
                System.out.println("Ungültiges Datum. Bitte im Format TT.MM.JJJJ eingeben, z. B. 05.10.2026");
            }
        }
    }

    /** Fragt j oder n ab. */
    private boolean bestaetige(String frage) {
        while (true) {
            String eingabe = leseZeile(frage + " (j/n) ").toLowerCase();
            if (eingabe.equals("j")) {
                return true;
            }
            if (eingabe.equals("n")) {
                return false;
            }
            System.out.println("Bitte j oder n eingeben.");
        }
    }

    /**
     * Zeigt eine nummerierte Liste und lässt ein Element wählen.
     * Gibt null zurück bei 0 (abbrechen) oder wenn die Liste leer ist.
     */
    private <T> T waehle(String was, List<T> liste, Function<T, String> anzeige) {
        return waehle(was, liste, anzeige, "Abbrechen");
    }

    /** Wie oben, aber mit eigenem Text für 0, z. B. "Behalten" beim Bearbeiten. */
    private <T> T waehle(String was, List<T> liste, Function<T, String> anzeige, String textFuerNull) {
        if (liste.isEmpty()) {
            System.out.println("Keine " + was + " vorhanden.");
            return null;
        }
        System.out.println(was + " wählen:");
        for (int i = 0; i < liste.size(); i++) {
            System.out.println("  " + (i + 1) + ") " + anzeige.apply(liste.get(i)));
        }
        System.out.println("  0) " + textFuerNull);
        while (true) {
            int nr = leseZahl("Nummer: ");
            if (nr == 0) {
                return null;
            }
            if (nr >= 1 && nr <= liste.size()) {
                return liste.get(nr - 1);
            }
            System.out.println("Bitte eine Zahl von 0 bis " + liste.size() + " eingeben.");
        }
    }
}
