package stundenbuch.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import stundenbuch.modell.Zeiterfassung;
import stundenbuch.speicher.Speicher;

/**
 * Hält die geladenen Daten für alle Seiten. Nach jeder Änderung wird sofort gespeichert
 * und allen Seiten Bescheid gesagt, damit sie sich neu zeichnen.
 */
final class Daten {

    private final Speicher speicher;
    private final Zeiterfassung zeiterfassung;
    private final List<Runnable> beobachter = new ArrayList<>();
    private Consumer<String> meldungen = text -> { };

    Daten(Speicher speicher) {
        this.speicher = speicher;
        this.zeiterfassung = speicher.laden();
    }

    Zeiterfassung z() {
        return zeiterfassung;
    }

    Speicher speicher() {
        return speicher;
    }

    void beobachte(Runnable r) {
        beobachter.add(r);
    }

    void setMeldungen(Consumer<String> meldungen) {
        this.meldungen = meldungen;
    }

    /** Kurze Meldung in der Statuszeile unten. */
    void meldung(String text) {
        meldungen.accept(text);
    }

    /** Speichert und zeichnet alle Seiten neu. Gibt false zurück, wenn das Speichern nicht klappte. */
    boolean geaendert() {
        boolean ok = true;
        try {
            speicher.speichern(zeiterfassung);
        } catch (IOException ex) {
            ok = false;
            Alert a = new Alert(Alert.AlertType.ERROR,
                    "Speichern fehlgeschlagen: " + ex.getMessage()
                            + "\n\nIst eine Datei gerade in Excel offen? Dann dort schließen und die Änderung wiederholen.",
                    ButtonType.OK);
            a.setHeaderText("Nicht gespeichert");
            a.showAndWait();
        }
        beobachter.forEach(Runnable::run);
        return ok;
    }

    boolean hatPausen() {
        return zeiterfassung.getEintraege().stream().anyMatch(e -> e.getPauseMinuten() > 0);
    }
}
