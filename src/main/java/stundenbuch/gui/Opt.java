package stundenbuch.gui;

/** Ein Eintrag in einer Auswahlliste: der Wert (kann null sein, z. B. "Alle Projekte") und sein Text. */
record Opt<T>(T wert, String text) {

    @Override
    public String toString() {
        return text;
    }
}
