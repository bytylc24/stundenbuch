package stundenbuch.bericht;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import stundenbuch.anzeige.Format;

/**
 * Schreibt einen Bericht als Word-Datei (.docx) oder als CSV für Excel.
 * Ohne zusätzliche Bibliothek: Eine .docx ist eine ZIP-Datei mit ein paar XML-Dateien darin.
 * Redet nicht mit dem Benutzer: Fehler werden als Exception weitergegeben.
 */
public final class BerichtExport {

    // Farben im Word-Bericht (Hex wie in HTML, ohne #)
    private static final String DUNKELBLAU = "1F4E79";
    private static final String HELLBLAU = "DDEBF7";
    private static final String SUMMENBLAU = "D9E2F3";
    private static final String ZEBRA = "F3F6FA";
    private static final String GRAU = "595959";

    // A4 hoch mit 2 cm Rand; Word misst in Twips (1/1440 Zoll), 2 cm = 1134
    private static final int RAND = 1134;
    private static final int TEXTBREITE = 11906 - 2 * RAND;

    private BerichtExport() {
    }

    // ---------- CSV ----------

    /**
     * CSV mit ; und BOM, damit Excel die Umlaute erkennt. Aufbau wie im Terminal:
     * Kopfangaben, Kennzahlen, dann jede Tabelle mit Überschrift, getrennt durch Leerzeilen.
     */
    public static void alsCsv(Bericht b, Path datei) throws IOException {
        StringBuilder sb = new StringBuilder("﻿");
        csvZeile(sb, b.getTitel());
        sb.append("\r\n");
        for (String[] a : b.getAngaben()) {
            csvZeile(sb, a[0], a[1]);
        }
        for (String[] k : b.getKennzahlen()) {
            csvZeile(sb, k[1], k[0]);
        }
        for (Tabelle t : b.getTabellen()) {
            sb.append("\r\n");
            csvZeile(sb, t.getTitel());
            csvZeile(sb, t.getSpalten().stream().map(Tabelle.Spalte::name).toArray(String[]::new));
            for (Tabelle.Zeile z : t.getZeilen()) {
                csvZeile(sb, z.zellen().toArray(String[]::new));
            }
        }
        Files.writeString(datei, sb.toString(), StandardCharsets.UTF_8);
    }

    private static void csvZeile(StringBuilder sb, String... zellen) {
        for (int i = 0; i < zellen.length; i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(csvZelle(zellen[i]));
        }
        sb.append("\r\n");
    }

    /** Setzt Anführungszeichen, wenn nötig. Text mit = + - @ am Anfang hielte Excel für eine Formel. */
    private static String csvZelle(String text) {
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) {
            text = " " + text;
        }
        if (text.contains(";") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    // ---------- Word (.docx) ----------

    public static void alsDocx(Bericht b, Path datei) throws IOException {
        try (OutputStream out = Files.newOutputStream(datei);
             ZipOutputStream zip = new ZipOutputStream(out)) {
            eintrag(zip, "[Content_Types].xml", INHALTSTYPEN);
            eintrag(zip, "_rels/.rels", PAKET_VERWEISE);
            eintrag(zip, "docProps/core.xml", eigenschaften(b));
            eintrag(zip, "word/_rels/document.xml.rels", DOKUMENT_VERWEISE);
            eintrag(zip, "word/styles.xml", FORMATVORLAGEN);
            eintrag(zip, "word/footer1.xml", FUSSZEILE);
            eintrag(zip, "word/document.xml", dokument(b));
        }
    }

    private static void eintrag(ZipOutputStream zip, String name, String inhalt) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(inhalt.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    /** Der eigentliche Inhalt: Titel, Angaben, Kennzahlen und die Tabellen. */
    private static String dokument(Bericht b) {
        StringBuilder sb = new StringBuilder(XML_KOPF)
                .append("<w:document xmlns:w=\"").append(W).append("\" xmlns:r=\"").append(R).append("\"><w:body>");

        sb.append(absatz("<w:pStyle w:val=\"Title\"/>", lauf(b.getTitel(), "")));

        // Angaben als randlose Tabelle mit zwei Spalten
        int links = 2000;
        sb.append(tabellenAnfang(new int[]{links, TEXTBREITE - links}, "<w:tblBorders>" + ohneRahmen() + "</w:tblBorders>"));
        for (String[] a : b.getAngaben()) {
            sb.append("<w:tr>")
                    .append(zelle(links, 1, null, null, false, false, lauf(a[0], "<w:color w:val=\"" + GRAU + "\"/>")))
                    .append(zelle(TEXTBREITE - links, 1, null, null, false, false, lauf(a[1], "<w:b/>")))
                    .append("</w:tr>");
        }
        sb.append("</w:tbl>").append(abstand(200));

        // Kennzahlen als hellblaue Kacheln, die weißen Striche dazwischen wirken als Abstand
        List<String[]> kennzahlen = b.getKennzahlen();
        int[] kachel = verteile(TEXTBREITE, kennzahlen.size());
        sb.append(tabellenAnfang(kachel, "<w:tblBorders>" + ohneRahmen().replace(
                "<w:insideV w:val=\"nil\"/>", "<w:insideV w:val=\"single\" w:sz=\"48\" w:color=\"FFFFFF\"/>")
                + "</w:tblBorders>"));
        sb.append("<w:tr>");
        for (int i = 0; i < kennzahlen.size(); i++) {
            String inhalt = lauf(kennzahlen.get(i)[0], "<w:b/><w:color w:val=\"" + DUNKELBLAU + "\"/><w:sz w:val=\"36\"/>")
                    + "</w:p><w:p>" + lauf(kennzahlen.get(i)[1], "<w:color w:val=\"" + GRAU + "\"/><w:sz w:val=\"18\"/>");
            sb.append(zelle(kachel[i], 1, HELLBLAU, null, false, false, inhalt));
        }
        sb.append("</w:tr></w:tbl>");

        for (Tabelle t : b.getTabellen()) {
            sb.append(absatz("<w:pStyle w:val=\"Heading1\"/>", lauf(t.getTitel(), "")));
            sb.append(tabelle(t));
        }

        // Word erwartet nach einer Tabelle am Ende noch einen Absatz
        sb.append("<w:p/><w:sectPr><w:footerReference w:type=\"default\" r:id=\"rId2\"/>")
                .append("<w:pgSz w:w=\"11906\" w:h=\"16838\"/>")
                .append("<w:pgMar w:top=\"").append(RAND).append("\" w:right=\"").append(RAND)
                .append("\" w:bottom=\"").append(RAND).append("\" w:left=\"").append(RAND)
                .append("\" w:header=\"567\" w:footer=\"567\" w:gutter=\"0\"/>")
                .append("</w:sectPr></w:body></w:document>");
        return sb.toString();
    }

    /**
     * Eine Tabelle wie im Terminal: dunkelblauer Kopf (wiederholt sich auf jeder Seite),
     * Datenzeilen abwechselnd hinterlegt, Monate hellblau, Summen fett mit Linie darüber.
     */
    private static String tabelle(Tabelle t) {
        List<Tabelle.Spalte> spalten = t.getSpalten();
        int n = spalten.size();
        int[] w = spaltenBreiten(t);

        StringBuilder sb = new StringBuilder(tabellenAnfang(w, "<w:tblBorders>"
                + "<w:top w:val=\"single\" w:sz=\"4\" w:color=\"BFBFBF\"/><w:left w:val=\"nil\"/>"
                + "<w:bottom w:val=\"single\" w:sz=\"4\" w:color=\"BFBFBF\"/><w:right w:val=\"nil\"/>"
                + "<w:insideH w:val=\"single\" w:sz=\"4\" w:color=\"E4E4E4\"/><w:insideV w:val=\"nil\"/>"
                + "</w:tblBorders>"));

        sb.append("<w:tr><w:trPr><w:cantSplit/><w:tblHeader/></w:trPr>");
        for (int i = 0; i < n; i++) {
            sb.append(zelle(w[i], 1, DUNKELBLAU, null, spalten.get(i).rechts(), true,
                    lauf(spalten.get(i).name(), "<w:b/><w:color w:val=\"FFFFFF\"/>")));
        }
        sb.append("</w:tr>");

        // Kleine Tabellen bleiben ganz beisammen, große zumindest am Ende:
        // so steht die Summe nie allein auf einer neuen Seite
        List<Tabelle.Zeile> zeilen = t.getZeilen();
        int nr = 0; // für die abwechselnde Hinterlegung, beginnt pro Monat neu
        for (int zeile = 0; zeile < zeilen.size(); zeile++) {
            Tabelle.Zeile z = zeilen.get(zeile);
            boolean zusammen = zeile < zeilen.size() - 1 && (zeilen.size() <= 15 || zeile >= zeilen.size() - 3);
            sb.append("<w:tr><w:trPr><w:cantSplit/></w:trPr>");
            int s = t.spanne(z);
            int spannBreite = 0;
            for (int i = 0; i < s; i++) {
                spannBreite += w[i];
            }
            switch (z.art()) {
                case NORMAL -> {
                    String fuellung = nr++ % 2 == 1 ? ZEBRA : null;
                    for (int i = 0; i < n; i++) {
                        sb.append(zelle(w[i], 1, fuellung, null, spalten.get(i).rechts(), zusammen,
                                lauf(z.zellen().get(i), "")));
                    }
                }
                case GRUPPE -> {
                    nr = 0;
                    // Eine Monatsüberschrift nie allein am Seitenende
                    sb.append(zelle(spannBreite, s, HELLBLAU, null, false, true,
                            lauf(z.zellen().get(0), "<w:b/><w:color w:val=\"" + DUNKELBLAU + "\"/>")));
                }
                case ZWISCHENSUMME, SUMME -> {
                    boolean gesamt = z.art() == Tabelle.Art.SUMME;
                    String fuellung = gesamt ? SUMMENBLAU : null;
                    String linie = gesamt
                            ? "<w:top w:val=\"double\" w:sz=\"6\" w:color=\"" + DUNKELBLAU + "\"/>"
                            : "<w:top w:val=\"single\" w:sz=\"6\" w:color=\"" + DUNKELBLAU + "\"/>";
                    sb.append(zelle(spannBreite, s, fuellung, linie, s > 1, zusammen, lauf(z.zellen().get(0), "<w:b/>")));
                    for (int i = s; i < n; i++) {
                        sb.append(zelle(w[i], 1, fuellung, linie, spalten.get(i).rechts(), zusammen,
                                lauf(z.zellen().get(i), "<w:b/>")));
                    }
                }
            }
            sb.append("</w:tr>");
        }
        return sb.append("</w:tbl>").toString();
    }

    /**
     * Spaltenbreiten: Jede Spalte bekommt so viel Platz, wie ihr längster Inhalt braucht.
     * Den Rest der Seitenbreite bekommt die Spalte mit Umbruch (Beschreibung), sonst die erste.
     * Passt es nicht, wird alles im selben Verhältnis kleiner.
     */
    private static int[] spaltenBreiten(Tabelle t) {
        List<Tabelle.Spalte> spalten = t.getSpalten();
        int n = spalten.size();
        int[] zeichen = new int[n];
        for (int i = 0; i < n; i++) {
            zeichen[i] = spalten.get(i).name().length();
        }
        for (Tabelle.Zeile z : t.getZeilen()) {
            if (z.art() == Tabelle.Art.NORMAL) {
                for (int i = 0; i < n; i++) {
                    zeichen[i] = Math.max(zeichen[i], z.zellen().get(i).length());
                }
            }
        }

        int dehnbar = 0;
        for (int i = 0; i < n; i++) {
            if (spalten.get(i).maxBreite() > 0) {
                dehnbar = i;
            }
        }
        int[] w = new int[n];
        int summe = 0;
        for (int i = 0; i < n; i++) {
            // Calibri 10 pt: im Schnitt etwa 110 Twips pro Zeichen, dazu 300 für den Innenabstand
            w[i] = i == dehnbar ? 1500 : Math.min(zeichen[i], 35) * 110 + 300;
            summe += w[i];
        }
        if (summe <= TEXTBREITE) {
            w[dehnbar] += TEXTBREITE - summe;
            return w;
        }
        int rest = TEXTBREITE;
        for (int i = 0; i < n - 1; i++) {
            w[i] = w[i] * TEXTBREITE / summe;
            rest -= w[i];
        }
        w[n - 1] = rest;
        return w;
    }

    private static int[] verteile(int gesamt, int teile) {
        int[] w = new int[teile];
        for (int i = 0; i < teile; i++) {
            w[i] = gesamt / teile + (i < gesamt % teile ? 1 : 0);
        }
        return w;
    }

    // ---------- XML-Bausteine ----------
    // Word ist bei der Reihenfolge der Elemente streng: tcW, gridSpan, tcBorders, shd, vAlign usw.

    private static String tabellenAnfang(int[] spalten, String rahmen) {
        int gesamt = 0;
        StringBuilder gitter = new StringBuilder("<w:tblGrid>");
        for (int s : spalten) {
            gesamt += s;
            gitter.append("<w:gridCol w:w=\"").append(s).append("\"/>");
        }
        return "<w:tbl><w:tblPr><w:tblW w:w=\"" + gesamt + "\" w:type=\"dxa\"/>" + rahmen
                + "<w:tblLayout w:type=\"fixed\"/>"
                + "<w:tblCellMar><w:top w:w=\"50\" w:type=\"dxa\"/><w:left w:w=\"100\" w:type=\"dxa\"/>"
                + "<w:bottom w:w=\"50\" w:type=\"dxa\"/><w:right w:w=\"100\" w:type=\"dxa\"/></w:tblCellMar>"
                + "</w:tblPr>" + gitter + "</w:tblGrid>";
    }

    private static String ohneRahmen() {
        return "<w:top w:val=\"nil\"/><w:left w:val=\"nil\"/><w:bottom w:val=\"nil\"/><w:right w:val=\"nil\"/>"
                + "<w:insideH w:val=\"nil\"/><w:insideV w:val=\"nil\"/>";
    }

    /**
     * Eine Zelle. fuellung und obenLinie dürfen null sein, inhalt sind fertige Läufe (w:r).
     * zusammen: Word hält die Zeile mit der nächsten auf einer Seite.
     */
    private static String zelle(int breite, int spanne, String fuellung, String obenLinie, boolean rechts,
                                boolean zusammen, String inhalt) {
        StringBuilder sb = new StringBuilder("<w:tc><w:tcPr><w:tcW w:w=\"").append(breite).append("\" w:type=\"dxa\"/>");
        if (spanne > 1) {
            sb.append("<w:gridSpan w:val=\"").append(spanne).append("\"/>");
        }
        if (obenLinie != null) {
            sb.append("<w:tcBorders>").append(obenLinie).append("</w:tcBorders>");
        }
        if (fuellung != null) {
            sb.append("<w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"").append(fuellung).append("\"/>");
        }
        sb.append("<w:vAlign w:val=\"center\"/></w:tcPr>");
        // Reihenfolge wie von Word verlangt: keepNext vor jc
        sb.append(absatz((zusammen ? "<w:keepNext/>" : "") + (rechts ? "<w:jc w:val=\"right\"/>" : ""), inhalt));
        return sb.append("</w:tc>").toString();
    }

    private static String absatz(String eigenschaften, String inhalt) {
        return "<w:p>" + (eigenschaften.isEmpty() ? "" : "<w:pPr>" + eigenschaften + "</w:pPr>") + inhalt + "</w:p>";
    }

    /** Ein Stück Text mit Format (rPr-Inhalt in der Reihenfolge b, color, sz). */
    private static String lauf(String text, String format) {
        return "<w:r>" + (format.isEmpty() ? "" : "<w:rPr>" + format + "</w:rPr>")
                + "<w:t xml:space=\"preserve\">" + xml(text) + "</w:t></w:r>";
    }

    /** Leerer Absatz als Abstand (Höhe in Twips). */
    private static String abstand(int hoehe) {
        return "<w:p><w:pPr><w:spacing w:before=\"0\" w:after=\"0\" w:line=\"" + hoehe
                + "\" w:lineRule=\"exact\"/></w:pPr></w:p>";
    }

    /** Ersetzt die Zeichen, die in XML eine Bedeutung haben; Steuerzeichen sind in XML verboten. */
    private static String xml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
    }

    private static String eigenschaften(Bericht b) {
        String jetzt = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        return XML_KOPF + "<cp:coreProperties"
                + " xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\""
                + " xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<dc:title>" + xml(b.getTitel()) + "</dc:title><dc:creator>Stundenbuch</dc:creator>"
                + "<dcterms:created xsi:type=\"dcterms:W3CDTF\">" + jetzt + "</dcterms:created>"
                + "</cp:coreProperties>";
    }

    // ---------- Feste Teile der .docx ----------

    private static final String XML_KOPF = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
    private static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final String R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String TYP = "application/vnd.openxmlformats-officedocument.wordprocessingml.";

    /** Welche Datei im ZIP welchen Inhalt hat. */
    private static final String INHALTSTYPEN = XML_KOPF
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Override PartName=\"/word/document.xml\" ContentType=\"" + TYP + "document.main+xml\"/>"
            + "<Override PartName=\"/word/styles.xml\" ContentType=\"" + TYP + "styles+xml\"/>"
            + "<Override PartName=\"/word/footer1.xml\" ContentType=\"" + TYP + "footer+xml\"/>"
            + "<Override PartName=\"/docProps/core.xml\""
            + " ContentType=\"application/vnd.openxmlformats-package.core-properties+xml\"/>"
            + "</Types>";

    private static final String PAKET_VERWEISE = XML_KOPF
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"" + R + "/officeDocument\" Target=\"word/document.xml\"/>"
            + "<Relationship Id=\"rId2\""
            + " Type=\"http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties\""
            + " Target=\"docProps/core.xml\"/>"
            + "</Relationships>";

    private static final String DOKUMENT_VERWEISE = XML_KOPF
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"" + R + "/styles\" Target=\"styles.xml\"/>"
            + "<Relationship Id=\"rId2\" Type=\"" + R + "/footer\" Target=\"footer1.xml\"/>"
            + "</Relationships>";

    /** Schrift Calibri 10 pt, dazu Titel und Überschrift 1 (erscheinen so auch im Navigationsbereich von Word). */
    private static final String FORMATVORLAGEN = XML_KOPF
            + "<w:styles xmlns:w=\"" + W + "\">"
            + "<w:docDefaults><w:rPrDefault><w:rPr>"
            + "<w:rFonts w:ascii=\"Calibri\" w:hAnsi=\"Calibri\" w:eastAsia=\"Calibri\" w:cs=\"Calibri\"/>"
            + "<w:sz w:val=\"20\"/><w:szCs w:val=\"20\"/><w:lang w:val=\"de-AT\"/>"
            + "</w:rPr></w:rPrDefault>"
            + "<w:pPrDefault><w:pPr><w:spacing w:after=\"0\" w:line=\"264\" w:lineRule=\"auto\"/></w:pPr></w:pPrDefault>"
            + "</w:docDefaults>"
            + "<w:style w:type=\"paragraph\" w:default=\"1\" w:styleId=\"Normal\"><w:name w:val=\"Normal\"/><w:qFormat/></w:style>"
            + "<w:style w:type=\"paragraph\" w:styleId=\"Title\"><w:name w:val=\"Title\"/><w:basedOn w:val=\"Normal\"/>"
            + "<w:next w:val=\"Normal\"/><w:qFormat/>"
            + "<w:pPr><w:pBdr><w:bottom w:val=\"single\" w:sz=\"12\" w:space=\"6\" w:color=\"" + DUNKELBLAU + "\"/></w:pBdr>"
            + "<w:spacing w:after=\"240\"/></w:pPr>"
            + "<w:rPr><w:b/><w:color w:val=\"" + DUNKELBLAU + "\"/><w:sz w:val=\"44\"/><w:szCs w:val=\"44\"/></w:rPr></w:style>"
            + "<w:style w:type=\"paragraph\" w:styleId=\"Heading1\"><w:name w:val=\"heading 1\"/><w:basedOn w:val=\"Normal\"/>"
            + "<w:next w:val=\"Normal\"/><w:qFormat/>"
            + "<w:pPr><w:keepNext/><w:spacing w:before=\"360\" w:after=\"120\"/><w:outlineLvl w:val=\"0\"/></w:pPr>"
            + "<w:rPr><w:b/><w:color w:val=\"" + DUNKELBLAU + "\"/><w:sz w:val=\"26\"/><w:szCs w:val=\"26\"/></w:rPr></w:style>"
            + "<w:style w:type=\"paragraph\" w:styleId=\"Footer\"><w:name w:val=\"footer\"/><w:basedOn w:val=\"Normal\"/>"
            + "<w:pPr><w:jc w:val=\"center\"/></w:pPr>"
            + "<w:rPr><w:color w:val=\"808080\"/><w:sz w:val=\"16\"/><w:szCs w:val=\"16\"/></w:rPr></w:style>"
            + "</w:styles>";

    /** Fußzeile mit Seitenzahl, z. B. "Stundenbuch · Seite 1 von 2". Aussehen kommt aus der Vorlage Footer. */
    private static final String FUSSZEILE = XML_KOPF
            + "<w:ftr xmlns:w=\"" + W + "\"><w:p><w:pPr><w:pStyle w:val=\"Footer\"/></w:pPr>"
            + "<w:r><w:t xml:space=\"preserve\">Stundenbuch · Seite </w:t></w:r>"
            + "<w:fldSimple w:instr=\" PAGE \"><w:r><w:t>1</w:t></w:r></w:fldSimple>"
            + "<w:r><w:t xml:space=\"preserve\"> von </w:t></w:r>"
            + "<w:fldSimple w:instr=\" NUMPAGES \"><w:r><w:t>1</w:t></w:r></w:fldSimple>"
            + "</w:p></w:ftr>";
}
