package stundenbuch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;

/**
 * Startpunkt der Anwendung.
 * Später startet hier nur noch das Menü.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        // TESTCODE Schritt 4: wird in Schritt 5 entfernt
        Speicher speicher = new Speicher();
        System.out.println("Ordner: " + speicher.getOrdner());

        Zeiterfassung leer = speicher.laden();
        System.out.println("Erster Start, Mitarbeiter: " + leer.getMitarbeiter().size() + " (erwartet 0)");

        Zeiterfassung z = new Zeiterfassung();
        Mitarbeiter jonas = z.addMitarbeiter("Jonas");
        Mitarbeiter lena = z.addMitarbeiter("Lena");
        Projekt graz = z.addProjekt("Graz", "Mustermann");
        Projekt wien = z.addProjekt("Wien", "Huber; Söhne");
        z.addEintrag(LocalDate.of(2026, 10, 5), jonas, graz, 10.0, "Konzept erstellt");
        z.addEintrag(LocalDate.of(2026, 10, 6), lena, wien, 4.5, "Tabellen; Indizes; Pfad C:\\daten\\s1");
        speicher.speichern(z);

        // Eine kaputte Zeile anhängen: muss beim Laden übersprungen werden
        Files.writeString(speicher.getOrdner().resolve("eintraege.csv"), "kaputt;abc\n",
                StandardCharsets.UTF_8, StandardOpenOption.APPEND);

        Zeiterfassung geladen = speicher.laden();
        System.out.println("--- Nach dem Laden ---");
        System.out.println("Mitarbeiter: " + geladen.getMitarbeiter() + " (erwartet [Jonas, Lena])");
        System.out.println("Projekte:    " + geladen.getProjekte() + " (erwartet [Graz/Mustermann, Wien/Huber; Söhne])");
        System.out.println("Einträge:    " + geladen.getEintraege().size() + " (erwartet 2, kaputte Zeile übersprungen)");
        for (Eintrag e : geladen.getEintraege()) {
            System.out.println("  #" + e.getId() + " " + e.getDatum() + " " + e.getMitarbeiter() + " "
                    + e.getProjekt() + " " + e.getStunden() + " h: " + e.getBeschreibung());
        }
        Eintrag e2 = geladen.findeEintrag(2);
        boolean gleich = e2.getBeschreibung().equals("Tabellen; Indizes; Pfad C:\\daten\\s1");
        System.out.println("Text mit ; und \\ gleich wie vorher: " + gleich + " (erwartet true)");
        boolean selbesObjekt = e2.getMitarbeiter() == geladen.findeMitarbeiter(2);
        System.out.println("Eintrag zeigt auf dasselbe Lena-Objekt: " + selbesObjekt + " (erwartet true)");
        System.out.println("Stunden Wien: " + geladen.getStundenGesamt(geladen.findeProjekt(2), null) + " (erwartet 4.5)");
        System.out.println("Neue ID nach Laden: " + geladen.addMitarbeiter("Max").getId() + " (erwartet 3)");
    }
}
