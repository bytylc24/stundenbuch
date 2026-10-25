package stundenbuch.modell;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Ein Eintrag: Wer hat wann an welchem Projekt wie viele Stunden was gemacht.
 * Hält Objekte statt Namen, damit Umbenennen automatisch überall wirkt.
 * Optional steht dabei die Uhrzeit von-bis; dann sind die Stunden daraus berechnet.
 */
public class Eintrag {

    private static final DateTimeFormatter UHRZEIT = DateTimeFormatter.ofPattern("H:mm");

    private final int id;
    private LocalDate datum;
    private Mitarbeiter mitarbeiter;
    private Projekt projekt;
    private double stunden;
    private String beschreibung;
    private LocalTime von; // null = keine Uhrzeit angegeben, nur Stunden
    private LocalTime bis;

    public Eintrag(int id, LocalDate datum, Mitarbeiter mitarbeiter, Projekt projekt,
                   double stunden, String beschreibung, LocalTime von, LocalTime bis) {
        this.id = id;
        this.datum = datum;
        this.mitarbeiter = mitarbeiter;
        this.projekt = projekt;
        this.stunden = stunden;
        this.beschreibung = beschreibung;
        this.von = von;
        this.bis = bis;
    }

    /** Stunden zwischen von und bis, z. B. 9:00-18:30 ergibt 9,5. */
    public static double stundenZwischen(LocalTime von, LocalTime bis) {
        return Duration.between(von, bis).toMinutes() / 60.0;
    }

    /** Die Uhrzeit als Text, z. B. "9:00-18:30". Leer, wenn keine angegeben ist. */
    public String getZeitText() {
        return von == null ? "" : von.format(UHRZEIT) + "-" + bis.format(UHRZEIT);
    }

    public LocalTime getVon() {
        return von;
    }

    public LocalTime getBis() {
        return bis;
    }

    /** Setzt die Uhrzeit (beide null = entfernen). Die Stunden setzt der Aufrufer dazu. */
    public void setZeit(LocalTime von, LocalTime bis) {
        this.von = von;
        this.bis = bis;
    }

    public int getId() {
        return id;
    }

    public LocalDate getDatum() {
        return datum;
    }

    public void setDatum(LocalDate datum) {
        this.datum = datum;
    }

    public Mitarbeiter getMitarbeiter() {
        return mitarbeiter;
    }

    public void setMitarbeiter(Mitarbeiter mitarbeiter) {
        this.mitarbeiter = mitarbeiter;
    }

    public Projekt getProjekt() {
        return projekt;
    }

    public void setProjekt(Projekt projekt) {
        this.projekt = projekt;
    }

    public double getStunden() {
        return stunden;
    }

    public void setStunden(double stunden) {
        this.stunden = stunden;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public void setBeschreibung(String beschreibung) {
        this.beschreibung = beschreibung;
    }
}
