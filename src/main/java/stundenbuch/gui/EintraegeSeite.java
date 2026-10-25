package stundenbuch.gui;

import java.time.YearMonth;
import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import stundenbuch.anzeige.Format;
import stundenbuch.modell.Eintrag;
import stundenbuch.modell.Mitarbeiter;

/** Alle Einträge als Tabelle, neueste zuerst. Hier wird erfasst, bearbeitet und gelöscht. */
final class EintraegeSeite implements Seite {

    private final Daten daten;

    private final TableView<Eintrag> tabelle = new TableView<>();
    private final ComboBox<Opt<YearMonth>> monatAuswahl = new ComboBox<>();
    private final ComboBox<Opt<Mitarbeiter>> mitarbeiterAuswahl = new ComboBox<>();
    private final Label summe = new Label();
    private final Button bearbeiten = new Button("Bearbeiten");
    private final Button loeschen = new Button("Löschen");
    private final VBox wurzel = new VBox(14);
    private boolean aufbauend;

    EintraegeSeite(Daten daten) {
        this.daten = daten;
        aufbauen();
    }

    @Override
    public String titel() {
        return "Einträge";
    }

    @Override
    public Node inhalt() {
        return wurzel;
    }

    private void aufbauen() {
        Button neu = new Button("+ Neuer Eintrag");
        neu.getStyleClass().add("primaer");
        neu.setOnAction(e -> neuerEintrag());
        bearbeiten.setOnAction(e -> bearbeiten());
        loeschen.getStyleClass().add("gefahr");
        loeschen.setOnAction(e -> loeschen());

        monatAuswahl.valueProperty().addListener((o, a, b) -> {
            if (!aufbauend) {
                tabelleFuellen();
            }
        });
        mitarbeiterAuswahl.valueProperty().addListener((o, a, b) -> {
            if (!aufbauend) {
                tabelleFuellen();
            }
        });

        Region platz = new Region();
        HBox.setHgrow(platz, Priority.ALWAYS);
        HBox leiste = new HBox(10, neu, bearbeiten, loeschen, platz,
                new Label("Monat"), monatAuswahl, new Label("Mitarbeiter"), mitarbeiterAuswahl);
        leiste.setAlignment(Pos.CENTER_LEFT);

        spaltenAnlegen();
        tabelle.setPlaceholder(new Label("Noch keine Einträge. Mit „+ Neuer Eintrag“ geht es los."));
        tabelle.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> auswahlGeaendert());
        tabelle.setRowFactory(v -> {
            javafx.scene.control.TableRow<Eintrag> zeile = new javafx.scene.control.TableRow<>();
            zeile.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !zeile.isEmpty()) {
                    bearbeiten();
                }
            });
            return zeile;
        });
        tabelle.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.DELETE) {
                loeschen();
            } else if (ev.getCode() == KeyCode.ENTER) {
                bearbeiten();
            }
        });
        VBox.setVgrow(tabelle, Priority.ALWAYS);

        summe.getStyleClass().add("hinweis");
        wurzel.getChildren().addAll(leiste, tabelle, summe);
        aktualisieren();
    }

    private void spaltenAnlegen() {
        tabelle.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabelle.getColumns().add(spalte("#", 55, false, e -> String.valueOf(e.getId())));
        tabelle.getColumns().add(spalte("Datum", 160, false, e -> e.getDatum().format(Format.TAG)));
        tabelle.getColumns().add(spalte("Mitarbeiter", 130, false, e -> e.getMitarbeiter().getName()));
        tabelle.getColumns().add(spalte("Projekt", 210, false, e -> e.getProjekt().getName()));
        tabelle.getColumns().add(spalte("Zeit", 110, false, Eintrag::getZeitText));
        tabelle.getColumns().add(spalte("Pause", 90, true,
                e -> e.getPauseMinuten() > 0 ? e.getPauseMinuten() + " Min." : ""));
        tabelle.getColumns().add(spalte("Stunden", 95, true, e -> Format.zahl(e.getStunden())));
        tabelle.getColumns().add(spalte("Beschreibung", 300, false, Eintrag::getBeschreibung));
    }

    private TableColumn<Eintrag, String> spalte(String name, double breite, boolean rechts,
                                                java.util.function.Function<Eintrag, String> wert) {
        TableColumn<Eintrag, String> s = new TableColumn<>(name);
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
    public void aktualisieren() {
        // Auswahllisten neu füllen und die bisherige Auswahl behalten, wenn es sie noch gibt
        aufbauend = true;
        YearMonth altMonat = monatAuswahl.getValue() == null ? null : monatAuswahl.getValue().wert();
        Mitarbeiter altM = mitarbeiterAuswahl.getValue() == null ? null : mitarbeiterAuswahl.getValue().wert();

        List<Opt<YearMonth>> monate = new java.util.ArrayList<>();
        monate.add(new Opt<>(null, "Alle Monate"));
        daten.z().getMonateMitEintraegen().forEach(m -> monate.add(new Opt<>(m, m.format(Format.MONAT))));
        monatAuswahl.setItems(FXCollections.observableArrayList(monate));
        monatAuswahl.setValue(monate.stream().filter(o -> java.util.Objects.equals(o.wert(), altMonat))
                .findFirst().orElse(monate.get(0)));

        List<Opt<Mitarbeiter>> leute = new java.util.ArrayList<>();
        leute.add(new Opt<>(null, "Alle"));
        daten.z().getMitarbeiter().forEach(m -> leute.add(new Opt<>(m, m.getName())));
        mitarbeiterAuswahl.setItems(FXCollections.observableArrayList(leute));
        mitarbeiterAuswahl.setValue(leute.stream().filter(o -> o.wert() == altM)
                .findFirst().orElse(leute.get(0)));
        aufbauend = false;

        tabelleFuellen();
    }

    private void tabelleFuellen() {
        YearMonth monat = monatAuswahl.getValue() == null ? null : monatAuswahl.getValue().wert();
        Mitarbeiter m = mitarbeiterAuswahl.getValue() == null ? null : mitarbeiterAuswahl.getValue().wert();
        List<Eintrag> liste = daten.z().getEintraegeNachDatum().stream()
                .filter(e -> monat == null || YearMonth.from(e.getDatum()).equals(monat))
                .filter(e -> m == null || e.getMitarbeiter() == m)
                .toList();
        tabelle.setItems(FXCollections.observableArrayList(liste));
        double gesamt = liste.stream().mapToDouble(Eintrag::getStunden).sum();
        long tage = liste.stream().map(Eintrag::getDatum).distinct().count();
        summe.setText(liste.size() + (liste.size() == 1 ? " Eintrag" : " Einträge") + " · " + tage
                + (tage == 1 ? " Arbeitstag" : " Arbeitstage") + " · " + Format.stunden(gesamt));
        auswahlGeaendert();
    }

    private void auswahlGeaendert() {
        boolean keine = tabelle.getSelectionModel().getSelectedItem() == null;
        bearbeiten.setDisable(keine);
        loeschen.setDisable(keine);
    }

    private void neuerEintrag() {
        if (daten.z().getMitarbeiter().isEmpty() || daten.z().getProjekte().isEmpty()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION,
                    "Lege zuerst mindestens einen Mitarbeiter und ein Projekt an (links im Menü).", ButtonType.OK);
            a.setHeaderText("Noch nichts zum Auswählen");
            a.initOwner(StundenbuchApp.fenster());
            a.showAndWait();
            return;
        }
        EintragDialog.zeige(daten, null, StundenbuchApp.fenster());
    }

    private void bearbeiten() {
        Eintrag e = tabelle.getSelectionModel().getSelectedItem();
        if (e != null) {
            EintragDialog.zeige(daten, e, StundenbuchApp.fenster());
        }
    }

    private void loeschen() {
        Eintrag e = tabelle.getSelectionModel().getSelectedItem();
        if (e == null) {
            return;
        }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Eintrag #" + e.getId() + " (" + e.getDatum().format(Format.DATUM) + ", "
                        + e.getMitarbeiter().getName() + ", " + Format.stunden(e.getStunden()) + ") wirklich löschen?",
                ButtonType.YES, ButtonType.NO);
        a.setHeaderText("Eintrag löschen");
        a.initOwner(StundenbuchApp.fenster());
        a.getDialogPane().getStylesheets().add(StundenbuchApp.stylesheet());
        if (a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            daten.z().loescheEintrag(e);
            daten.geaendert();
            daten.meldung("Gelöscht: Eintrag #" + e.getId());
        }
    }
}
