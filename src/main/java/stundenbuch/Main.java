package stundenbuch;

import stundenbuch.anzeige.Menue;
import stundenbuch.speicher.Speicher;

/**
 * Startpunkt der Anwendung: startet nur das Menü.
 */
public class Main {

    public static void main(String[] args) {
        new Menue(new Speicher()).start();
    }
}
