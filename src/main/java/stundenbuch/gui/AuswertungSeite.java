package stundenbuch.gui;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import stundenbuch.anzeige.Format;
import stundenbuch.bericht.Tabelle;
import stundenbuch.modell.Mitarbeiter;
import stundenbuch.modell.Projekt;

/** Tabelle Projekte × Mitarbeiter, gesamt oder für einen Monat (wie Menüpunkt 5 im Terminal). */
final class AuswertungSeite implements Seite {

    private final Daten daten;
    private final ComboBox<Opt<YearMonth>> monatAuswahl = new ComboBox<>();
    private final VBox inhalt = new VBox(12);
    private final VBox wurzel = new VBox(14);
    private boolean aufbauend;

    AuswertungSeite(Daten daten) {
        this.daten = daten;
        monatAuswahl.valueProperty().addListener((o, a, b) -> {
            if (!aufbauend) {
                tabelleZeichnen();
            }
        });
        HBox leiste = new HBox(10, new Label("Zeitraum"), monatAuswahl);
        leiste.setAlignment(Pos.CENTER_LEFT);
        ScrollPane scroll = new ScrollPane(inhalt);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("durchsichtig");
        VBox.setVgrow(scroll, javafx.scene.layout.Priority.ALWAYS);
        wurzel.getChildren().addAll(leiste, scroll);
        aktualisieren();
    }

    @Override
    public String titel() {
        return "Auswertung";
    }

    @Override
    public Node inhalt() {
        return wurzel;
    }

    @Override
    public void aktualisieren() {
        aufbauend = true;
        YearMonth alt = monatAuswahl.getValue() == null ? null : monatAuswahl.getValue().wert();
        List<Opt<YearMonth>> monate = new ArrayList<>();
        monate.add(new Opt<>(null, "Gesamter Zeitraum"));
        daten.z().getMonateMitEintraegen().forEach(m -> monate.add(new Opt<>(m, m.format(Format.MONAT))));
        monatAuswahl.setItems(FXCollections.observableArrayList(monate));
        monatAuswahl.setValue(monate.stream().filter(o -> Objects.equals(o.wert(), alt))
                .findFirst().orElse(monate.get(0)));
        aufbauend = false;
        tabelleZeichnen();
    }

    private void tabelleZeichnen() {
        inhalt.getChildren().clear();
        List<Projekt> projekte = daten.z().getProjekte();
        List<Mitarbeiter> mitarbeiter = daten.z().getMitarbeiter();
        if (projekte.isEmpty() || mitarbeiter.isEmpty()) {
            Label leer = new Label("Lege zuerst Mitarbeiter und Projekte an, dann erscheint hier die Auswertung.");
            leer.getStyleClass().add("hinweis");
            inhalt.getChildren().add(leer);
            return;
        }
        YearMonth monat = monatAuswahl.getValue() == null ? null : monatAuswahl.getValue().wert();

        Tabelle t = new Tabelle("Stunden pro Projekt").spalte("Projekt");
        for (Mitarbeiter m : mitarbeiter) {
            t.zahlenSpalte(m.getName());
        }
        t.zahlenSpalte("Gesamt");
        for (Projekt p : projekte) {
            List<String> zeile = new ArrayList<>(List.of(p.getName()));
            for (Mitarbeiter m : mitarbeiter) {
                zeile.add(Format.zahl(daten.z().getStunden(p, m, monat)));
            }
            zeile.add(Format.zahl(daten.z().getStundenGesamt(p, monat)));
            t.zeile(zeile.toArray(String[]::new));
        }
        List<String> summe = new ArrayList<>(List.of("Summe"));
        for (Mitarbeiter m : mitarbeiter) {
            summe.add(Format.zahl(daten.z().getStundenGesamt(m, monat)));
        }
        summe.add(Format.zahl(daten.z().getStundenGesamt(monat)));
        t.summe(summe.toArray(String[]::new));

        Label titel = new Label("Stunden pro Projekt und Mitarbeiter");
        titel.getStyleClass().add("zwischentitel");
        inhalt.getChildren().addAll(titel, TabellenAnsicht.erstellen(t));
    }
}
