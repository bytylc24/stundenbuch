package stundenbuch;

import java.io.Console;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

/**
 * Ein- und Ausgabe im Terminal. Rechnet nicht und schreibt keine Dateien selbst.
 * Wie etwas aussieht (Rahmen, Farben, Tabellen), steht in Konsole.
 */
public class Menue {

    // Erlaubt 05.10.2026 und 5.10.2026; STRICT lehnt Daten wie 31.02.2026 ab
    private static final DateTimeFormatter DATUM_FORMAT =
            DateTimeFormatter.ofPattern("d.M.uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final Scanner scanner;
    private final Speicher speicher;
    private Zeiterfassung zeiterfassung;

    // Letzte Meldung (z. B. "Gespeichert: ..."). Weil das Fenster vor jedem Bildschirm geleert wird,
    // zeigt der nächste Bildschirm sie noch einmal an. Jede neue Eingabe löscht sie.
    private String meldung;

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
        hinweis("Deine Daten liegen in: " + speicher.getOrdner());

        try {
            boolean weiter = true;
            while (weiter) {
                Konsole.leeren();
                zeigeHauptmenue();
                int auswahl = leseZahl("Auswahl: ");
                switch (auswahl) {
                    case 1 -> eintragErfassen();
                    case 2 -> {
                        eintraegeAnzeigen();
                        weiterMitEnter();
                    }
                    case 3 -> eintragBearbeiten();
                    case 4 -> eintragLoeschen();
                    case 5 -> {
                        auswertungAnzeigen();
                        weiterMitEnter();
                    }
                    case 6 -> mitarbeiterVerwalten();
                    case 7 -> projekteVerwalten();
                    case 8 -> {
                        hilfeAnzeigen();
                        weiterMitEnter();
                    }
                    case 9 -> berichtErstellen();
                    case 0 -> weiter = false;
                    default -> fehler("Bitte eine Zahl von 0 bis 9 eingeben.");
                }
            }
        } catch (EingabeEnde ex) {
            System.out.println();
            System.out.println("Eingabe beendet.");
        }

        speichern();
        ok("Gespeichert. Auf Wiedersehen!");
    }

    /**
     * Hauptmenü nach Themen gruppiert. Die Nummern bleiben wie früher (Bericht kam als 9 dazu),
     * damit sich niemand umgewöhnen muss.
     */
    private void zeigeHauptmenue() {
        System.out.println();
        Konsole.titelBox("STUNDENBUCH", LocalDate.now().format(Format.TAG)).forEach(System.out::println);
        YearMonth jetzt = YearMonth.now();
        System.out.println(Konsole.grau("  " + zeiterfassung.getMitarbeiter().size() + " Mitarbeiter · "
                + zeiterfassung.getProjekte().size() + " Projekte · "
                + zeiterfassung.getEintraege().size() + " Einträge · "
                + jetzt.format(Format.MONAT) + ": " + Format.stunden(zeiterfassung.getStundenGesamt(jetzt))));
        zeigeMeldung();
        System.out.println();
        System.out.println(Konsole.akzent("  ERFASSEN"));
        menuepunkt(1, "Eintrag erfassen");
        menuepunkt(2, "Arbeitstage anzeigen");
        menuepunkt(3, "Eintrag bearbeiten");
        menuepunkt(4, "Eintrag löschen");
        System.out.println(Konsole.akzent("  AUSWERTEN"));
        menuepunkt(5, "Auswertung (Stunden pro Projekt)");
        menuepunkt(9, "Bericht ansehen und speichern (Word/CSV)");
        System.out.println(Konsole.akzent("  VERWALTEN"));
        menuepunkt(6, "Mitarbeiter verwalten");
        menuepunkt(7, "Projekte verwalten");
        menuepunkt(8, "Hilfe");
        menuepunkt(0, "Speichern und beenden");
        System.out.println();
    }

    private static void menuepunkt(int nr, String text) {
        System.out.println("    " + Konsole.fett(nr + ")") + " " + text);
    }

    // ---------- Menüpunkte ----------

    private void eintragErfassen() {
        titel("Eintrag erfassen");
        if (zeiterfassung.getMitarbeiter().isEmpty() || zeiterfassung.getProjekte().isEmpty()) {
            fehler("Lege zuerst mindestens einen Mitarbeiter (6) und ein Projekt (7) an.");
            return;
        }
        LocalDate datum = leseDatum("Datum (TT.MM.JJJJ, leer = heute): ");
        Mitarbeiter m = waehle("Mitarbeiter", zeiterfassung.getMitarbeiter(), Mitarbeiter::getName);
        if (m == null) {
            hinweis("Abgebrochen.");
            return;
        }
        Projekt p = waehle("Projekt", zeiterfassung.getProjekte(), Projekt::getName);
        if (p == null) {
            hinweis("Abgebrochen.");
            return;
        }
        double stunden = leseStunden("Stunden: ");
        String beschreibung = leseText("Was wurde gemacht? ");

        Eintrag e = zeiterfassung.addEintrag(datum, m, p, stunden, beschreibung);
        speichern();
        ok("Gespeichert: Eintrag #" + e.getId() + " (" + e.getDatum().format(Format.DATUM)
                + ", " + m.getName() + ", " + p.getName() + ", " + Format.stunden(stunden) + ")");
    }

    /** Zeigt alle Einträge gruppiert nach Tag, neuester Tag zuerst, mit Tagessumme. */
    private void eintraegeAnzeigen() {
        titel("Arbeitstage");
        List<Eintrag> liste = zeiterfassung.getEintraegeNachDatum();
        if (liste.isEmpty()) {
            hinweis("Noch keine Einträge.");
            return;
        }
        // Spaltenbreiten nach dem längsten Namen richten
        int breiteM = liste.stream().mapToInt(e -> e.getMitarbeiter().getName().length()).max().orElse(0);
        int breiteP = liste.stream().mapToInt(e -> e.getProjekt().getName().length()).max().orElse(0);

        LocalDate letzterTag = null;
        for (Eintrag e : liste) {
            if (!e.getDatum().equals(letzterTag)) {
                letzterTag = e.getDatum();
                String tag = letzterTag.format(Format.TAG);
                String summe = "gesamt " + Format.stunden(zeiterfassung.getStundenAmTag(letzterTag));
                System.out.println();
                System.out.println("  " + Konsole.akzent(tag)
                        + " ".repeat(Math.max(2, Konsole.BREITE - 2 - tag.length() - summe.length()))
                        + Konsole.fett(summe));
                System.out.println("  " + Konsole.grau("─".repeat(Konsole.BREITE - 2)));
            }
            System.out.println("  " + Konsole.grau(Konsole.links("#" + e.getId(), 5)) + " "
                    + Konsole.links(e.getMitarbeiter().getName(), breiteM) + "  "
                    + Konsole.links(e.getProjekt().getName(), breiteP) + "  "
                    + Konsole.rechts(Format.stunden(e.getStunden()), 8));
            for (String zeile : Konsole.umbrechen(e.getBeschreibung(), Konsole.BREITE - 8)) {
                System.out.println("        " + Konsole.grau(zeile));
            }
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
            fehler("Es gibt keinen Eintrag #" + id + ".");
        }
    }

    /** Jedes Feld einzeln abfragen; leere Eingabe (bzw. 0 bei Listen) behält den alten Wert. */
    private void eintragBearbeiten() {
        Eintrag e = waehleEintrag();
        if (e == null) {
            return;
        }
        System.out.println();
        System.out.println(Konsole.ueberschrift("Eintrag #" + e.getId() + " bearbeiten"));
        hinweis("Leere Eingabe = alten Wert behalten");

        // Erst alles abfragen, dann auf einmal übernehmen: kein halb geänderter Eintrag
        LocalDate datum = leseDatum("Datum (alt: " + e.getDatum().format(Format.DATUM) + "): ", e.getDatum());

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

        double stunden = leseStunden("Stunden (alt: " + String.format(Format.DEUTSCH, "%.1f", e.getStunden()) + "): ",
                e.getStunden());
        String beschreibung = leseText("Was wurde gemacht? (alt: " + e.getBeschreibung() + "): ",
                e.getBeschreibung());

        e.setDatum(datum);
        e.setMitarbeiter(m);
        e.setProjekt(p);
        e.setStunden(stunden);
        e.setBeschreibung(beschreibung);
        speichern();
        ok("Gespeichert: Eintrag #" + e.getId() + " (" + datum.format(Format.DATUM) + ", "
                + m.getName() + ", " + p.getName() + ", " + Format.stunden(stunden) + ", " + beschreibung + ")");
    }

    private void eintragLoeschen() {
        Eintrag e = waehleEintrag();
        if (e == null) {
            return;
        }
        String frage = "Eintrag #" + e.getId() + " (" + e.getDatum().format(Format.DATUM) + ", "
                + e.getMitarbeiter().getName() + ") wirklich löschen?";
        if (!bestaetige(frage)) {
            hinweis("Nicht gelöscht.");
            return;
        }
        zeiterfassung.loescheEintrag(e);
        speichern();
        ok("Eintrag #" + e.getId() + " gelöscht.");
    }

    /** Tabelle: Zeilen = Projekte, Spalten = Mitarbeiter, mit Gesamt und Summe. */
    private void auswertungAnzeigen() {
        titel("Auswertung");
        List<Projekt> projekte = zeiterfassung.getProjekte();
        List<Mitarbeiter> mitarbeiter = zeiterfassung.getMitarbeiter();
        if (projekte.isEmpty() || mitarbeiter.isEmpty()) {
            fehler("Lege zuerst mindestens einen Mitarbeiter (6) und ein Projekt (7) an.");
            return;
        }

        // 1) Zeitraum wählen: nur Monate mit Einträgen, neueste zuerst
        List<YearMonth> monate = zeiterfassung.getMonateMitEintraegen();
        System.out.println("Zeitraum wählen:");
        System.out.println("  " + Konsole.fett("0)") + " Gesamter Zeitraum");
        for (int i = 0; i < monate.size(); i++) {
            System.out.println("  " + Konsole.fett((i + 1) + ")") + " " + monate.get(i).format(Format.MONAT));
        }
        int nr = leseZahl("Nummer: ", 0, monate.size());
        YearMonth monat = nr == 0 ? null : monate.get(nr - 1); // null = alle Monate

        // 2) Tabelle bauen und zeichnen
        Tabelle t = new Tabelle("Stunden pro Projekt").spalte("Projekt");
        for (Mitarbeiter m : mitarbeiter) {
            t.zahlenSpalte(m.getName());
        }
        t.zahlenSpalte("Gesamt");
        for (Projekt p : projekte) {
            List<String> zeile = new ArrayList<>(List.of(p.getName()));
            for (Mitarbeiter m : mitarbeiter) {
                zeile.add(Format.zahl(zeiterfassung.getStunden(p, m, monat)));
            }
            zeile.add(Format.zahl(zeiterfassung.getStundenGesamt(p, monat)));
            t.zeile(zeile.toArray(String[]::new));
        }
        List<String> summe = new ArrayList<>(List.of("Summe"));
        for (Mitarbeiter m : mitarbeiter) {
            summe.add(Format.zahl(zeiterfassung.getStundenGesamt(m, monat)));
        }
        summe.add(Format.zahl(zeiterfassung.getStundenGesamt(monat)));
        t.summe(summe.toArray(String[]::new));

        titel("Auswertung: " + (monat == null ? "Gesamter Zeitraum" : monat.format(Format.MONAT)));
        hinweis("  Stunden pro Projekt und Mitarbeiter");
        zeige(t);
    }

    // ---------- Bericht ----------

    /** Bericht: Filter wählen, im Terminal ansehen und erst dann als Word oder CSV speichern. */
    private void berichtErstellen() {
        titel("Bericht");
        if (zeiterfassung.getEintraege().isEmpty()) {
            fehler("Noch keine Einträge. Erfasse zuerst Stunden (1).");
            return;
        }
        while (true) {
            Bericht b = berichtFilterWaehlen();
            if (b == null) {
                hinweis("Abgebrochen.");
                return;
            }
            berichtAnzeigen(b);

            // Ansehen, dann entscheiden: speichern, Filter ändern oder zurück
            boolean filterAendern = false;
            while (!filterAendern) {
                System.out.println();
                if (b.istLeer()) {
                    System.out.println(Konsole.fett("1)") + " Filter ändern   " + Konsole.fett("0)") + " Zurück");
                    if (leseZahl("Auswahl: ", 0, 1) == 0) {
                        return;
                    }
                    filterAendern = true;
                    continue;
                }
                System.out.println(Konsole.fett("1)") + " Als Word speichern (.docx)   "
                        + Konsole.fett("2)") + " Als CSV speichern (Excel)   " + Konsole.fett("3)") + " Beides");
                System.out.println(Konsole.fett("4)") + " Filter ändern   " + Konsole.fett("0)") + " Zurück");
                switch (leseZahl("Auswahl: ", 0, 4)) {
                    case 1 -> oeffnenAnbieten(berichtSpeichern(b, ".docx"), false);
                    case 2 -> oeffnenAnbieten(berichtSpeichern(b, ".csv"), false);
                    case 3 -> {
                        Path docx = berichtSpeichern(b, ".docx");
                        Path csv = berichtSpeichern(b, ".csv");
                        if (docx != null || csv != null) {
                            oeffnenAnbieten(speicher.getBerichtOrdner(), true);
                        }
                    }
                    case 4 -> filterAendern = true;
                    case 0 -> {
                        return;
                    }
                    default -> { /* leseZahl lässt nur 0 bis 4 zu */ }
                }
            }
        }
    }

    /** Fragt Zeitraum, Projekt und Mitarbeiter ab. Gibt null zurück, wenn abgebrochen wurde. */
    private Bericht berichtFilterWaehlen() {
        titel("Bericht: Filter");
        System.out.println("Zeitraum wählen:");
        System.out.println("  " + Konsole.fett("1)") + " Gesamter Zeitraum");
        System.out.println("  " + Konsole.fett("2)") + " Ein Jahr");
        System.out.println("  " + Konsole.fett("3)") + " Ein Monat");
        System.out.println("  " + Konsole.fett("4)") + " Eigener Zeitraum (von - bis)");
        System.out.println("  " + Konsole.fett("0)") + " Abbrechen");

        Bericht.Zeitraum zeitraum;
        switch (leseZahl("Nummer: ", 0, 4)) {
            case 1 -> zeitraum = Bericht.Zeitraum.gesamt();
            case 2 -> {
                Integer jahr = waehle("Jahr", zeiterfassung.getJahreMitEintraegen(), String::valueOf);
                if (jahr == null) {
                    return null;
                }
                zeitraum = Bericht.Zeitraum.jahr(jahr);
            }
            case 3 -> {
                YearMonth monat = waehle("Monat", zeiterfassung.getMonateMitEintraegen(),
                        m -> m.format(Format.MONAT));
                if (monat == null) {
                    return null;
                }
                zeitraum = Bericht.Zeitraum.monat(monat);
            }
            case 4 -> {
                LocalDate von = leseDatum("Von (TT.MM.JJJJ, leer = heute): ");
                LocalDate bis;
                while (true) {
                    bis = leseDatum("Bis (TT.MM.JJJJ, leer = heute): ");
                    if (!bis.isBefore(von)) {
                        break;
                    }
                    fehler("Das Ende darf nicht vor dem Anfang (" + von.format(Format.DATUM) + ") liegen.");
                }
                zeitraum = Bericht.Zeitraum.vonBis(von, bis);
            }
            default -> {
                return null;
            }
        }

        // 0 heißt hier nicht "abbrechen", sondern "kein Filter"
        Projekt p = waehle("Projekt", zeiterfassung.getProjekte(), Projekt::getName, "Alle Projekte");
        Mitarbeiter m = waehle("Mitarbeiter", zeiterfassung.getMitarbeiter(), Mitarbeiter::getName, "Alle Mitarbeiter");
        return new Bericht(zeiterfassung, zeitraum, p, m);
    }

    /** Vorschau im Terminal, aufgebaut wie die Word-Datei. */
    private void berichtAnzeigen(Bericht b) {
        Konsole.leeren();
        System.out.println();
        Konsole.titelBox(b.getTitel().toUpperCase(Format.DEUTSCH), "Vorschau").forEach(System.out::println);
        for (String[] a : b.getAngaben()) {
            System.out.println("  " + Konsole.grau(Konsole.links(a[0], 13)) + a[1]);
        }
        if (b.istLeer()) {
            System.out.println();
            fehler("Keine Einträge für diese Auswahl.");
            return;
        }
        System.out.println();
        Konsole.kacheln(b.getKennzahlen()).forEach(z -> System.out.println("  " + z));
        for (Tabelle t : b.getTabellen()) {
            System.out.println();
            System.out.println("  " + Konsole.akzent(t.getTitel()));
            zeige(t);
        }
        System.out.println();
        hinweis("Dateiname: " + b.getDateiname() + ".docx / .csv");
    }

    /**
     * Speichert den Bericht im Ordner Berichte. Gibt es die Datei schon, wird gefragt;
     * bei "n" bekommt die neue Datei eine Nummer (_2, _3, ...). Gibt null zurück bei einem Fehler.
     */
    private Path berichtSpeichern(Bericht b, String endung) {
        Path ordner = speicher.getBerichtOrdner();
        Path datei = ordner.resolve(b.getDateiname() + endung);
        if (Files.exists(datei) && !bestaetige("Die Datei " + datei.getFileName() + " gibt es schon. Überschreiben?")) {
            for (int nr = 2; Files.exists(datei); nr++) {
                datei = ordner.resolve(b.getDateiname() + "_" + nr + endung);
            }
        }
        try {
            Files.createDirectories(ordner);
            if (endung.equals(".docx")) {
                BerichtExport.alsDocx(b, datei);
            } else {
                BerichtExport.alsCsv(b, datei);
            }
            ok("Gespeichert: " + datei);
            return datei;
        } catch (IOException ex) {
            fehler("Speichern fehlgeschlagen: " + ex.getMessage());
            hinweis("Ist die Datei gerade in Word oder Excel offen? Dann dort schließen und nochmal speichern.");
            return null;
        }
    }

    /** Bietet an, die Datei (oder den Ordner) gleich zu öffnen. Geht nur unter Windows. */
    private void oeffnenAnbieten(Path pfad, boolean istOrdner) {
        if (pfad == null || !System.getProperty("os.name", "").startsWith("Windows")) {
            return;
        }
        if (!bestaetige(istOrdner ? "Ordner jetzt öffnen?" : "Datei jetzt öffnen?")) {
            return;
        }
        try {
            // Der Explorer öffnet eine Datei mit dem passenden Programm (Word, Excel)
            new ProcessBuilder("explorer.exe", pfad.toString()).start();
        } catch (IOException ex) {
            fehler("Konnte nicht geöffnet werden: " + ex.getMessage());
        }
    }

    // ---------- Verwalten ----------

    private void mitarbeiterVerwalten() {
        while (true) {
            titel("Mitarbeiter");
            List<Mitarbeiter> liste = zeiterfassung.getMitarbeiter();
            if (liste.isEmpty()) {
                hinweis("  (noch keine)");
            } else {
                Tabelle t = new Tabelle("Mitarbeiter").spalte("Kennung").spalte("Name").zahlenSpalte("Stunden");
                for (Mitarbeiter m : liste) {
                    t.zeile(m.getKennung(), m.getName(), Format.zahl(zeiterfassung.getStundenGesamt(m, null)));
                }
                zeige(t);
            }
            zeigeMeldung();
            untermenue();

            switch (leseZahl("Auswahl: ")) {
                case 1 -> mitarbeiterHinzufuegen();
                case 2 -> mitarbeiterUmbenennen();
                case 3 -> mitarbeiterLoeschen();
                case 0 -> {
                    return;
                }
                default -> fehler("Bitte eine Zahl von 0 bis 3 eingeben.");
            }
        }
    }

    private void mitarbeiterHinzufuegen() {
        String name = leseText("Name: ");
        Mitarbeiter m = zeiterfassung.addMitarbeiter(name);
        if (m == null) {
            fehler("Den Namen " + name + " gibt es schon.");
            return;
        }
        speichern();
        ok("Angelegt: " + name + " (" + m.getKennung() + ")");
    }

    private void mitarbeiterUmbenennen() {
        Mitarbeiter m = waehle("Mitarbeiter", zeiterfassung.getMitarbeiter(), Mitarbeiter::getName);
        if (m == null) {
            return;
        }
        String neu = leseText("Neuer Name (alt: " + m.getName() + "): ", m.getName());
        // Nur die Schreibweise des eigenen Namens zu ändern ist erlaubt
        if (!neu.equalsIgnoreCase(m.getName()) && zeiterfassung.istMitarbeiterNameVergeben(neu)) {
            fehler("Den Namen " + neu + " gibt es schon.");
            return;
        }
        m.setName(neu);
        speichern();
        ok("Umbenannt in: " + neu);
    }

    private void mitarbeiterLoeschen() {
        Mitarbeiter m = waehle("Mitarbeiter", zeiterfassung.getMitarbeiter(), Mitarbeiter::getName);
        if (m == null) {
            return;
        }
        if (!bestaetige("Mitarbeiter " + m.getName() + " wirklich löschen?")) {
            hinweis("Nicht gelöscht.");
            return;
        }
        if (!zeiterfassung.loescheMitarbeiter(m)) {
            fehler("Geht nicht: " + m.getName() + " hat noch Einträge. Lösche diese zuerst.");
            return;
        }
        speichern();
        ok("Gelöscht: " + m.getName());
    }

    private void projekteVerwalten() {
        while (true) {
            titel("Projekte");
            List<Projekt> liste = zeiterfassung.getProjekte();
            if (liste.isEmpty()) {
                hinweis("  (noch keine)");
            } else {
                Tabelle t = new Tabelle("Projekte").spalte("Ort").spalte("Kunde").zahlenSpalte("Stunden");
                for (Projekt p : liste) {
                    t.zeile(p.getOrt(), p.getKunde(), Format.zahl(zeiterfassung.getStundenGesamt(p, null)));
                }
                zeige(t);
            }
            zeigeMeldung();
            untermenue();

            switch (leseZahl("Auswahl: ")) {
                case 1 -> projektHinzufuegen();
                case 2 -> projektUmbenennen();
                case 3 -> projektLoeschen();
                case 0 -> {
                    return;
                }
                default -> fehler("Bitte eine Zahl von 0 bis 3 eingeben.");
            }
        }
    }

    private void projektHinzufuegen() {
        String ort = leseText("Ort: ");
        String kunde = leseText("Kunde: ");
        Projekt p = zeiterfassung.addProjekt(ort, kunde);
        if (p == null) {
            fehler("Das Projekt " + ort + "/" + kunde + " gibt es schon.");
            return;
        }
        speichern();
        ok("Angelegt: " + p.getName());
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
            fehler("Das Projekt " + neuerName + " gibt es schon.");
            return;
        }
        p.setOrt(ort);
        p.setKunde(kunde);
        speichern();
        ok("Umbenannt in: " + p.getName());
    }

    private void projektLoeschen() {
        Projekt p = waehle("Projekt", zeiterfassung.getProjekte(), Projekt::getName);
        if (p == null) {
            return;
        }
        if (!bestaetige("Projekt " + p.getName() + " wirklich löschen?")) {
            hinweis("Nicht gelöscht.");
            return;
        }
        if (!zeiterfassung.loescheProjekt(p)) {
            fehler("Geht nicht: " + p.getName() + " hat noch Einträge. Lösche diese zuerst.");
            return;
        }
        speichern();
        ok("Gelöscht: " + p.getName());
    }

    private static void untermenue() {
        System.out.println(Konsole.fett("1)") + " Hinzufügen   " + Konsole.fett("2)") + " Umbenennen   "
                + Konsole.fett("3)") + " Löschen   " + Konsole.fett("0)") + " Zurück");
    }

    /** Erklärt das ganze Programm: Menüpunkte, Regeln und wo die Daten liegen. */
    private void hilfeAnzeigen() {
        titel("Hilfe");
        System.out.println("""
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
                  9) Bericht ansehen und speichern
                     Filter wählen: Zeitraum (alles, Jahr, Monat oder von - bis),
                     Projekt und Mitarbeiter (0 = alle). Der Bericht erscheint zuerst
                     hier im Terminal. Gefällt er dir, speichere ihn als Word (.docx)
                     oder CSV (für Excel), im selben Aufbau. Der Dateiname kommt aus
                     dem Filter, z. B. oeblarn_2026_tanja.docx.
                  6) Mitarbeiter verwalten / 7) Projekte verwalten
                     Hinzufügen, umbenennen, löschen. Ein neuer Name gilt auch
                     für alle alten Einträge. Jeder Mitarbeiter bekommt eine
                     feste Kennung: AB_01 = Arbeiter 1, AB_02 = Arbeiter 2 usw.
                  0) Speichern und beenden

                REGELN
                  - Doppelte Namen sind nicht erlaubt (Groß-/Kleinschreibung egal).
                  - Wer noch Einträge hat, kann nicht gelöscht werden.
                    Lösche zuerst die Einträge.
                  - In Listen bedeutet 0 abbrechen bzw. behalten
                    (beim Bericht: alle Projekte bzw. alle Mitarbeiter).
                  - Jede Änderung wird sofort gespeichert.
                """);
        System.out.println("Deine Daten liegen in: " + speicher.getOrdner());
        System.out.println("(mitarbeiter.csv, projekte.csv, eintraege.csv)");
        System.out.println("Gespeicherte Berichte liegen in: " + speicher.getBerichtOrdner());
    }

    // ---------- Ausgabe ----------

    /** Neuer Bildschirm: Fenster leeren, dann die Überschrift des Menüpunkts. */
    private static void titel(String text) {
        Konsole.leeren();
        System.out.println();
        System.out.println(Konsole.ueberschrift(text));
    }

    /** Zeigt die letzte Meldung (nach dem Leeren des Fensters wäre sie sonst weg). */
    private void zeigeMeldung() {
        if (meldung != null) {
            System.out.println();
            System.out.println(meldung);
        }
    }

    /** Hält eine reine Anzeige (z. B. die Auswertung) offen, bis Enter gedrückt wird. Nicht bei Testläufen. */
    private void weiterMitEnter() {
        if (Konsole.INTERAKTIV) {
            System.out.println();
            leseZeile(Konsole.grau("Weiter mit Enter ..."));
        }
    }

    /** Tabelle eingerückt ausgeben. */
    private static void zeige(Tabelle t) {
        Konsole.tabelle(t).forEach(z -> System.out.println("  " + z));
    }

    private void ok(String text) {
        melde(Konsole.gruen(text));
    }

    private void fehler(String text) {
        melde(Konsole.rot(text));
    }

    private void hinweis(String text) {
        melde(Konsole.grau(text));
    }

    /** Sofort ausgeben und merken, damit der nächste Bildschirm sie noch zeigt (nur im echten Fenster). */
    private void melde(String text) {
        System.out.println(text);
        if (Konsole.INTERAKTIV) {
            meldung = text;
        }
    }

    // ---------- Speichern ----------

    /** Speichert sofort. Bei einem Fehler nur warnen, nicht abstürzen. */
    private void speichern() {
        try {
            speicher.speichern(zeiterfassung);
        } catch (IOException ex) {
            fehler("WARNUNG: Speichern fehlgeschlagen: " + ex.getMessage());
        }
    }

    // ---------- Eingabe-Hilfsmethoden ----------

    /** Liest eine Zeile (ohne Leerzeichen am Rand). Wirft EingabeEnde, wenn nichts mehr kommt. */
    private String leseZeile(String text) {
        meldung = null; // was davor gemeldet wurde, hat der Benutzer gesehen
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
                fehler("Bitte eine ganze Zahl eingeben.");
            }
        }
    }

    /** Fragt eine Zahl von min bis max ab. */
    private int leseZahl(String text, int min, int max) {
        while (true) {
            int nr = leseZahl(text);
            if (nr >= min && nr <= max) {
                return nr;
            }
            fehler("Bitte eine Zahl von " + min + " bis " + max + " eingeben.");
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
            fehler("Die Eingabe darf nicht leer sein.");
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
                fehler("Die Stunden müssen größer als 0 und höchstens 24 sein.");
            } catch (NumberFormatException ex) {
                fehler("Bitte eine Zahl eingeben, z. B. 4,5");
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
                fehler("Ungültiges Datum. Bitte im Format TT.MM.JJJJ eingeben, z. B. 05.10.2026");
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
            fehler("Bitte j oder n eingeben.");
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
            hinweis("Keine " + was + " vorhanden.");
            return null;
        }
        System.out.println(was + " wählen:");
        for (int i = 0; i < liste.size(); i++) {
            System.out.println("  " + Konsole.fett((i + 1) + ")") + " " + anzeige.apply(liste.get(i)));
        }
        System.out.println("  " + Konsole.fett("0)") + " " + textFuerNull);
        int nr = leseZahl("Nummer: ", 0, liste.size());
        return nr == 0 ? null : liste.get(nr - 1);
    }
}
