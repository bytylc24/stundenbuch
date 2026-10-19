package stundenbuch;

/**
 * Ein Mitarbeiter, der Stunden erfasst.
 */
public class Mitarbeiter {

    /** Vorsilbe der Kennung: AB_01 = Arbeiter 1. */
    public static final String KENNUNG_VORSILBE = "AB_";

    private final int id;
    private String name;

    public Mitarbeiter(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    /** Lesbare Kennung aus der ID, z. B. 1 -> "AB_01", 12 -> "AB_12". */
    public String getKennung() {
        return String.format("%s%02d", KENNUNG_VORSILBE, id);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
