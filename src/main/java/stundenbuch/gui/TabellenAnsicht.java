package stundenbuch.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import stundenbuch.bericht.Tabelle;

/**
 * Zeichnet eine {@link Tabelle} (dieselbe wie für Terminal, Word und CSV) als JavaFX-Tabelle.
 */
final class TabellenAnsicht {

    private static final double ZEILENHOEHE = 32;
    private static final double KOPFHOEHE = 40;

    private TabellenAnsicht() {
    }

    /** Breite nach dem längsten Text der Spalte (grob 8 Pixel pro Zeichen), damit nichts abgeschnitten wird. */
    private static double breite(Tabelle t, int index) {
        int laenge = t.getSpalten().get(index).name().length() + 2;
        for (Tabelle.Zeile z : t.getZeilen()) {
            if (index < z.zellen().size()) {
                laenge = Math.max(laenge, z.zellen().get(index).length() + (z.art() == Tabelle.Art.NORMAL ? 0 : 2));
            }
        }
        return Math.min(380, Math.max(70, laenge * 8.5 + 28));
    }

    /** Die Tabelle ist so hoch wie ihr Inhalt, damit mehrere untereinander in einer Seite Platz haben. */
    static TableView<Tabelle.Zeile> erstellen(Tabelle t) {
        TableView<Tabelle.Zeile> view = new TableView<>(FXCollections.observableArrayList(t.getZeilen()));
        view.getStyleClass().add("daten-tabelle");
        view.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        view.setFixedCellSize(ZEILENHOEHE);
        view.setSelectionModel(null);
        view.setFocusTraversable(false);

        for (int i = 0; i < t.getSpalten().size(); i++) {
            final int index = i;
            Tabelle.Spalte s = t.getSpalten().get(i);
            TableColumn<Tabelle.Zeile, String> spalte = new TableColumn<>(s.name());
            spalte.setSortable(false);
            spalte.setReorderable(false);
            spalte.setPrefWidth(breite(t, index));
            spalte.setCellValueFactory(c -> new SimpleStringProperty(
                    index < c.getValue().zellen().size() ? c.getValue().zellen().get(index) : ""));
            spalte.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String text, boolean leer) {
                    super.updateItem(text, leer);
                    setText(leer ? null : text);
                    // Zahlen rechts, bei Gruppen und Summen ist der Text meist die Beschriftung
                    setStyle(s.rechts() ? "-fx-alignment: CENTER-RIGHT;" : "-fx-alignment: CENTER-LEFT;");
                }
            });
            view.getColumns().add(spalte);
        }

        view.setRowFactory(v -> new TableRow<>() {
            @Override
            protected void updateItem(Tabelle.Zeile z, boolean leer) {
                super.updateItem(z, leer);
                getStyleClass().removeAll("zeile-gruppe", "zeile-summe");
                setStyle(null);
                if (!leer && z != null && z.block() > 0) {
                    // Ein Tag (gleicher Block) wird gemeinsam umrandet: oben bei der ersten, unten bei der letzten Zeile
                    var zeilen = getTableView().getItems();
                    int i = getIndex();
                    boolean erste = i == 0 || zeilen.get(i - 1).block() != z.block();
                    boolean letzte = i == zeilen.size() - 1 || zeilen.get(i + 1).block() != z.block();
                    setStyle("-fx-border-color: #1f4e79; -fx-border-width: " + (erste ? 1.5 : 0) + " 1.5 "
                            + (letzte ? 1.5 : 0) + " 1.5;");
                }
                if (!leer && z != null) {
                    switch (z.art()) {
                        case GRUPPE -> getStyleClass().add("zeile-gruppe");
                        case ZWISCHENSUMME, SUMME -> getStyleClass().add("zeile-summe");
                        default -> { }
                    }
                }
            }
        });

        double hoehe = t.getZeilen().size() * ZEILENHOEHE + KOPFHOEHE + 6;
        view.setPrefHeight(hoehe);
        view.setMinHeight(hoehe);
        view.setMaxHeight(hoehe);
        return view;
    }
}
