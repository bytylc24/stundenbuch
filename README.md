# Stundenbuch

Das Stundenbuch ist ein kleines Programm zur Zeiterfassung im Team. Du trägst ein,
**wer wann an welchem Projekt wie viele Stunden** gearbeitet hat und was gemacht wurde.
Das Programm zählt die Stunden pro Projekt und pro Mitarbeiter zusammen und
erstellt auf Wunsch einen Bericht als Word- oder Excel-Datei.

**Inhalt**

- [Teil 1: App benutzen](#teil-1-app-benutzen) (für alle im Team)
- [Teil 2: Am Code arbeiten](#teil-2-am-code-arbeiten) (nur für Entwickler)

---

# Teil 1: App benutzen

Du brauchst nur die Datei `Stundenbuch.zip`. Java musst du **nicht** installieren.

## 1. Installieren

1. `Stundenbuch.zip` auf den Desktop kopieren.
2. Rechtsklick auf die ZIP, dann **„Alle extrahieren…“**. Als Ziel den Desktop wählen,
   z. B. `C:\Users\DeinName\Desktop`.
3. Danach gibt es den Ordner `Desktop\Stundenbuch` und darin `Stundenbuch.exe`.
4. `Stundenbuch.exe` doppelklicken.
   - Beim ersten Start warnt Windows: *„Der Computer wurde durch Windows geschützt“*.
     Das ist normal, weil die App nicht signiert ist. Klicke auf **„Weitere Informationen“**
     und dann auf **„Trotzdem ausführen“**.
5. Tipp: Rechtsklick auf `Stundenbuch.exe`, dann *„Weitere Optionen anzeigen“* und
   *„Senden an“* und *„Desktop (Verknüpfung erstellen)“*. So startest du die App künftig direkt vom Desktop.

> Die `.exe` funktioniert nur zusammen mit den anderen Dateien im Ordner `Stundenbuch`.
> Verschiebe sie nicht allein woandershin, sondern immer den ganzen Ordner.

## 2. Erste Schritte

Beim ersten Start ist alles leer. Die Reihenfolge:

1. **Mitarbeiter anlegen:** Menüpunkt `6`, dann `1) Hinzufügen`
2. **Projekte anlegen:** Menüpunkt `7`, dann `1) Hinzufügen`. Ein Projekt besteht aus **Ort** und **Kunde**, z. B. `Graz/Mustermann`
3. **Stunden eintragen:** Menüpunkt `1`

Bedient wird alles mit Zahlen: Zahl eintippen, dann Enter.

| Nr. | Menüpunkt | Was passiert |
|---|---|---|
| 1 | Eintrag erfassen | Datum (leer = heute), Mitarbeiter, Projekt, Stunden (z. B. `4,5`) und Beschreibung |
| 2 | Arbeitstage anzeigen | alle Einträge nach Tag, neuester zuerst, mit Tagessumme |
| 3 | Eintrag bearbeiten | Eintrag über seine `#`-Nummer wählen; leere Eingabe behält den alten Wert |
| 4 | Eintrag löschen | Eintrag über seine `#`-Nummer wählen und mit `j` bestätigen |
| 5 | Auswertung | Tabelle Projekte × Mitarbeiter, gesamt oder für einen Monat |
| 9 | Bericht ansehen und speichern | Bericht als Word oder CSV, siehe unten |
| 6 | Mitarbeiter verwalten | hinzufügen, umbenennen, löschen |
| 7 | Projekte verwalten | hinzufügen, umbenennen, löschen |
| 8 | Hilfe | erklärt alles direkt im Programm |
| 0 | Speichern und beenden | |

Gut zu wissen:

- **Jede Änderung wird sofort gespeichert.** Du kannst das Fenster auch einfach schließen.
- In Auswahllisten heißt `0` immer *abbrechen* bzw. *behalten*.
- Doppelte Namen sind nicht erlaubt (Groß-/Kleinschreibung egal).
- Wer noch Einträge hat, kann nicht gelöscht werden. Lösche zuerst die Einträge.
- Umbenennen wirkt auch auf alle alten Einträge.
- Jeder Mitarbeiter bekommt eine feste Kennung: `AB_01` = Arbeiter 1, `AB_02` = Arbeiter 2 usw.
- Gelöschte Nummern werden nicht neu vergeben. Ausnahme: Sind **alle** Mitarbeiter gelöscht, beginnt
  die Kennung wieder bei `AB_01`. Genauso bei Projekten (wieder Nr. 1) und Einträgen (wieder `#1`).

## 3. Bericht als Word oder Excel speichern

Menüpunkt `9`:

1. **Filter wählen**
   - Zeitraum: gesamter Zeitraum, ein Jahr, ein Monat oder von bis
   - Projekt: ein bestimmtes oder `0` = alle
   - Mitarbeiter: ein bestimmter oder `0` = alle
2. **Vorschau ansehen.** Der Bericht erscheint zuerst im Fenster.
3. **Speichern**, wenn er passt: `1` Word (`.docx`), `2` CSV (für Excel), `3` beides.
   Passt er nicht, mit `4` den Filter ändern.

Die Datei sieht aus wie die Vorschau: Kopfangaben, Gesamtstunden, alle Einträge
(bei mehreren Monaten mit Zwischensummen) und Summen pro Mitarbeiter und Projekt.

**Wo landet die Datei?** In `Desktop\Stundenbuch-Daten\Berichte`.
Danach fragt das Programm, ob es die Datei gleich öffnen soll.

**Wie heißt die Datei?** Der Name ergibt sich aus dem Filter:

| Filter | Dateiname |
|---|---|
| Projekt Öblarn, Jahr 2026, Mitarbeiterin Tanja | `oeblarn_2026_tanja.docx` |
| Projekt Graz/Huber, Oktober 2026 (gibt es Graz mehrmals, kommt der Kunde dazu) | `graz_huber_2026_10.docx` |
| alles, ohne Filter | `stundenbericht_gesamt.docx` |

Gibt es die Datei schon, fragt das Programm, ob sie überschrieben werden soll.
Bei `n` bekommt die neue Datei eine Nummer, z. B. `oeblarn_2026_tanja_2.docx`.

## 4. Wo liegen meine Daten?

Im Ordner **`Desktop\Stundenbuch-Daten`**, egal wohin du die App entpackt hast.
(Ist bei dir die OneDrive-Sicherung aktiv, liegt er im Desktop-Ordner von OneDrive.)

| Datei/Ordner | Inhalt |
|---|---|
| `mitarbeiter.csv` | alle Mitarbeiter |
| `projekte.csv` | alle Projekte |
| `eintraege.csv` | alle Einträge |
| `Berichte\` | deine gespeicherten Berichte |

Wichtig:

- **Jeder PC hat seine eigenen Daten.** Was du einträgst, sehen die anderen im Team nicht.
  Das Programm hat keine gemeinsame Datenbank. Für einen gemeinsamen Überblick schickt
  jeder seinen Bericht (Menüpunkt `9`) weiter.
- **Sicherung:** Kopiere ab und zu den ganzen Ordner `Stundenbuch-Daten` auf einen
  USB-Stick oder in die Cloud. Mehr braucht es nicht.
- **Auf einen anderen PC umziehen:** Den Ordner `Stundenbuch-Daten` auf den Desktop
  des neuen PCs kopieren und die App dort installieren (Schritt 1).
- **Die CSV-Dateien nicht in Excel bearbeiten und speichern.** Excel ändert dabei Datum
  und Zahlen, sodass das Programm Zeilen nicht mehr lesen kann. Ändern nur über die App.
  Ansehen ist kein Problem; für Excel gibt es den CSV-Bericht.

## 5. Neue Version installieren

Bekommst du eine neue `Stundenbuch.zip`:

1. Den Ordner **`Desktop\Stundenbuch`** (die alte App) löschen.
2. Die neue ZIP wie in Schritt 1 auf den Desktop entpacken.

Den Ordner **`Stundenbuch-Daten` nicht löschen**, dort sind deine Einträge.
Die Namen sind absichtlich verschieden, damit man beim Austauschen der App
nicht versehentlich die Daten erwischt.

## 6. Probleme und Lösungen

| Problem | Lösung |
|---|---|
| Windows blockiert den Start | „Weitere Informationen“, dann „Trotzdem ausführen“ (siehe Schritt 1) |
| „Speichern fehlgeschlagen“ beim Bericht | Die Datei ist wahrscheinlich in Word/Excel offen. Dort schließen und nochmal speichern |
| Desktop-Verknüpfung geht nicht mehr | Die App wurde in einen falschen Ordner entpackt. Es muss `Desktop\Stundenbuch\Stundenbuch.exe` geben, nicht `Desktop\Stundenbuch\Stundenbuch\Stundenbuch.exe` |
| Keine Farben im Fenster | Farben gibt es nur im Windows Terminal (Standard unter Windows 11). Alles andere funktioniert trotzdem |
| Einträge sind weg | Prüfen, ob der Ordner `Desktop\Stundenbuch-Daten` noch da ist; sonst die Sicherung zurückkopieren |
| Ein Mitarbeiter oder Projekt lässt sich nicht löschen | Es gibt noch Einträge dafür. Zuerst diese löschen (Menüpunkt `4`) |

---

# Teil 2: Am Code arbeiten

Java-Konsolenanwendung, Maven, keine zusätzlichen Bibliotheken.

## Voraussetzungen

- JDK 17 oder neuer
- Maven

Sind `java`/`mvn` nicht im PATH (z. B. nur über IntelliJ installiert):

- JDK: `%USERPROFILE%\.jdks\openjdk-25.0.2`
- Maven: `%LOCALAPPDATA%\Programs\IntelliJ IDEA 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd`
- Nach einem IntelliJ-Update ändert sich der Maven-Pfad. Dann auch oben in `build-exe.bat` anpassen.

## Projekt öffnen, bauen, starten

- **IntelliJ:** *File, Open*, den Projektordner (oder die `pom.xml`) wählen, als SDK ein JDK ab 17.
  Starten: `Main.java` öffnen, grüner Pfeil neben `main`.
- **Terminal:**
  ```
  mvn package
  java -jar target/stundenbuch-1.0.jar
  ```

## Testen (Probelauf)

Getestet wird mit vorbereiteten Eingaben aus `testeingaben/` und einem eigenen Testordner,
damit die echten Daten unberührt bleiben. Die Daten landen dann in `testdaten\Desktop\Stundenbuch-Daten`.
In PowerShell muss `-Duser.home=...` in Anführungszeichen stehen:

```
Get-Content testeingaben\schritt10.txt | java "-Duser.home=testdaten" -jar target\stundenbuch-1.0.jar
```

| Datei | Prüft |
|---|---|
| `schritt1.txt` bis `schritt10b.txt` | die Grundfunktionen; `schritt10.txt` ist der Gesamt-Probelauf |
| `schritt12.txt` | den Bericht (alle Filter, leeres Ergebnis, Überschreiben) |
| `schritt13.txt` | Kennungen: nach Löschen aller Mitarbeiter wieder `AB_01`, sonst weiterzählen |
| `schritt14.txt` | dasselbe für Projekt-Nummern und `#`-Nummern der Einträge |

`schritt12.txt` enthält Umlaute. Dafür in der Git Bash starten statt in PowerShell:
`java -Duser.home=testdaten -jar target/stundenbuch-1.0.jar < testeingaben/schritt12.txt`

## .exe und ZIP erstellen

Doppelklick auf `build-exe.bat` (nur Windows). Ergebnis:

- `dist\Stundenbuch\Stundenbuch.exe`: läuft ohne installiertes Java (bringt eine eigene, kleine Java-Laufzeit mit)
- `dist\Stundenbuch.zip` (ca. 16 MB): **diese Datei gibt man an das Team weiter** (siehe Teil 1)

## Klassen

| Klasse | Aufgabe |
|---|---|
| `Main` | startet das Menü |
| `Menue` | Ein- und Ausgabe im Terminal |
| `Konsole` | Aussehen im Terminal: Rahmen, Farben, Tabellen |
| `Zeiterfassung` | hält Mitarbeiter, Projekte und Einträge, rechnet und prüft die Regeln |
| `Speicher` | liest und schreibt die CSV-Dateien |
| `Bericht` | filtert Einträge (Zeitraum, Projekt, Mitarbeiter), rechnet Summen, baut Tabellen und den Dateinamen |
| `Tabelle` | nur Daten: Spalten und Zeilen. Terminal, Word und CSV zeichnen dieselbe Tabelle |
| `BerichtExport` | schreibt den Bericht als `.docx` (ZIP mit XML, ohne Bibliothek) oder `.csv` |
| `Format` | gemeinsame Formate: Datum, Monat, Stunden mit Komma |
| `Mitarbeiter` | Datenklasse: id, name |
| `Projekt` | Datenklasse: id, ort, kunde |
| `Eintrag` | Datenklasse: id, datum, mitarbeiter, projekt, stunden, beschreibung |

## Was ändere ich wo?

| Ich will ... | Datei |
|---|---|
| Menütexte, neue Menüpunkte, Eingaben, Hilfetext | `Menue.java` |
| Aussehen im Terminal (Rahmen, Farben) | `Konsole.java` |
| Inhalt des Berichts, Dateiname | `Bericht.java` |
| Aussehen der Word-Datei (Farben, Schrift) oder der CSV | `BerichtExport.java` |
| neue Berechnungen oder Regeln (z. B. Summen, Löschschutz) | `Zeiterfassung.java` |
| Speicherort, CSV-Format, Kopfzeile | `Speicher.java` |
| neue Felder (z. B. Telefonnummer beim Mitarbeiter) | Datenklasse **und** `Speicher.java` (Schreiben und Laden) **und** `Menue.java` (Eingabe) |
| Kennung `AB_01` ändern | `Mitarbeiter.java` (`KENNUNG_VORSILBE`) |

Regeln, die das Projekt sauber halten:

- `Zeiterfassung`, `Bericht` und `BerichtExport` benutzen nie `System.out`; nur `Menue` redet mit dem Benutzer.
- `Eintrag` speichert Objekte (`Mitarbeiter`, `Projekt`), keine Namen. Darum wirkt Umbenennen überall.
- Bei Änderungen am CSV-Format: **alte Dateien müssen weiter ladbar sein**, sonst sind bei allen Benutzern
  die Daten weg. So wurde es schon für `1` und `AB_01` gelöst (`Speicher.idAusKennung`).
- Die Menünummern nicht umsortieren: das Team kennt sie, und die Testeingaben hängen davon ab.

## CSV-Format der Daten

Trennzeichen `;`, UTF-8. Ein `;` im Text wird als `\s` gespeichert, ein `\` als `\\`.

| Datei | Spalten | Beispiel |
|---|---|---|
| `mitarbeiter.csv` | Kennung;Name | `AB_01;Jonas` |
| `projekte.csv` | Nr;Ort;Kunde | `1;Graz;Mustermann` |
| `eintraege.csv` | Nr;Datum;Mitarbeiter;ProjektNr;Stunden;Beschreibung | `1;2026-10-05;AB_01;1;10.0;Konzept erstellt` |

`eintraege.csv` beginnt mit dieser Kopfzeile. Sie fängt bewusst mit `Nr` an und nicht mit `ID`,
weil Excel Dateien, die mit `ID` beginnen, für SYLK-Dateien hält.

## Weitere Unterlagen

- `STUNDENBUCH_SPEC.md`: der Bauplan, nach dem die App Schritt für Schritt entstanden ist
