package stundenbuch.gui;

import javafx.application.Application;

/**
 * Startpunkt der Oberfläche. Diese Klasse erbt bewusst nicht von Application:
 * Nur so startet die Jar auch ohne JavaFX-Module (alles liegt in einer Jar).
 */
public class Start {

    public static void main(String[] args) {
        Application.launch(StundenbuchApp.class, args);
    }
}
