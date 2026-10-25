package stundenbuch.modell;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Hält alle Mitarbeiter, Projekte und Einträge, rechnet und prüft die Regeln.
 * Benutzt kein System.out: Ausgaben macht nur das Menü.
 */
public class Zeiterfassung {

    private final List<Mitarbeiter> mitarbeiter = new ArrayList<>();
    private final List<Projekt> projekte = new ArrayList<>();
    private final List<Eintrag> eintraege = new ArrayList<>();

    // Nächste freie IDs. Sie wachsen nur, damit gelöschte IDs nie wieder vergeben werden.
    // Ausnahme: Ist eine Liste ganz leer, beginnt ihre Nummer wieder bei 1 (AB_01, Projekt 1, #1).
    private int naechsteMitarbeiterId = 1;
    private int naechsteProjektId = 1;
    private int naechsteEintragId = 1;

    // ---------- Hinzufügen ----------

    /**
     * Legt einen Mitarbeiter an. Gibt null zurück, wenn der Name schon vergeben ist.
     * Ist die Liste leer, bekommt er wieder AB_01. Das ist sicher, weil ein Mitarbeiter
     * mit Einträgen nicht gelöscht werden kann: Kein Eintrag verweist mehr auf eine alte Kennung.
     */
    public Mitarbeiter addMitarbeiter(String name) {
        if (istMitarbeiterNameVergeben(name)) {
            return null;
        }
        if (mitarbeiter.isEmpty()) {
            naechsteMitarbeiterId = 1;
        }
        Mitarbeiter m = new Mitarbeiter(naechsteMitarbeiterId++, name);
        mitarbeiter.add(m);
        return m;
    }

    /**
     * Legt ein Projekt an. Gibt null zurück, wenn Ort/Kunde schon vergeben ist.
     * Ist die Liste leer, beginnt die Nummer wieder bei 1 (sicher, siehe addMitarbeiter).
     */
    public Projekt addProjekt(String ort, String kunde) {
        if (istProjektNameVergeben(ort, kunde)) {
            return null;
        }
        if (projekte.isEmpty()) {
            naechsteProjektId = 1;
        }
        Projekt p = new Projekt(naechsteProjektId++, ort, kunde);
        projekte.add(p);
        return p;
    }

    /** Legt einen Eintrag an. Gibt es keine Einträge mehr, beginnt die #-Nummer wieder bei 1. */
    public Eintrag addEintrag(LocalDate datum, Mitarbeiter m, Projekt p, double stunden, String text,
                              LocalTime von, LocalTime bis) {
        if (eintraege.isEmpty()) {
            naechsteEintragId = 1;
        }
        Eintrag e = new Eintrag(naechsteEintragId++, datum, m, p, stunden, text, von, bis);
        eintraege.add(e);
        return e;
    }

    // ---------- Laden (nur für den Speicher: übernimmt die gespeicherte ID, nicht für das Menü) ----------

    public void ladeMitarbeiter(int id, String name) {
        mitarbeiter.add(new Mitarbeiter(id, name));
        naechsteMitarbeiterId = Math.max(naechsteMitarbeiterId, id + 1);
    }

    public void ladeProjekt(int id, String ort, String kunde) {
        projekte.add(new Projekt(id, ort, kunde));
        naechsteProjektId = Math.max(naechsteProjektId, id + 1);
    }

    public void ladeEintrag(int id, LocalDate datum, Mitarbeiter m, Projekt p, double stunden, String text,
                            LocalTime von, LocalTime bis) {
        eintraege.add(new Eintrag(id, datum, m, p, stunden, text, von, bis));
        naechsteEintragId = Math.max(naechsteEintragId, id + 1);
    }

    // ---------- Löschen ----------

    /** Löscht den Mitarbeiter. Gibt false zurück, wenn er noch Einträge hat (Löschschutz). */
    public boolean loescheMitarbeiter(Mitarbeiter m) {
        boolean hatEintraege = eintraege.stream().anyMatch(e -> e.getMitarbeiter() == m);
        if (hatEintraege) {
            return false;
        }
        return mitarbeiter.remove(m);
    }

    /** Löscht das Projekt. Gibt false zurück, wenn es noch Einträge hat (Löschschutz). */
    public boolean loescheProjekt(Projekt p) {
        boolean hatEintraege = eintraege.stream().anyMatch(e -> e.getProjekt() == p);
        if (hatEintraege) {
            return false;
        }
        return projekte.remove(p);
    }

    public boolean loescheEintrag(Eintrag e) {
        return eintraege.remove(e);
    }

    // ---------- Rechnen ----------

    /** Stunden eines Mitarbeiters an einem Projekt. monat == null bedeutet "alle Monate". */
    public double getStunden(Projekt p, Mitarbeiter m, YearMonth monat) {
        return eintraege.stream()
                .filter(e -> e.getProjekt() == p && e.getMitarbeiter() == m)
                .filter(e -> monat == null || YearMonth.from(e.getDatum()).equals(monat))
                .mapToDouble(Eintrag::getStunden)
                .sum();
    }

    /** Alle Stunden eines Projekts. */
    public double getStundenGesamt(Projekt p, YearMonth monat) {
        return eintraege.stream()
                .filter(e -> e.getProjekt() == p)
                .filter(e -> monat == null || YearMonth.from(e.getDatum()).equals(monat))
                .mapToDouble(Eintrag::getStunden)
                .sum();
    }

    /** Alle Stunden eines Mitarbeiters. */
    public double getStundenGesamt(Mitarbeiter m, YearMonth monat) {
        return eintraege.stream()
                .filter(e -> e.getMitarbeiter() == m)
                .filter(e -> monat == null || YearMonth.from(e.getDatum()).equals(monat))
                .mapToDouble(Eintrag::getStunden)
                .sum();
    }

    /** Alle Stunden überhaupt. */
    public double getStundenGesamt(YearMonth monat) {
        return eintraege.stream()
                .filter(e -> monat == null || YearMonth.from(e.getDatum()).equals(monat))
                .mapToDouble(Eintrag::getStunden)
                .sum();
    }

    /** Alle Stunden eines Tages (für die Tagessumme bei "Arbeitstage anzeigen"). */
    public double getStundenAmTag(LocalDate datum) {
        return eintraege.stream()
                .filter(e -> e.getDatum().equals(datum))
                .mapToDouble(Eintrag::getStunden)
                .sum();
    }

    /** Alle Monate, in denen es Einträge gibt, neuester zuerst. */
    public List<YearMonth> getMonateMitEintraegen() {
        return eintraege.stream()
                .map(e -> YearMonth.from(e.getDatum()))
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    /** Alle Jahre, in denen es Einträge gibt, neuestes zuerst. */
    public List<Integer> getJahreMitEintraegen() {
        return eintraege.stream()
                .map(e -> e.getDatum().getYear())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    /** Einträge sortiert: neuester Tag zuerst, innerhalb eines Tages nach ID. */
    public List<Eintrag> getEintraegeNachDatum() {
        return eintraege.stream()
                .sorted(Comparator.comparing(Eintrag::getDatum).reversed()
                        .thenComparing(Eintrag::getId))
                .toList();
    }

    // ---------- Suchen ----------

    public Mitarbeiter findeMitarbeiter(int id) {
        for (Mitarbeiter m : mitarbeiter) {
            if (m.getId() == id) {
                return m;
            }
        }
        return null;
    }

    public Projekt findeProjekt(int id) {
        for (Projekt p : projekte) {
            if (p.getId() == id) {
                return p;
            }
        }
        return null;
    }

    public Eintrag findeEintrag(int id) {
        for (Eintrag e : eintraege) {
            if (e.getId() == id) {
                return e;
            }
        }
        return null;
    }

    // ---------- Prüfen (Groß-/Kleinschreibung egal) ----------

    public boolean istMitarbeiterNameVergeben(String name) {
        return mitarbeiter.stream().anyMatch(m -> m.getName().equalsIgnoreCase(name));
    }

    public boolean istProjektNameVergeben(String ort, String kunde) {
        String name = ort + "/" + kunde;
        return projekte.stream().anyMatch(p -> p.getName().equalsIgnoreCase(name));
    }

    // ---------- Getter (nur lesen, Änderungen nur über die Methoden oben) ----------

    public List<Mitarbeiter> getMitarbeiter() {
        return Collections.unmodifiableList(mitarbeiter);
    }

    public List<Projekt> getProjekte() {
        return Collections.unmodifiableList(projekte);
    }

    public List<Eintrag> getEintraege() {
        return Collections.unmodifiableList(eintraege);
    }
}
