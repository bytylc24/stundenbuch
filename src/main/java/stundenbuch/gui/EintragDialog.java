package stundenbuch.gui;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;
import stundenbuch.anzeige.Format;
import stundenbuch.modell.Eintrag;
import stundenbuch.modell.Mitarbeiter;
import stundenbuch.modell.Projekt;

/**
 * Fenster zum Erfassen und Bearbeiten eines Eintrags. Prüft bei jeder Eingabe und lässt
 * "Speichern" erst zu, wenn alles stimmt. Die Stunden werden bei Uhrzeit von-bis daraus
 * berechnet, abzüglich der Pause.
 */
final class EintragDialog {

    /** Alles, was ein gültiger Eintrag braucht. fehler != null heißt: noch nicht gültig. */
    private record Ergebnis(String fehler, LocalDate datum, Mitarbeiter m, Projekt p, double stunden,
                            LocalTime von, LocalTime bis, int pause, String text) {

        static Ergebnis fehler(String text) {
            return new Ergebnis(text, null, null, null, 0, null, null, 0, null);
        }
    }

    private final Daten daten;
    private final Eintrag bestehend; // null = neuer Eintrag

    private final DatePicker datum = new DatePicker();
    private final ComboBox<Mitarbeiter> mitarbeiter = new ComboBox<>();
    private final ComboBox<Projekt> projekt = new ComboBox<>();
    private final RadioButton mitUhrzeit = new RadioButton("Uhrzeit von – bis");
    private final RadioButton nurStunden = new RadioButton("Stunden direkt");
    private final TextField von = new TextField();
    private final TextField bis = new TextField();
    private final TextField pause = new TextField();
    private final TextField stunden = new TextField();
    private final TextArea beschreibung = new TextArea();
    private final Label hinweis = new Label();

    private EintragDialog(Daten daten, Eintrag bestehend) {
        this.daten = daten;
        this.bestehend = bestehend;
    }

    /** Zeigt das Fenster. Gibt true zurück, wenn gespeichert wurde. */
    static boolean zeige(Daten daten, Eintrag bestehend, Window besitzer) {
        return new EintragDialog(daten, bestehend).oeffnen(besitzer);
    }

    private boolean oeffnen(Window besitzer) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(besitzer);
        dialog.setTitle(bestehend == null ? "Neuer Eintrag" : "Eintrag #" + bestehend.getId() + " bearbeiten");
        dialog.setHeaderText(null);
        ButtonType speichern = new ButtonType("Speichern", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(speichern, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(StundenbuchApp.stylesheet());
        dialog.getDialogPane().setContent(aufbauen());
        Button ok = (Button) dialog.getDialogPane().lookupButton(speichern);
        ok.getStyleClass().add("primaer");

        vorbelegen();
        Runnable pruefen = () -> {
            Ergebnis e = ergebnis();
            ok.setDisable(e.fehler() != null);
            hinweis.getStyleClass().removeAll("fehler", "gut");
            if (e.fehler() != null) {
                hinweis.setText(e.fehler());
                hinweis.getStyleClass().add("fehler");
            } else {
                hinweis.setText("= " + Format.stunden(e.stunden())
                        + (e.pause() > 0 ? "  (ohne " + e.pause() + " Min. Pause)" : ""));
                hinweis.getStyleClass().add("gut");
            }
            boolean zeit = mitUhrzeit.isSelected();
            von.setDisable(!zeit);
            bis.setDisable(!zeit);
            pause.setDisable(!zeit);
            stunden.setDisable(zeit);
        };
        datum.getEditor().textProperty().addListener((o, a, b) -> pruefen.run());
        datum.valueProperty().addListener((o, a, b) -> pruefen.run());
        mitarbeiter.valueProperty().addListener((o, a, b) -> pruefen.run());
        projekt.valueProperty().addListener((o, a, b) -> pruefen.run());
        mitUhrzeit.selectedProperty().addListener((o, a, b) -> pruefen.run());
        von.textProperty().addListener((o, a, b) -> pruefen.run());
        bis.textProperty().addListener((o, a, b) -> pruefen.run());
        pause.textProperty().addListener((o, a, b) -> pruefen.run());
        stunden.textProperty().addListener((o, a, b) -> pruefen.run());
        beschreibung.textProperty().addListener((o, a, b) -> pruefen.run());
        pruefen.run();

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != speichern) {
            return false;
        }
        uebernehmen(ergebnis());
        return true;
    }

    private Node aufbauen() {
        datum.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate d) {
                return d == null ? "" : d.format(Format.DATUM);
            }

            @Override
            public LocalDate fromString(String s) {
                return s == null || s.isBlank() ? null : Eingabe.datum(s);
            }
        });
        datum.setEditable(true);
        mitarbeiter.setItems(javafx.collections.FXCollections.observableArrayList(daten.z().getMitarbeiter()));
        projekt.setItems(javafx.collections.FXCollections.observableArrayList(daten.z().getProjekte()));
        mitarbeiter.setMaxWidth(Double.MAX_VALUE);
        projekt.setMaxWidth(Double.MAX_VALUE);

        ToggleGroup gruppe = new ToggleGroup();
        mitUhrzeit.setToggleGroup(gruppe);
        nurStunden.setToggleGroup(gruppe);
        von.setPromptText("8:00");
        bis.setPromptText("15:30");
        pause.setPromptText("z. B. 30 oder 0,5h");
        stunden.setPromptText("z. B. 4,5");
        beschreibung.setPromptText("Was wurde gemacht?");
        beschreibung.setPrefRowCount(3);
        beschreibung.setWrapText(true);
        von.setPrefColumnCount(6);
        bis.setPrefColumnCount(6);

        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(10);
        g.setPadding(new Insets(8, 4, 4, 4));
        g.setMinWidth(480);
        int r = 0;
        g.addRow(r++, new Label("Datum"), datum);
        g.addRow(r++, new Label("Mitarbeiter"), mitarbeiter);
        g.addRow(r++, new Label("Projekt"), projekt);
        g.add(new HBox(18, mitUhrzeit, nurStunden), 1, r++);
        g.addRow(r++, new Label("Von"), new HBox(8, von, new Label("bis"), bis));
        g.addRow(r++, new Label("Pause"), pause);
        g.addRow(r++, new Label("Stunden"), stunden);
        g.addRow(r++, new Label("Beschreibung"), beschreibung);
        g.add(hinweis, 1, r);
        GridPane.setHgrow(mitarbeiter, Priority.ALWAYS);
        GridPane.setHgrow(beschreibung, Priority.ALWAYS);
        VBox box = new VBox(g);
        box.getStyleClass().add("dialog-inhalt");
        return box;
    }

    private void vorbelegen() {
        if (bestehend == null) {
            datum.setValue(LocalDate.now());
            mitUhrzeit.setSelected(true);
            if (daten.z().getMitarbeiter().size() == 1) {
                mitarbeiter.setValue(daten.z().getMitarbeiter().get(0));
            }
            if (daten.z().getProjekte().size() == 1) {
                projekt.setValue(daten.z().getProjekte().get(0));
            }
            return;
        }
        Eintrag e = bestehend;
        datum.setValue(e.getDatum());
        mitarbeiter.setValue(e.getMitarbeiter());
        projekt.setValue(e.getProjekt());
        beschreibung.setText(e.getBeschreibung());
        if (e.getVon() != null) {
            mitUhrzeit.setSelected(true);
            von.setText(e.getVon().toString());
            bis.setText(e.getBis().toString());
            pause.setText(e.getPauseMinuten() > 0 ? String.valueOf(e.getPauseMinuten()) : "");
        } else {
            nurStunden.setSelected(true);
            stunden.setText(Format.zahl(e.getStunden()));
        }
    }

    /** Liest alle Felder und prüft sie. Der erste Fehler wird als Text zurückgegeben. */
    private Ergebnis ergebnis() {
        LocalDate d = datum.getValue();
        if (d == null) {
            d = Eingabe.datum(datum.getEditor().getText());
        }
        if (d == null) {
            return Ergebnis.fehler("Bitte ein gültiges Datum eingeben (TT.MM.JJJJ).");
        }
        if (mitarbeiter.getValue() == null) {
            return Ergebnis.fehler("Bitte einen Mitarbeiter wählen.");
        }
        if (projekt.getValue() == null) {
            return Ergebnis.fehler("Bitte ein Projekt wählen.");
        }
        double h;
        LocalTime v = null;
        LocalTime b = null;
        int p = 0;
        if (mitUhrzeit.isSelected()) {
            v = Eingabe.zeit(von.getText());
            b = Eingabe.zeit(bis.getText());
            if (v == null || b == null) {
                return Ergebnis.fehler("Bitte Beginn und Ende eingeben, z. B. 8:00 und 15:30.");
            }
            if (!b.isAfter(v)) {
                return Ergebnis.fehler("Das Ende muss nach dem Beginn liegen. Über Mitternacht bitte zwei Einträge machen.");
            }
            Integer pm = Eingabe.pauseMinuten(pause.getText());
            if (pm == null) {
                return Ergebnis.fehler("Pause bitte so: 30 (Minuten) oder 0,5h (Stunden).");
            }
            if (pm >= Duration.between(v, b).toMinutes()) {
                return Ergebnis.fehler("Die Pause muss kürzer sein als die Arbeitszeit.");
            }
            p = pm;
            h = Eintrag.stundenZwischen(v, b, p);
        } else {
            Double s = Eingabe.stunden(stunden.getText());
            if (s == null) {
                return Ergebnis.fehler("Die Stunden müssen größer als 0 und höchstens 24 sein, z. B. 4,5.");
            }
            h = s;
        }
        String text = beschreibung.getText().trim();
        if (text.isEmpty()) {
            return Ergebnis.fehler("Bitte beschreiben, was gemacht wurde.");
        }
        return new Ergebnis(null, d, mitarbeiter.getValue(), projekt.getValue(), h, v, b, p, text);
    }

    private void uebernehmen(Ergebnis e) {
        if (bestehend == null) {
            Eintrag neu = daten.z().addEintrag(e.datum(), e.m(), e.p(), e.stunden(), e.text(), e.von(), e.bis(), e.pause());
            daten.geaendert();
            daten.meldung("Gespeichert: Eintrag #" + neu.getId() + " (" + Format.stunden(e.stunden()) + ")");
        } else {
            bestehend.setZeit(e.von(), e.bis(), e.pause());
            bestehend.setDatum(e.datum());
            bestehend.setMitarbeiter(e.m());
            bestehend.setProjekt(e.p());
            bestehend.setStunden(e.stunden());
            bestehend.setBeschreibung(e.text());
            daten.geaendert();
            daten.meldung("Gespeichert: Eintrag #" + bestehend.getId() + " geändert");
        }
    }
}
