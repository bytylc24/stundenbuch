package stundenbuch;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Startpunkt der Anwendung.
 * Später startet hier nur noch das Menü.
 */
public class Main {

    public static void main(String[] args) {
        // TESTCODE Schritt 3: wird in Schritt 5 entfernt
        Zeiterfassung z = new Zeiterfassung();
        Mitarbeiter jonas = z.addMitarbeiter("Jonas");
        Mitarbeiter lena = z.addMitarbeiter("Lena");
        Mitarbeiter max = z.addMitarbeiter("Max");
        Projekt graz = z.addProjekt("Graz", "Mustermann");
        Projekt wien = z.addProjekt("Wien", "Huber");

        z.addEintrag(LocalDate.of(2026, 10, 5), jonas, graz, 10.0, "Konzept erstellt");
        z.addEintrag(LocalDate.of(2026, 10, 5), jonas, wien, 5.0, "Datenbank aufgesetzt");
        z.addEintrag(LocalDate.of(2026, 10, 6), lena, graz, 20.0, "Programmiert");
        z.addEintrag(LocalDate.of(2026, 9, 28), lena, wien, 3.0, "Besprechung");

        YearMonth oktober = YearMonth.of(2026, 10);

        System.out.println("--- Stunden gesamt (alle Monate) ---");
        System.out.println("Jonas @ Graz: " + z.getStunden(graz, jonas, null) + " (erwartet 10.0)");
        System.out.println("Jonas @ Wien: " + z.getStunden(wien, jonas, null) + " (erwartet 5.0)");
        System.out.println("Graz gesamt:  " + z.getStundenGesamt(graz, null) + " (erwartet 30.0)");
        System.out.println("Wien gesamt:  " + z.getStundenGesamt(wien, null) + " (erwartet 8.0)");
        System.out.println("Lena gesamt:  " + z.getStundenGesamt(lena, null) + " (erwartet 23.0)");
        System.out.println("Max gesamt:   " + z.getStundenGesamt(max, null) + " (erwartet 0.0)");
        System.out.println("Alles:        " + z.getStundenGesamt(null) + " (erwartet 38.0)");

        System.out.println("--- Monatsfilter Oktober 2026 ---");
        System.out.println("Wien Oktober: " + z.getStundenGesamt(wien, oktober) + " (erwartet 5.0)");
        System.out.println("Lena Oktober: " + z.getStundenGesamt(lena, oktober) + " (erwartet 20.0)");
        System.out.println("Monate:       " + z.getMonateMitEintraegen() + " (erwartet [2026-10, 2026-09])");

        System.out.println("--- Sortierung nach Datum ---");
        for (Eintrag e : z.getEintraegeNachDatum()) {
            System.out.println("  #" + e.getId() + " " + e.getDatum() + " " + e.getMitarbeiter() + " " + e.getProjekt());
        }

        System.out.println("--- Regeln ---");
        System.out.println("Doppelt 'lena' anlegen:    " + z.addMitarbeiter("lena") + " (erwartet null)");
        System.out.println("Doppelt 'graz/MUSTERMANN': " + z.addProjekt("graz", "MUSTERMANN") + " (erwartet null)");
        System.out.println("Lena löschen (hat Einträge): " + z.loescheMitarbeiter(lena) + " (erwartet false)");
        System.out.println("Wien löschen (hat Einträge): " + z.loescheProjekt(wien) + " (erwartet false)");
        System.out.println("Max löschen (keine Einträge): " + z.loescheMitarbeiter(max) + " (erwartet true)");
        Mitarbeiter neu = z.addMitarbeiter("Sara");
        System.out.println("Neue ID nach Löschen von Max (#3): " + neu.getId() + " (erwartet 4)");
        System.out.println("findeMitarbeiter(2): " + z.findeMitarbeiter(2) + " (erwartet Lena)");
        System.out.println("findeMitarbeiter(3): " + z.findeMitarbeiter(3) + " (erwartet null)");
    }
}
