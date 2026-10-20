package stundenbuch;

import java.io.Console;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Aussehen im Terminal: Farben, Rahmen und Tabellen.
 * Benutzt nur Rahmenzeichen, die es auch in der alten Windows-Konsole (Codepage 850) gibt.
 * Farben nur, wenn das Terminal sie sicher kann. Sonst steht der Text einfach ohne Farbe da.
 */
final class Konsole {

    /** true in einem echten Fenster; false, wenn Ein- oder Ausgabe umgeleitet sind (Testläufe, IntelliJ). */
    static final boolean INTERAKTIV = interaktiv();

    private static final boolean FARBE = farbeMoeglich();

    /** Breite von Titelrahmen und Überschriften. */
    static final int BREITE = 64;

    private Konsole() {
    }

    // ---------- Farben ----------
    // Farbcode, überall gleich:
    //   weiß  = normaler Text          grün = erledigt (gespeichert, angelegt, gelöscht)
    //   rot   = Fehler, geht nicht     gelb = Frage (j/n) oder abgebrochen, nichts passiert
    //   cyan  = Überschriften, Menünummern
    //   grau  = Hinweise, Rahmen, Nebeninfos

    static String fett(String text) {
        return farbe("1", text);
    }

    static String gruen(String text) {
        return farbe("32", text);
    }

    static String rot(String text) {
        return farbe("31", text);
    }

    static String gelb(String text) {
        return farbe("33", text);
    }

    static String grau(String text) {
        return farbe("90", text);
    }

    static String akzent(String text) {
        return farbe("1;36", text);
    }

    /** Menünummer wie "1)", in derselben Farbe wie Überschriften. */
    static String taste(String text) {
        return akzent(text);
    }

    /** ANSI-Farbcode um den Text. Erst nach dem Auffüllen mit Leerzeichen aufrufen, sonst stimmen die Breiten nicht. */
    private static String farbe(String code, String text) {
        return FARBE && !text.isEmpty() ? "\u001B[" + code + "m" + text + "\u001B[0m" : text;
    }

    /**
     * Farben nur in einem echten Fenster. NO_COLOR (gesetzt, egal welcher Wert) schaltet sie ab.
     * Unter Windows muss das Fenster die Farbcodes erst verstehen lernen, siehe farbenEinschalten.
     */
    private static boolean farbeMoeglich() {
        if (System.getenv("NO_COLOR") != null || !INTERAKTIV) {
            return false;
        }
        return !istWindows() || farbenEinschalten();
    }

    /**
     * Ein Windows-Fenster zeigt Farbcodes erst an, wenn sie eingeschaltet sind (Windows 10 und neuer).
     * Java kann das nicht selbst, aber cmd.exe schaltet sie beim Start ein und lässt sie an.
     * Darum genügt ein leerer cmd-Aufruf im selben Fenster. Klappt der nicht, gibt es keine Farben.
     */
    private static boolean farbenEinschalten() {
        try {
            return new ProcessBuilder("cmd", "/c", "rem").inheritIO().start().waitFor() == 0;
        } catch (IOException ex) {
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static boolean interaktiv() {
        Console c = System.console();
        return c != null && istTerminal(c);
    }

    private static boolean istWindows() {
        return System.getProperty("os.name", "").startsWith("Windows");
    }

    /**
     * Leert das Fenster, damit jeder Bildschirm oben beginnt. Unter Windows mit "cls",
     * das klappt auch in der alten Konsole. Bei Testläufen passiert nichts.
     */
    static void leeren() {
        if (!INTERAKTIV) {
            return;
        }
        try {
            if (istWindows()) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\u001B[H\u001B[2J");
                System.out.flush();
            }
        } catch (IOException ex) {
            // Dann bleibt der alte Text eben stehen
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /** Ab Java 22 gibt es Console.isTerminal(); vorher ist System.console() bei einer Pipe ohnehin null. */
    private static boolean istTerminal(Console c) {
        try {
            return (Boolean) Console.class.getMethod("isTerminal").invoke(c);
        } catch (ReflectiveOperationException ex) {
            return true;
        }
    }

    // ---------- Rahmen und Überschriften ----------

    /** Doppelter Rahmen mit Titel links und kleinem Text rechts, z. B. dem Datum. */
    static List<String> titelBox(String titel, String rechts) {
        int innen = BREITE - 2;
        String abstand = " ".repeat(Math.max(1, innen - 4 - titel.length() - rechts.length()));
        return List.of(
                grau("╔" + "═".repeat(innen) + "╗"),
                grau("║") + "  " + akzent(titel) + abstand + grau(rechts) + "  " + grau("║"),
                grau("╚" + "═".repeat(innen) + "╝"));
    }

    /** Überschrift mit Linie: "── Mitarbeiter ─────────". */
    static String ueberschrift(String text) {
        return grau("── ") + akzent(text) + " " + grau("─".repeat(Math.max(3, BREITE - text.length() - 4)));
    }

    /** Kleine Kästchen nebeneinander, oben die Zahl, darunter die Bezeichnung. */
    static List<String> kacheln(List<String[]> kennzahlen) {
        int w = 14;
        for (String[] k : kennzahlen) {
            w = Math.max(w, Math.max(k[0].length(), k[1].length()));
        }
        StringBuilder oben = new StringBuilder("┌");
        StringBuilder zahl = new StringBuilder();
        StringBuilder text = new StringBuilder();
        StringBuilder unten = new StringBuilder("└");
        for (int i = 0; i < kennzahlen.size(); i++) {
            String trenner = i < kennzahlen.size() - 1 ? "┬" : "┐";
            oben.append("─".repeat(w + 2)).append(trenner);
            unten.append("─".repeat(w + 2)).append(trenner.equals("┬") ? "┴" : "┘");
            zahl.append(grau("│")).append(" ").append(fett(links(kennzahlen.get(i)[0], w))).append(" ");
            text.append(grau("│")).append(" ").append(grau(links(kennzahlen.get(i)[1], w))).append(" ");
        }
        zahl.append(grau("│"));
        text.append(grau("│"));
        return List.of(grau(oben.toString()), zahl.toString(), text.toString(), grau(unten.toString()));
    }

    // ---------- Tabellen ----------

    /**
     * Zeichnet eine Tabelle mit Rahmen. Datenzeilen stehen ohne Trennlinie untereinander,
     * Überschriften und Summen bekommen eine Linie. Lange Texte werden umgebrochen.
     */
    static List<String> tabelle(Tabelle t) {
        List<Tabelle.Spalte> spalten = t.getSpalten();
        int n = spalten.size();

        // 1) Spaltenbreiten: längster Inhalt, höchstens maxBreite
        int[] w = new int[n];
        for (int i = 0; i < n; i++) {
            w[i] = spalten.get(i).name().length();
        }
        for (Tabelle.Zeile z : t.getZeilen()) {
            for (int i = z.art() == Tabelle.Art.NORMAL ? 0 : t.spanne(z); i < n; i++) {
                w[i] = Math.max(w[i], z.zellen().get(i).length());
            }
        }
        for (int i = 0; i < n; i++) {
            int max = spalten.get(i).maxBreite();
            if (max > 0) {
                w[i] = Math.max(spalten.get(i).name().length(), Math.min(w[i], max));
            }
        }
        // Beschriftungen über mehrere Spalten verbreitern die Tabelle nur, wenn sie sonst nicht passen
        for (Tabelle.Zeile z : t.getZeilen()) {
            if (z.art() != Tabelle.Art.NORMAL) {
                int s = t.spanne(z);
                int fehlt = z.zellen().get(0).length() - breite(w, 0, s);
                if (fehlt > 0) {
                    w[s - 1] += fehlt;
                }
            }
        }

        // 2) Zeichnen
        List<String> aus = new ArrayList<>();
        int[] alle = grenzen(n, 1);
        aus.add(grau(linie(w, new int[0], alle, '┌', '┐')));
        List<String> kopf = new ArrayList<>();
        for (Tabelle.Spalte s : spalten) {
            kopf.add(s.name());
        }
        aus.addAll(zeichneZeile(t, new Tabelle.Zeile(Tabelle.Art.NORMAL, kopf), w, true));

        int[] vorher = alle;
        boolean vorherNormal = false; // nach dem Kopf immer eine Linie
        for (Tabelle.Zeile z : t.getZeilen()) {
            int[] jetzt = grenzen(n, t.spanne(z));
            boolean normal = z.art() == Tabelle.Art.NORMAL;
            if (!normal || !vorherNormal) {
                aus.add(grau(linie(w, vorher, jetzt, '├', '┤')));
            }
            aus.addAll(zeichneZeile(t, z, w, false));
            vorher = jetzt;
            vorherNormal = normal;
        }
        aus.add(grau(linie(w, vorher, new int[0], '└', '┘')));
        return aus;
    }

    /** Spalten, vor denen ein senkrechter Strich steht: ab der Spanne der ersten Zelle. */
    private static int[] grenzen(int n, int spanne) {
        int[] g = new int[Math.max(0, n - spanne)];
        for (int i = 0; i < g.length; i++) {
            g[i] = spanne + i;
        }
        return g;
    }

    /** Waagrechte Linie; an jeder Spaltengrenze passt das Zeichen zu den Strichen darüber und darunter. */
    private static String linie(int[] w, int[] oben, int[] unten, char links, char rechts) {
        StringBuilder sb = new StringBuilder().append(links);
        for (int i = 0; i < w.length; i++) {
            if (i > 0) {
                boolean o = enthaelt(oben, i);
                boolean u = enthaelt(unten, i);
                sb.append(o && u ? '┼' : o ? '┴' : u ? '┬' : '─');
            }
            sb.append("─".repeat(w[i] + 2));
        }
        return sb.append(rechts).toString();
    }

    private static boolean enthaelt(int[] liste, int wert) {
        for (int x : liste) {
            if (x == wert) {
                return true;
            }
        }
        return false;
    }

    /** Eine Tabellenzeile, bei langen Texten auf mehrere Bildschirmzeilen verteilt. */
    private static List<String> zeichneZeile(Tabelle t, Tabelle.Zeile z, int[] w, boolean kopf) {
        int n = w.length;
        int s = t.spanne(z);

        // Abschnitte zwischen den senkrechten Strichen: Text, Breite, rechtsbündig?
        List<String> texte = new ArrayList<>();
        List<Integer> breiten = new ArrayList<>();
        List<Boolean> rechts = new ArrayList<>();
        texte.add(z.zellen().get(0));
        breiten.add(breite(w, 0, s));
        rechts.add(z.art() == Tabelle.Art.NORMAL ? t.getSpalten().get(0).rechts() && !kopf
                : z.art() != Tabelle.Art.GRUPPE && s > 1);
        for (int i = s; i < n; i++) {
            texte.add(z.zellen().get(i));
            breiten.add(w[i]);
            rechts.add(t.getSpalten().get(i).rechts());
        }

        List<List<String>> umbrochen = new ArrayList<>();
        int hoehe = 1;
        for (int i = 0; i < texte.size(); i++) {
            umbrochen.add(umbrechen(texte.get(i), breiten.get(i)));
            hoehe = Math.max(hoehe, umbrochen.get(i).size());
        }

        List<String> aus = new ArrayList<>();
        for (int zeile = 0; zeile < hoehe; zeile++) {
            StringBuilder sb = new StringBuilder(grau("│"));
            for (int i = 0; i < texte.size(); i++) {
                List<String> teile = umbrochen.get(i);
                String text = zeile < teile.size() ? teile.get(zeile) : "";
                text = rechts.get(i) ? rechts(text, breiten.get(i)) : links(text, breiten.get(i));
                if (kopf || z.art() == Tabelle.Art.SUMME || z.art() == Tabelle.Art.ZWISCHENSUMME) {
                    text = fett(text);
                } else if (z.art() == Tabelle.Art.GRUPPE) {
                    text = akzent(text);
                }
                sb.append(" ").append(text).append(" ").append(grau("│"));
            }
            aus.add(sb.toString());
        }
        return aus;
    }

    /** Breite der Spalten von bis (ohne bis) samt den Trennern dazwischen. */
    private static int breite(int[] w, int von, int bis) {
        int summe = 0;
        for (int i = von; i < bis; i++) {
            summe += w[i];
        }
        return summe + 3 * (bis - von - 1);
    }

    /** Bricht an Leerzeichen um. Zu lange Wörter werden hart getrennt. */
    static List<String> umbrechen(String text, int breite) {
        List<String> zeilen = new ArrayList<>();
        StringBuilder aktuell = new StringBuilder();
        for (String wort : text.trim().split("\\s+")) {
            while (wort.length() > breite) {
                if (aktuell.length() > 0) {
                    zeilen.add(aktuell.toString());
                    aktuell.setLength(0);
                }
                zeilen.add(wort.substring(0, breite));
                wort = wort.substring(breite);
            }
            if (aktuell.length() > 0 && aktuell.length() + 1 + wort.length() > breite) {
                zeilen.add(aktuell.toString());
                aktuell.setLength(0);
            }
            if (aktuell.length() > 0) {
                aktuell.append(' ');
            }
            aktuell.append(wort);
        }
        zeilen.add(aktuell.toString());
        return zeilen;
    }

    static String links(String text, int breite) {
        return text + " ".repeat(Math.max(0, breite - text.length()));
    }

    static String rechts(String text, int breite) {
        return " ".repeat(Math.max(0, breite - text.length())) + text;
    }
}
