package stundenbuch.gui;

import javafx.scene.Node;

/** Eine Seite der Oberfläche (Einträge, Auswertung, ...). */
interface Seite {

    String titel();

    Node inhalt();

    /** Daten haben sich geändert: neu zeichnen. */
    void aktualisieren();
}
