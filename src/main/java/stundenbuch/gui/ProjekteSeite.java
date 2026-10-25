package stundenbuch.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import stundenbuch.anzeige.Format;
import stundenbuch.modell.Projekt;

/** Projekte (Ort und Kunde) anlegen, umbenennen und löschen. */
final class ProjekteSeite implements Seite {

    private final Daten daten;
    private final TableView<Projekt> tabelle = new TableView<>();
    private final Button umbenennen = new Button("Umbenennen");
    private final Button loeschen = new Button("Löschen");
    private final VBox wurzel = new VBox(14);

    ProjekteSeite(Daten daten) {
        this.daten = daten;

        Button neu = new Button("+ Neues Projekt");
        neu.getStyleClass().add("primaer");
        neu.setOnAction(e -> hinzufuegen());
        umbenennen.setOnAction(e -> umbenennen());
        loeschen.getStyleClass().add("gefahr");
        loeschen.setOnAction(e -> loeschen());
        HBox leiste = new HBox(10, neu, umbenennen, loeschen);
        leiste.setAlignment(Pos.CENTER_LEFT);

        tabelle.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabelle.getColumns().add(spalte("Nr", 70, false, p -> String.valueOf(p.getId())));
        tabelle.getColumns().add(spalte("Ort", 220, false, Projekt::getOrt));
        tabelle.getColumns().add(spalte("Kunde", 220, false, Projekt::getKunde));
        tabelle.getColumns().add(spalte("Stunden gesamt", 140, true,
                p -> Format.zahl(daten.z().getStundenGesamt(p, null))));
        tabelle.setPlaceholder(new Label("Noch keine Projekte."));
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

    private TableColumn<Projekt, String> spalte(String name, double breite, boolean rechts,
                                                java.util.function.Function<Projekt, String> wert) {
        TableColumn<Projekt, String> s = new TableColumn<>(name);
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
        return "Projekte";
    }

    @Override
    public Node inhalt() {
        return wurzel;
    }

    @Override
    public void aktualisieren() {
        tabelle.setItems(FXCollections.observableArrayList(daten.z().getProjekte()));
        tabelle.refresh();
        auswahlGeaendert();
    }

    private void auswahlGeaendert() {
        boolean keines = tabelle.getSelectionModel().getSelectedItem() == null;
        umbenennen.setDisable(keines);
        loeschen.setDisable(keines);
    }

    /** Fragt Ort und Kunde ab. Gibt null zurück bei Abbrechen oder wenn etwas fehlt. */
    private String[] frageProjekt(String titel, String ort, String kunde) {
        Dialog<ButtonType> d = new Dialog<>();
        d.initOwner(StundenbuchApp.fenster());
        d.setTitle(titel);
        d.setHeaderText(null);
        ButtonType ok = new ButtonType("Speichern", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        d.getDialogPane().getStylesheets().add(StundenbuchApp.stylesheet());
        Button okKnopf = (Button) d.getDialogPane().lookupButton(ok);
        okKnopf.getStyleClass().add("primaer");

        TextField ortFeld = new TextField(ort);
        TextField kundeFeld = new TextField(kunde);
        ortFeld.setPromptText("z. B. Graz");
        kundeFeld.setPromptText("z. B. Mustermann");
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(10);
        g.setPadding(new Insets(8, 4, 4, 4));
        g.addRow(0, new Label("Ort"), ortFeld);
        g.addRow(1, new Label("Kunde"), kundeFeld);
        d.getDialogPane().setContent(g);
        Runnable pruefen = () -> okKnopf.setDisable(ortFeld.getText().isBlank() || kundeFeld.getText().isBlank());
        ortFeld.textProperty().addListener((o, a, b) -> pruefen.run());
        kundeFeld.textProperty().addListener((o, a, b) -> pruefen.run());
        pruefen.run();

        if (d.showAndWait().orElse(ButtonType.CANCEL) != ok) {
            return null;
        }
        return new String[]{ortFeld.getText().trim(), kundeFeld.getText().trim()};
    }

    private void hinzufuegen() {
        String[] eingabe = frageProjekt("Neues Projekt", "", "");
        if (eingabe == null) {
            return;
        }
        Projekt p = daten.z().addProjekt(eingabe[0], eingabe[1]);
        if (p == null) {
            fehler("Das Projekt „" + eingabe[0] + "/" + eingabe[1] + "“ gibt es schon.");
            return;
        }
        daten.geaendert();
        daten.meldung("Angelegt: " + p.getName());
    }

    private void umbenennen() {
        Projekt p = tabelle.getSelectionModel().getSelectedItem();
        if (p == null) {
            return;
        }
        String[] neu = frageProjekt("Projekt umbenennen", p.getOrt(), p.getKunde());
        if (neu == null) {
            return;
        }
        String neuerName = neu[0] + "/" + neu[1];
        // Nur die Schreibweise des eigenen Namens zu ändern ist erlaubt
        if (!neuerName.equalsIgnoreCase(p.getName()) && daten.z().istProjektNameVergeben(neu[0], neu[1])) {
            fehler("Das Projekt „" + neuerName + "“ gibt es schon.");
            return;
        }
        p.setOrt(neu[0]);
        p.setKunde(neu[1]);
        daten.geaendert();
        daten.meldung("Umbenannt in: " + neuerName);
    }

    private void loeschen() {
        Projekt p = tabelle.getSelectionModel().getSelectedItem();
        if (p == null) {
            return;
        }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Projekt " + p.getName() + " wirklich löschen?", ButtonType.YES, ButtonType.NO);
        a.setHeaderText("Projekt löschen");
        a.initOwner(StundenbuchApp.fenster());
        a.getDialogPane().getStylesheets().add(StundenbuchApp.stylesheet());
        if (a.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }
        if (!daten.z().loescheProjekt(p)) {
            fehler(p.getName() + " hat noch Einträge. Lösche diese zuerst.");
            return;
        }
        daten.geaendert();
        daten.meldung("Gelöscht: " + p.getName());
    }

    private void fehler(String text) {
        Alert a = new Alert(Alert.AlertType.WARNING, text, ButtonType.OK);
        a.setHeaderText("Geht nicht");
        a.initOwner(StundenbuchApp.fenster());
        a.getDialogPane().getStylesheets().add(StundenbuchApp.stylesheet());
        a.showAndWait();
    }
}
