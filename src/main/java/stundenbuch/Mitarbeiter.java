package stundenbuch;

/**
 * Ein Mitarbeiter, der Stunden erfasst.
 */
public class Mitarbeiter {

    private final int id;
    private String name;

    public Mitarbeiter(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
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
