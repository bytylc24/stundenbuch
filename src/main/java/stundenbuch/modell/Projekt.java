package stundenbuch.modell;

/**
 * Ein Projekt, bestehend aus Ort und Kunde, z. B. Graz/Mustermann.
 */
public class Projekt {

    private final int id;
    private String ort;
    private String kunde;

    public Projekt(int id, String ort, String kunde) {
        this.id = id;
        this.ort = ort;
        this.kunde = kunde;
    }

    public int getId() {
        return id;
    }

    public String getOrt() {
        return ort;
    }

    public void setOrt(String ort) {
        this.ort = ort;
    }

    public String getKunde() {
        return kunde;
    }

    public void setKunde(String kunde) {
        this.kunde = kunde;
    }

    /** Anzeigename aus Ort und Kunde, z. B. "Graz/Mustermann". */
    public String getName() {
        return ort + "/" + kunde;
    }

    @Override
    public String toString() {
        return getName();
    }
}
