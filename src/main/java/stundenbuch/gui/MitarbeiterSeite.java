package stundenbuch.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import stundenbuch.anzeige.Format;
import stundenbuch.modell.Mitarbeiter;

/** Mitarbeiter anlegen, umbenennen und löschen. */
final class MitarbeiterSeite implements Seite {

    private final Daten daten;
    private final TableView<Mitarbeiter> tabelle = new TableView<>();
    private final Button umbenennen = new Button("Umbenennen");
    private final Button loeschen = new Button("Löschen");
    private final VBox wurzel = new VBox(14);

    MitarbeiterSeite(Daten daten) {
        this.daten = daten;

        Button neu = new Button("+ Neuer Mitarbeiter");
        neu.getStyleClass().add("primaer");
        neu.setOnAction(e -> hinzufuegen());
        umbenennen.setOnAction(e -> umbenennen());
        loeschen.getStyleClass().add("gefahr");
        loeschen.setOnAction(e -> loeschen());
        HBox leiste = new HBox(10, neu, umbenennen, loeschen);
        leiste.setAlignment(Pos.CENTER_LEFT);

        tabelle.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabelle.getColumns().add(spalte("Kennung", 120, false, Mitarbeiter::getKennung));
        tabelle.getColumns().add(spalte("Name", 300, false, Mitarbeiter::getName));
        tabelle.getColumns().add(spalte("Stunden gesamt", 140, true,
                m -> Format.zahl(daten.z().getStundenGesamt(m, null))));
        tabelle.setPlaceholder(new Label("Noch keine Mitarbeiter."));
        tabelle.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> auswahlGeaendert());
        tabelle.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                umbenennen();
            }
        });
        VBox.setVgrow(tabelle, Priority.ALWAYS);

        wurzel.getChildren().addAll(leiste, tabelle);
        aktualisieren();
    }

    private TableColumn<Mitarbeiter, String> spalte(String name, double breite, boolean rechts,
                                                    java.util.function.Function<Mitarbeiter, String> wert) {
        TableColumn<Mitarbeiter, String> s = new TableColumn<>(name);
        s.setPrefWidth(breite);
        s.setSortable(false);
        s.setReorderable(false);
        s.setCellValueFactory(c -> new SimpleStringProperty(wert.apply(c.getValue())));
        s.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String text, boolean leer) {
                super.updateItem(text, leer);
                setText(leer ? null : text);
                setStyle(rechts ? "-fx-alignment: CENTER-RIGHT;" : "-fx-alignment: CENTER-LEFT;");
            }
        });
        return s;
    }

    @Override
    public String titel() {
        return "Mitarbeiter";
    }

    @Override
    public Node inhalt() {
        return wurzel;
    }

    @Override
    public void aktualisieren() {
        tabelle.setItems(FXCollections.observableArrayList(daten.z().getMitarbeiter()));
        tabelle.refresh();
        auswahlGeaendert();
    }

    private void auswahlGeaendert() {
        boolean keine = tabelle.getSelectionModel().getSelectedItem() == null;
        umbenennen.setDisable(keine);
        loeschen.setDisable(keine);
    }

    /** Fragt einen Namen ab. Gibt null zurück bei Abbrechen oder leerer Eingabe. */
    private String frageName(String titel, String frage, String vorgabe) {
        TextInputDialog d = new TextInputDialog(vorgabe);
        d.initOwner(StundenbuchApp.fenster());
        d.getDialogPane().getStylesheets().add(StundenbuchApp.stylesheet());
        d.setTitle(titel);
        d.setHeaderText(null);
        d.setContentText(frage);
        String name = d.showAndWait().map(String::trim).orElse("");
        return name.isEmpty() ? null : name;
    }

    private void hinzufuegen() {
        String name = frageName("Neuer Mitarbeiter", "Name:", "");
        if (name == null) {
            return;
        }
        Mitarbeiter m = daten.z().addMitarbeiter(name);
        if (m == null) {
            fehler("Den Namen „" + name + "“ gibt es schon.");
            return;
        }
        daten.geaendert();
        daten.meldung("Angelegt: " + name + " (" + m.getKennung() + ")");
    }

    private void umbenennen() {
        Mitarbeiter m = tabelle.getSelectionModel().getSelectedItem();
        if (m == null) {
            return;
        }
        String neu = frageName("Mitarbeiter umbenennen", "Neuer Name:", m.getName());
        if (neu == null) {
            return;
        }
        // Nur die Schreibweise des eigenen Namens zu ändern ist erlaubt
        if (!neu.equalsIgnoreCase(m.getName()) && daten.z().istMitarbeiterNameVergeben(neu)) {
            fehler("Den Namen „" + neu + "“ gibt es schon.");
            return;
        }
        m.setName(neu);
        daten.geaendert();
        daten.meldung("Umbenannt in: " + neu);
    }

    private void loeschen() {
        Mitarbeiter m = tabelle.getSelectionModel().getSelectedItem();
        if (m == null) {
            return;
        }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Mitarbeiter " + m.getName() + " wirklich löschen?", ButtonType.YES, ButtonType.NO);
        a.setHeaderText("Mitarbeiter löschen");
        a.initOwner(StundenbuchApp.fenster());
        a.getDialogPane().getStylesheets().add(StundenbuchApp.stylesheet());
        if (a.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }
        if (!daten.z().loescheMitarbeiter(m)) {
            fehler(m.getName() + " hat noch Einträge. Lösche diese zuerst.");
            return;
        }
        daten.geaendert();
        daten.meldung("Gelöscht: " + m.getName());
    }

    private void fehler(String text) {
        Alert a = new Alert(Alert.AlertType.WARNING, text, ButtonType.OK);
        a.setHeaderText("Geht nicht");
        a.initOwner(StundenbuchApp.fenster());
        a.getDialogPane().getStylesheets().add(StundenbuchApp.stylesheet());
        a.showAndWait();
    }
}
