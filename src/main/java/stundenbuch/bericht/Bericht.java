package stundenbuch.bericht;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import stundenbuch.anzeige.Format;
import stundenbuch.modell.Eintrag;
import stundenbuch.modell.Mitarbeiter;
import stundenbuch.modell.Projekt;
import stundenbuch.modell.Zeiterfassung;

/**
 * Ein Stundenbericht, gefiltert nach Zeitraum, Projekt und Mitarbeiter.
 * Rechnet nur und baut Tabellen. Wie er aussieht, entscheiden Terminal und Export.
 */
public class Bericht {

    /**
     * Zeitraum von/bis (beide inklusive, null = offen).
     * text steht im Bericht, dateiTeil im Dateinamen (null = kommt nicht in den Namen).
     */
    public record Zeitraum(LocalDate von, LocalDate bis, String text, String dateiTeil) {

        public static Zeitraum gesamt() {
            return new Zeitraum(null, null, "Gesamter Zeitraum", null);
        }

        public static Zeitraum jahr(int jahr) {
            return new Zeitraum(LocalDate.of(jahr, 1, 1), LocalDate.of(jahr, 12, 31),
                    "Jahr " + jahr, String.valueOf(jahr));
        }

        public static Zeitraum monat(YearMonth m) {
            // Im Dateinamen als Zahl, damit die Dateien im Explorer richtig sortiert sind
            return new Zeitraum(m.atDay(1), m.atEndOfMonth(), m.format(Format.MONAT),
                    String.format("%d_%02d", m.getYear(), m.getMonthValue()));
        }

        public static Zeitraum vonBis(LocalDate von, LocalDate bis) {
            return new Zeitraum(von, bis, von.format(Format.DATUM) + " bis " + bis.format(Format.DATUM),
                    von + "_bis_" + bis);
        }

        public boolean enthaelt(LocalDate datum) {
            return (von == null || !datum.isBefore(von)) && (bis == null || !datum.isAfter(bis));
        }
    }

    private final Zeitraum zeitraum;
    private final Projekt projekt;          // null = alle Projekte
    private final Mitarbeiter mitarbeiter;  // null = alle Mitarbeiter
    private final LocalDate erstellt = LocalDate.now();
    private final List<Eintrag> eintraege;
    private final List<Projekt> alleProjekte;
    private final List<Mitarbeiter> alleMitarbeiter;

    public Bericht(Zeiterfassung z, Zeitraum zeitraum, Projekt projekt, Mitarbeiter mitarbeiter) {
        this.zeitraum = zeitraum;
        this.projekt = projekt;
        this.mitarbeiter = mitarbeiter;
        this.alleProjekte = z.getProjekte();
        this.alleMitarbeiter = z.getMitarbeiter();
        // Im Bericht chronologisch: ältester Tag zuerst, wie man ihn liest
        this.eintraege = z.getEintraege().stream()
                .filter(e -> zeitraum.enthaelt(e.getDatum()))
                .filter(e -> projekt == null || e.getProjekt() == projekt)
                .filter(e -> mitarbeiter == null || e.getMitarbeiter() == mitarbeiter)
                .sorted(Comparator.comparing(Eintrag::getDatum).thenComparing(Eintrag::getId))
                .toList();
    }

    public boolean istLeer() {
        return eintraege.isEmpty();
    }

    public String getTitel() {
        return "Stundenbericht";
    }

    /** Was im Kopf des Berichts steht: Paare aus Bezeichnung und Wert. */
    public List<String[]> getAngaben() {
        return List.of(
                new String[]{"Projekt", projekt == null ? "Alle Projekte" : projekt.getName()},
                new String[]{"Mitarbeiter", mitarbeiter == null ? "Alle Mitarbeiter"
                        : mitarbeiter.getName() + " (" + mitarbeiter.getKennung() + ")"},
                new String[]{"Zeitraum", zeitraum.text()},
                new String[]{"Erstellt am", erstellt.format(Format.DATUM)});
    }

    /** Die großen Zahlen oben im Bericht: Paare aus Wert und Bezeichnung. */
    public List<String[]> getKennzahlen() {
        long tage = eintraege.stream().map(Eintrag::getDatum).distinct().count();
        return List.of(
                new String[]{Format.stunden(summe(eintraege)), "Stunden gesamt"},
                new String[]{String.valueOf(eintraege.size()), eintraege.size() == 1 ? "Eintrag" : "Einträge"},
                new String[]{String.valueOf(tage), tage == 1 ? "Arbeitstag" : "Arbeitstage"});
    }

    /** Einträge, dazu Summen pro Mitarbeiter und pro Projekt, wenn es mehr als einen gibt. */
    public List<Tabelle> getTabellen() {
        List<Tabelle> tabellen = new ArrayList<>();
        tabellen.add(eintragsTabelle());

        if (mitarbeiter == null) {
            List<Mitarbeiter> beteiligt = alleMitarbeiter.stream()
                    .filter(m -> eintraege.stream().anyMatch(e -> e.getMitarbeiter() == m)).toList();
            if (beteiligt.size() > 1) {
                Tabelle t = new Tabelle("Stunden pro Mitarbeiter").spalte("Mitarbeiter").spalte("Kennung");
                anteilSpalten(t);
                for (Mitarbeiter m : beteiligt) {
                    anteilZeile(t, eintraege.stream().filter(e -> e.getMitarbeiter() == m).toList(),
                            m.getName(), m.getKennung());
                }
                summenZeile(t, 2);
                tabellen.add(t);
            }
        }

        if (projekt == null) {
            List<Projekt> beteiligt = alleProjekte.stream()
                    .filter(p -> eintraege.stream().anyMatch(e -> e.getProjekt() == p)).toList();
            if (beteiligt.size() > 1) {
                Tabelle t = new Tabelle("Stunden pro Projekt").spalte("Projekt");
                anteilSpalten(t);
                for (Projekt p : beteiligt) {
                    anteilZeile(t, eintraege.stream().filter(e -> e.getProjekt() == p).toList(), p.getName());
                }
                summenZeile(t, 1);
                tabellen.add(t);
            }
        }
        return tabellen;
    }

    /**
     * Alle Einträge. Spalten, die wegen des Filters überall gleich wären, fallen weg.
     * Über mehrere Monate gibt es pro Monat eine Überschrift und eine Zwischensumme.
     */
    private Tabelle eintragsTabelle() {
        Tabelle t = new Tabelle("Einträge").spalte("Datum");
        if (mitarbeiter == null) {
            t.spalte("Mitarbeiter");
        }
        if (projekt == null) {
            t.spalte("Projekt");
        }
        // Die Zeit-Spalte gibt es nur, wenn mindestens ein Eintrag eine Uhrzeit hat
        boolean mitZeit = eintraege.stream().anyMatch(e -> !e.getZeitText().isEmpty());
        if (mitZeit) {
            t.spalte("Zeit");
        }
        t.spalte("Beschreibung", false, 36).zahlenSpalte("Stunden");
        int leer = t.getSpalten().size() - 2; // Zellen zwischen Beschriftung und Stunden bei Summen

        Map<YearMonth, List<Eintrag>> proMonat = new TreeMap<>();
        for (Eintrag e : eintraege) {
            proMonat.computeIfAbsent(YearMonth.from(e.getDatum()), k -> new ArrayList<>()).add(e);
        }
        boolean mehrereMonate = proMonat.size() > 1;

        for (Map.Entry<YearMonth, List<Eintrag>> monat : proMonat.entrySet()) {
            String name = monat.getKey().format(Format.MONAT);
            if (mehrereMonate) {
                t.gruppe(name);
            }
            for (Eintrag e : monat.getValue()) {
                List<String> zellen = new ArrayList<>();
                zellen.add(e.getDatum().format(Format.TAG));
                if (mitarbeiter == null) {
                    zellen.add(e.getMitarbeiter().getName());
                }
                if (projekt == null) {
                    zellen.add(e.getProjekt().getName());
                }
                if (mitZeit) {
                    zellen.add(e.getZeitText());
                }
                zellen.add(e.getBeschreibung());
                zellen.add(Format.zahl(e.getStunden()));
                t.zeile(zellen.toArray(String[]::new));
            }
            if (mehrereMonate) {
                t.zwischensumme(summenZellen("Summe " + name, leer, summe(monat.getValue())));
            }
        }
        t.summe(summenZellen("Gesamt", leer, summe(eintraege)));
        return t;
    }

    private static String[] summenZellen(String text, int leer, double stunden) {
        String[] zellen = new String[leer + 2];
        Arrays.fill(zellen, "");
        zellen[0] = text;
        zellen[zellen.length - 1] = Format.zahl(stunden);
        return zellen;
    }

    private static void anteilSpalten(Tabelle t) {
        t.zahlenSpalte("Einträge").zahlenSpalte("Stunden").zahlenSpalte("Anteil");
    }

    private void anteilZeile(Tabelle t, List<Eintrag> teil, String... namen) {
        List<String> zellen = new ArrayList<>(List.of(namen));
        zellen.add(String.valueOf(teil.size()));
        zellen.add(Format.zahl(summe(teil)));
        zellen.add(String.format(Format.DEUTSCH, "%.0f %%", 100 * summe(teil) / summe(eintraege)));
        t.zeile(zellen.toArray(String[]::new));
    }

    private void summenZeile(Tabelle t, int namensSpalten) {
        List<String> zellen = new ArrayList<>();
        zellen.add("Gesamt");
        for (int i = 1; i < namensSpalten; i++) {
            zellen.add("");
        }
        zellen.add(String.valueOf(eintraege.size()));
        zellen.add(Format.zahl(summe(eintraege)));
        zellen.add("100 %");
        t.summe(zellen.toArray(String[]::new));
    }

    private static double summe(List<Eintrag> liste) {
        return liste.stream().mapToDouble(Eintrag::getStunden).sum();
    }

    // ---------- Dateiname ----------

    /**
     * Dateiname ohne Endung aus dem Filter, z. B. "oeblarn_2026_tanja".
     * Projekt über den Ort; gibt es den Ort mehrmals, kommt der Kunde dazu.
     */
    public String getDateiname() {
        List<String> teile = new ArrayList<>();
        if (projekt != null) {
            long gleicherOrt = alleProjekte.stream().filter(p -> p.getOrt().equalsIgnoreCase(projekt.getOrt())).count();
            teile.add(gleicherOrt > 1 ? kurzname(projekt.getOrt() + " " + projekt.getKunde()) : kurzname(projekt.getOrt()));
        }
        if (zeitraum.dateiTeil() != null) {
            teile.add(zeitraum.dateiTeil());
        }
        if (mitarbeiter != null) {
            teile.add(kurzname(mitarbeiter.getName()));
        }
        if (projekt == null && mitarbeiter == null) {
            teile.add(0, "stundenbericht");
            if (zeitraum.dateiTeil() == null) {
                teile.add("gesamt");
            }
        }
        return String.join("_", teile);
    }

    /** Macht aus "Öblarn" ein "oeblarn": klein, ohne Umlaute und Sonderzeichen, Leerzeichen als _. */
    static String kurzname(String text) {
        String s = text.toLowerCase(Format.DEUTSCH)
                .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
        // Restliche Akzente entfernen (é -> e), dann alles außer a-z und 0-9 zu _
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return s.isEmpty() ? "bericht" : s;
    }
}
