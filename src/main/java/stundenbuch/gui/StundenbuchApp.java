package stundenbuch.gui;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import stundenbuch.anzeige.Format;
import stundenbuch.speicher.Speicher;

/**
 * Hauptfenster: Menü links, Seite rechts, Statuszeile unten. Die Daten (CSV-Dateien) sind
 * dieselben wie bei der Terminal-Version.
 */
public class StundenbuchApp extends Application {

    private static Stage fenster;

    private Daten daten;
    private final StackPane seitenBereich = new StackPane();
    private final Label seitenTitel = new Label();
    private final Label untertitel = new Label();
    private final Label status = new Label();

    static Window fenster() {
        return fenster;
    }

    static String stylesheet() {
        return StundenbuchApp.class.getResource("stundenbuch.css").toExternalForm();
    }

    @Override
    public void start(Stage stage) {
        Locale.setDefault(Format.DEUTSCH);
        fenster = stage;
        daten = new Daten(new Speicher());
        daten.setMeldungen(text -> status.setText(text));

        List<Seite> seiten = List.of(new EintraegeSeite(daten), new AuswertungSeite(daten),
                new BerichtSeite(daten), new MitarbeiterSeite(daten), new ProjekteSeite(daten));
        for (Seite s : seiten) {
            daten.beobachte(s::aktualisieren);
        }
        daten.beobachte(this::kopfAktualisieren);

        // Menü links
        Label logo = new Label("Stundenbuch");
        logo.getStyleClass().add("logo");
        VBox menue = new VBox(4, logo);
        menue.getStyleClass().add("menue");
        menue.setPadding(new Insets(20, 12, 16, 12));
        menue.setPrefWidth(210);
        ToggleGroup gruppe = new ToggleGroup();
        for (Seite s : seiten) {
            ToggleButton knopf = new ToggleButton(s.titel());
            knopf.setToggleGroup(gruppe);
            knopf.setMaxWidth(Double.MAX_VALUE);
            knopf.getStyleClass().add("menue-knopf");
            knopf.setOnAction(e -> {
                if (!knopf.isSelected()) {
                    knopf.setSelected(true); // eine Seite bleibt immer gewählt
                }
                zeigeSeite(s);
            });
            menue.getChildren().add(knopf);
        }
        Region platz = new Region();
        VBox.setVgrow(platz, Priority.ALWAYS);
        Label pfad = new Label("Daten:\n" + daten.speicher().getOrdner());
        pfad.getStyleClass().add("menue-klein");
        pfad.setWrapText(true);
        menue.getChildren().addAll(platz, pfad);

        // Kopf und Statuszeile rechts
        seitenTitel.getStyleClass().add("seitentitel");
        untertitel.getStyleClass().add("hinweis");
        VBox kopf = new VBox(2, seitenTitel, untertitel);
        status.getStyleClass().add("status");
        HBox statusLeiste = new HBox(status);
        statusLeiste.setAlignment(Pos.CENTER_LEFT);
        statusLeiste.getStyleClass().add("statusleiste");

        BorderPane inhalt = new BorderPane();
        inhalt.setTop(kopf);
        inhalt.setCenter(seitenBereich);
        inhalt.setBottom(statusLeiste);
        BorderPane.setMargin(kopf, new Insets(0, 0, 14, 0));
        BorderPane.setMargin(statusLeiste, new Insets(10, 0, 0, 0));
        inhalt.setPadding(new Insets(24, 28, 14, 28));
        inhalt.getStyleClass().add("inhalt");

        BorderPane wurzel = new BorderPane();
        wurzel.setLeft(menue);
        wurzel.setCenter(inhalt);

        // Nicht größer als der Bildschirm (bei Skalierung 125 % wäre 1180x740 zu hoch)
        javafx.geometry.Rectangle2D schirm = javafx.stage.Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(wurzel, Math.min(1180, schirm.getWidth() * 0.92), Math.min(740, schirm.getHeight() * 0.9));
        scene.getStylesheets().add(stylesheet());
        stage.setTitle("Stundenbuch");
        stage.setMinWidth(900);
        stage.setMinHeight(540);
        stage.setScene(scene);

        for (Seite s : seiten) {
            seitenBereich.getChildren().add(s.inhalt());
            s.inhalt().setVisible(false);
        }
        ((ToggleButton) menue.getChildren().get(1)).setSelected(true);
        zeigeSeite(seiten.get(0));
        stage.show();

        String warnung = daten.speicher().getLadeWarnung();
        if (warnung != null) {
            Alert a = new Alert(Alert.AlertType.WARNING, warnung, ButtonType.OK);
            a.setHeaderText("Achtung beim Laden");
            a.initOwner(stage);
            a.getDialogPane().getStylesheets().add(stylesheet());
            a.showAndWait();
        }
    }

    private void zeigeSeite(Seite s) {
        for (var n : seitenBereich.getChildren()) {
            n.setVisible(false);
        }
        s.inhalt().setVisible(true);
        s.aktualisieren();
        seitenTitel.setText(s.titel());
        kopfAktualisieren();
    }

    private void kopfAktualisieren() {
        YearMonth jetzt = YearMonth.now();
        untertitel.setText(LocalDate.now().format(Format.TAG) + "  ·  "
                + daten.z().getMitarbeiter().size() + " Mitarbeiter  ·  "
                + daten.z().getProjekte().size() + " Projekte  ·  "
                + daten.z().getEintraege().size() + " Einträge  ·  "
                + jetzt.format(Format.MONAT) + ": " + Format.stunden(daten.z().getStundenGesamt(jetzt)));
    }
}
