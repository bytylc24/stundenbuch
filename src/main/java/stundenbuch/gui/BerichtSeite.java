package stundenbuch.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import stundenbuch.anzeige.Format;
import stundenbuch.bericht.Bericht;
import stundenbuch.bericht.BerichtExport;
import stundenbuch.bericht.Tabelle;
import stundenbuch.modell.Mitarbeiter;
import stundenbuch.modell.Projekt;

/**
 * Bericht: Filter wählen, Vorschau ansehen und als Word oder CSV speichern.
 * Die Vorschau ist derselbe {@link Bericht} wie im Terminal und in der Datei.
 */
final class BerichtSeite implements Seite {

    private enum Art { GESAMT, JAHR, MONAT, EIGENER }

    private final Daten daten;

    private final ComboBox<Opt<Art>> zeitraumArt = new ComboBox<>();
    private final ComboBox<Integer> jahr = new ComboBox<>();
    private final ComboBox<YearMonth> monat = new ComboBox<>();
    private final DatePicker vonDatum = new DatePicker();
    private final DatePicker bisDatum = new DatePicker();
    private final Label bisText = new Label("bis");
    private final ComboBox<Opt<Projekt>> projekt = new ComboBox<>();
    private final ComboBox<Opt<Mitarbeiter>> mitarbeiter = new ComboBox<>();
    private final RadioButton mitPause = new RadioButton("Mit Pause (Pause abgezogen, eigene Spalte)");
    private final RadioButton ohnePause = new RadioButton("Ohne Pause (Stunden von Beginn bis Ende)");
    private final CheckBox fuerKunden = new CheckBox("Für Kunden (ohne Uhrzeit, Pause und Stunden-Auswertung pro Mitarbeiter)");
    private final CheckBox einmalBeschreiben = new CheckBox("Gleiche Beschreibung am selben Tag nur einmal zeigen");
    private final Label pauseHinweis = new Label("Noch keine Pausen erfasst – die Wahl wirkt erst dann.");

    private final VBox vorschau = new VBox(14);
    private final Button word = new Button("Als Word speichern");
    private final Button csv = new Button("Als CSV speichern");
    private final Button beides = new Button("Beides");
    private final VBox wurzel = new VBox(14);

    private Bericht bericht;
    private boolean aufbauend;

    BerichtSeite(Daten daten) {
        this.daten = daten;
        aufbauen();
    }

    @Override
    public String titel() {
        return "Bericht";
    }

    @Override
    public Node inhalt() {
        return wurzel;
    }

    private void aufbauen() {
        zeitraumArt.setItems(FXCollections.observableArrayList(
                new Opt<>(Art.GESAMT, "Gesamter Zeitraum"), new Opt<>(Art.JAHR, "Ein Jahr"),
                new Opt<>(Art.MONAT, "Ein Monat"), new Opt<>(Art.EIGENER, "Von – bis")));
        zeitraumArt.setValue(zeitraumArt.getItems().get(0));
        monat.setConverter(new StringConverter<>() {
            @Override
            public String toString(YearMonth m) {
                return m == null ? "" : m.format(Format.MONAT);
            }

            @Override
            public YearMonth fromString(String s) {
                return null;
            }
        });
        for (DatePicker d : List.of(vonDatum, bisDatum)) {
            d.setConverter(new StringConverter<>() {
                @Override
                public String toString(LocalDate x) {
                    return x == null ? "" : x.format(Format.DATUM);
                }

                @Override
                public LocalDate fromString(String s) {
                    return s == null || s.isBlank() ? null : Eingabe.datum(s);
                }
            });
            d.setPrefWidth(130);
        }
        vonDatum.setValue(LocalDate.now().withDayOfMonth(1));
        bisDatum.setValue(LocalDate.now());

        ToggleGroup gruppe = new ToggleGroup();
        mitPause.setToggleGroup(gruppe);
        ohnePause.setToggleGroup(gruppe);
        mitPause.setSelected(true);
        pauseHinweis.getStyleClass().add("hinweis");

        HBox zeile1 = new HBox(10, beschriftung("Zeitraum"), zeitraumArt, jahr, monat,
                vonDatum, bisText, bisDatum);
        zeile1.setAlignment(Pos.CENTER_LEFT);
        HBox zeile2 = new HBox(10, beschriftung("Projekt"), projekt, beschriftung("Mitarbeiter"), mitarbeiter);
        zeile2.setAlignment(Pos.CENTER_LEFT);
        HBox zeile3 = new HBox(18, beschriftung("Pause"), mitPause, ohnePause, pauseHinweis);
        zeile3.setAlignment(Pos.CENTER_LEFT);
        einmalBeschreiben.setSelected(true);
        HBox zeile4 = new HBox(18, beschriftung("Ausgabe"), einmalBeschreiben, fuerKunden);
        zeile4.setAlignment(Pos.CENTER_LEFT);
        VBox filter = new VBox(10, zeile1, zeile2, zeile3, zeile4);
        filter.getStyleClass().add("karte");
        filter.setPadding(new Insets(14));

        Runnable neuBauen = () -> {
            if (!aufbauend) {
                vorschauBauen();
            }
        };
        zeitraumArt.valueProperty().addListener((o, a, b) -> {
            sichtbarkeit();
            neuBauen.run();
        });
        jahr.valueProperty().addListener((o, a, b) -> neuBauen.run());
        monat.valueProperty().addListener((o, a, b) -> neuBauen.run());
        vonDatum.valueProperty().addListener((o, a, b) -> neuBauen.run());
        bisDatum.valueProperty().addListener((o, a, b) -> neuBauen.run());
        projekt.valueProperty().addListener((o, a, b) -> neuBauen.run());
        mitarbeiter.valueProperty().addListener((o, a, b) -> neuBauen.run());
        mitPause.selectedProperty().addListener((o, a, b) -> neuBauen.run());
        einmalBeschreiben.selectedProperty().addListener((o, a, b) -> neuBauen.run());
        fuerKunden.selectedProperty().addListener((o, a, b) -> {
            einmalBeschreiben.setDisable(b); // bei Kunden steht die Beschreibung pro Tag sowieso nur einmal
            neuBauen.run();
        });

        word.getStyleClass().add("primaer");
        word.setOnAction(e -> speichern(true, false));
        csv.setOnAction(e -> speichern(false, true));
        beides.setOnAction(e -> speichern(true, true));
        Button ordner = new Button("Berichte-Ordner öffnen");
        ordner.setOnAction(e -> oeffnen(daten.speicher().getBerichtOrdner()));
        Region platz = new Region();
        HBox.setHgrow(platz, Priority.ALWAYS);
        HBox knoepfe = new HBox(10, word, csv, beides, platz, ordner);
        knoepfe.setAlignment(Pos.CENTER_LEFT);

        ScrollPane scroll = new ScrollPane(vorschau);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("durchsichtig");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        wurzel.getChildren().addAll(filter, scroll, knoepfe);
        sichtbarkeit();
        aktualisieren();
    }

    private static Label beschriftung(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("filter-name");
        return l;
    }

    private void sichtbarkeit() {
        Art art = zeitraumArt.getValue().wert();
        zeige(jahr, art == Art.JAHR);
        zeige(monat, art == Art.MONAT);
        zeige(vonDatum, art == Art.EIGENER);
        zeige(bisText, art == Art.EIGENER);
        zeige(bisDatum, art == Art.EIGENER);
    }

    private static void zeige(Node n, boolean sichtbar) {
        n.setVisible(sichtbar);
        n.setManaged(sichtbar);
    }

    @Override
    public void aktualisieren() {
        aufbauend = true;
        Integer altJahr = jahr.getValue();
        YearMonth altMonat = monat.getValue();
        Projekt altP = projekt.getValue() == null ? null : projekt.getValue().wert();
        Mitarbeiter altM = mitarbeiter.getValue() == null ? null : mitarbeiter.getValue().wert();

        jahr.setItems(FXCollections.observableArrayList(daten.z().getJahreMitEintraegen()));
        jahr.setValue(jahr.getItems().contains(altJahr) ? altJahr
                : jahr.getItems().isEmpty() ? null : jahr.getItems().get(0));
        monat.setItems(FXCollections.observableArrayList(daten.z().getMonateMitEintraegen()));
        monat.setValue(monat.getItems().contains(altMonat) ? altMonat
                : monat.getItems().isEmpty() ? null : monat.getItems().get(0));

        List<Opt<Projekt>> projekte = new ArrayList<>();
        projekte.add(new Opt<>(null, "Alle Projekte"));
        daten.z().getProjekte().forEach(p -> projekte.add(new Opt<>(p, p.getName())));
        projekt.setItems(FXCollections.observableArrayList(projekte));
        projekt.setValue(projekte.stream().filter(o -> o.wert() == altP).findFirst().orElse(projekte.get(0)));

        List<Opt<Mitarbeiter>> leute = new ArrayList<>();
        leute.add(new Opt<>(null, "Alle Mitarbeiter"));
        daten.z().getMitarbeiter().forEach(m -> leute.add(new Opt<>(m, m.getName())));
        mitarbeiter.setItems(FXCollections.observableArrayList(leute));
        mitarbeiter.setValue(leute.stream().filter(o -> o.wert() == altM).findFirst().orElse(leute.get(0)));

        boolean pausen = daten.hatPausen();
        mitPause.setDisable(!pausen);
        ohnePause.setDisable(!pausen);
        zeige(pauseHinweis, !pausen);
        aufbauend = false;
        vorschauBauen();
    }

    /** Baut aus den gewählten Filtern den Bericht oder null, wenn die Auswahl noch nicht passt. */
    private Bericht berichtAusFilter() {
        Bericht.Zeitraum zeitraum;
        switch (zeitraumArt.getValue().wert()) {
            case JAHR -> {
                if (jahr.getValue() == null) {
                    return null;
                }
                zeitraum = Bericht.Zeitraum.jahr(jahr.getValue());
            }
            case MONAT -> {
                if (monat.getValue() == null) {
                    return null;
                }
                zeitraum = Bericht.Zeitraum.monat(monat.getValue());
            }
            case EIGENER -> {
                LocalDate von = vonDatum.getValue();
                LocalDate bis = bisDatum.getValue();
                if (von == null || bis == null || bis.isBefore(von)) {
                    return null;
                }
                zeitraum = Bericht.Zeitraum.vonBis(von, bis);
            }
            default -> zeitraum = Bericht.Zeitraum.gesamt();
        }
        Projekt p = projekt.getValue() == null ? null : projekt.getValue().wert();
        Mitarbeiter m = mitarbeiter.getValue() == null ? null : mitarbeiter.getValue().wert();
        return new Bericht(daten.z(), zeitraum, p, m, mitPause.isSelected() || !daten.hatPausen(),
                fuerKunden.isSelected(), einmalBeschreiben.isSelected());
    }

    private void vorschauBauen() {
        vorschau.getChildren().clear();
        bericht = berichtAusFilter();
        boolean speicherbar = bericht != null && !bericht.istLeer();
        word.setDisable(!speicherbar);
        csv.setDisable(!speicherbar);
        beides.setDisable(!speicherbar);

        if (bericht == null) {
            vorschau.getChildren().add(hinweis("Bitte einen gültigen Zeitraum wählen (das Ende darf nicht vor dem Anfang liegen)."));
            return;
        }
        Label titel = new Label(bericht.getTitel() + " – Vorschau");
        titel.getStyleClass().add("zwischentitel");
        vorschau.getChildren().add(titel);

        VBox angaben = new VBox(4);
        for (String[] a : bericht.getAngaben()) {
            Label name = new Label(a[0]);
            name.getStyleClass().add("filter-name");
            name.setMinWidth(100);
            angaben.getChildren().add(new HBox(10, name, new Label(a[1])));
        }
        vorschau.getChildren().add(angaben);

        if (bericht.istLeer()) {
            vorschau.getChildren().add(hinweis("Keine Einträge für diese Auswahl."));
            return;
        }
        FlowPane kacheln = new FlowPane(12, 12);
        for (String[] k : bericht.getKennzahlen()) {
            Label wert = new Label(k[0]);
            wert.getStyleClass().add("kachel-wert");
            Label text = new Label(k[1]);
            text.getStyleClass().add("hinweis");
            VBox kachel = new VBox(2, wert, text);
            kachel.getStyleClass().add("karte");
            kachel.setPadding(new Insets(12, 18, 12, 18));
            kacheln.getChildren().add(kachel);
        }
        vorschau.getChildren().add(kacheln);

        for (Tabelle t : bericht.getTabellen()) {
            Label ueberschrift = new Label(t.getTitel());
            ueberschrift.getStyleClass().add("zwischentitel");
            vorschau.getChildren().addAll(ueberschrift, TabellenAnsicht.erstellen(t));
        }
        Label dateiname = hinweis("Dateiname: " + bericht.getDateiname() + ".docx / .csv");
        vorschau.getChildren().add(dateiname);
    }

    private static Label hinweis(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("hinweis");
        return l;
    }

    // ---------- Speichern ----------

    private void speichern(boolean alsWord, boolean alsCsv) {
        if (bericht == null || bericht.istLeer()) {
            return;
        }
        Path docx = alsWord ? schreiben(".docx") : null;
        Path csvDatei = alsCsv ? schreiben(".csv") : null;
        if (docx == null && csvDatei == null) {
            return;
        }
        Path ziel = alsWord && alsCsv ? daten.speicher().getBerichtOrdner() : (alsWord ? docx : csvDatei);
        daten.meldung("Gespeichert: " + (alsWord && alsCsv ? ziel : ziel.getFileName()));

        ButtonType oeffnen = new ButtonType(alsWord && alsCsv ? "Ordner öffnen" : "Datei öffnen");
        Alert a = new Alert(Alert.AlertType.INFORMATION,
                (alsWord && alsCsv ? "Word und CSV gespeichert in:\n" : "Gespeichert:\n") + ziel,
                oeffnen, ButtonType.CLOSE);
        a.setHeaderText("Bericht gespeichert");
        a.getDialogPane().setMinWidth(620);
        a.initOwner(StundenbuchApp.fenster());
        a.getDialogPane().getStylesheets().add(StundenbuchApp.stylesheet());
        if (a.showAndWait().orElse(ButtonType.CLOSE) == oeffnen) {
            oeffnen(ziel);
        }
    }

    /** Schreibt die Datei in den Berichte-Ordner. Gibt es sie schon, wird gefragt (Nein = neue Nummer). */
    private Path schreiben(String endung) {
        Path ordner = daten.speicher().getBerichtOrdner();
        Path datei = ordner.resolve(bericht.getDateiname() + endung);
        if (Files.exists(datei)) {
            Alert frage = new Alert(Alert.AlertType.CONFIRMATION,
                    "Die Datei " + datei.getFileName() + " gibt es schon. Überschreiben?\n"
                            + "(Bei „Nein“ wird eine neue Datei mit Nummer angelegt.)",
                    ButtonType.YES, ButtonType.NO);
            frage.setHeaderText("Datei vorhanden");
            frage.initOwner(StundenbuchApp.fenster());
            frage.getDialogPane().getStylesheets().add(StundenbuchApp.stylesheet());
            if (frage.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
                for (int nr = 2; Files.exists(datei); nr++) {
                    datei = ordner.resolve(bericht.getDateiname() + "_" + nr + endung);
                }
            }
        }
        try {
            Files.createDirectories(ordner);
            if (endung.equals(".docx")) {
                BerichtExport.alsDocx(bericht, datei);
            } else {
                BerichtExport.alsCsv(bericht, datei);
            }
            return datei;
        } catch (IOException ex) {
            Alert a = new Alert(Alert.AlertType.ERROR,
                    "Speichern fehlgeschlagen: " + ex.getMessage()
                            + "\n\nIst die Datei gerade in Word oder Excel offen? Dann dort schließen und nochmal speichern.",
                    ButtonType.OK);
            a.setHeaderText("Nicht gespeichert");
            a.initOwner(StundenbuchApp.fenster());
            a.getDialogPane().getStylesheets().add(StundenbuchApp.stylesheet());
            a.showAndWait();
            return null;
        }
    }

    /** Der Explorer öffnet eine Datei mit dem passenden Programm (Word, Excel) oder einen Ordner. */
    private void oeffnen(Path pfad) {
        try {
            Files.createDirectories(pfad.toFile().isDirectory() || !pfad.toString().contains(".")
                    ? pfad : pfad.getParent());
            new ProcessBuilder("explorer.exe", pfad.toString()).start();
        } catch (IOException ex) {
            daten.meldung("Konnte nicht geöffnet werden: " + ex.getMessage());
        }
    }
}
