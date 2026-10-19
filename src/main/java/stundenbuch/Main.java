package stundenbuch;

import java.time.LocalDate;

/**
 * Startpunkt der Anwendung.
 * Später startet hier nur noch das Menü.
 */
public class Main {

    public static void main(String[] args) {
        // TESTCODE Schritt 2: wird in Schritt 5 entfernt
        Mitarbeiter jonas = new Mitarbeiter(1, "Jonas");
        Mitarbeiter lena = new Mitarbeiter(2, "Lena");
        Projekt graz = new Projekt(1, "Graz", "Mustermann");

        Eintrag e1 = new Eintrag(1, LocalDate.of(2026, 10, 5), jonas, graz, 10.0, "Konzept erstellt");
        Eintrag e2 = new Eintrag(2, LocalDate.of(2026, 10, 6), lena, graz, 4.5, "Datenbank aufgesetzt");

        System.out.println("Mitarbeiter: " + jonas + ", " + lena);
        System.out.println("Projekt:     " + graz);
        zeige(e1);
        zeige(e2);

        // Umbenennen: der Eintrag zeigt auf dasselbe Objekt, also ändert sich der Name dort mit
        jonas.setName("Jonas M.");
        System.out.println("Nach Umbenennen von Jonas:");
        zeige(e1);
    }

    private static void zeige(Eintrag e) {
        System.out.println("  #" + e.getId() + " " + e.getDatum() + " " + e.getMitarbeiter()
                + " " + e.getProjekt() + " " + e.getStunden() + " h: " + e.getBeschreibung());
    }
}
