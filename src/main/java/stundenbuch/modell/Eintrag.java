package stundenbuch.modell;

import java.time.LocalDate;

/**
 * Ein Eintrag: Wer hat wann an welchem Projekt wie viele Stunden was gemacht.
 * Hält Objekte statt Namen, damit Umbenennen automatisch überall wirkt.
 */
public class Eintrag {

    private final int id;
    private LocalDate datum;
    private Mitarbeiter mitarbeiter;
    private Projekt projekt;
    private double stunden;
    private String beschreibung;

    public Eintrag(int id, LocalDate datum, Mitarbeiter mitarbeiter, Projekt projekt,
                   double stunden, String beschreibung) {
        this.id = id;
        this.datum = datum;
        this.mitarbeiter = mitarbeiter;
        this.projekt = projekt;
        this.stunden = stunden;
        this.beschreibung = beschreibung;
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
