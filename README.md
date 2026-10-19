# Stundenbuch

Eine Java-Konsolenanwendung zur Zeiterfassung für kleine Teams: Sie erfasst, **wer wann an welchem Projekt wie viele Stunden** gearbeitet hat und was gemacht wurde.
Die Stunden werden pro Projekt und pro Mitarbeiter zusammengezählt und lassen sich als Tabelle, auch gefiltert nach Monat, auswerten.

## Voraussetzungen

- JDK 17 oder neuer
- Maven

## Bauen

```
mvn package
```

Ergebnis: `target/stundenbuch-1.0.jar`

## Starten

```
java -jar target/stundenbuch-1.0.jar
```

Im Programm erklärt der Menüpunkt `8) Hilfe` alle Funktionen.

## Wo liegen die Daten?

Im Ordner `Stundenbuch-Daten` auf dem Desktop, z. B. `C:\Users\Name\Desktop\Stundenbuch-Daten`
(bei aktiver OneDrive-Sicherung im Desktop-Ordner von OneDrive).
Dort liegen drei CSV-Dateien (Trennzeichen `;`, UTF-8):

| Datei | Inhalt | Beispiel |
|---|---|---|
| `mitarbeiter.csv` | Kennung;Name | `AB_01;Jonas` |
| `projekte.csv` | Nr;Ort;Kunde | `1;Graz;Mustermann` |
| `eintraege.csv` | Nr;Datum;Mitarbeiter;ProjektNr;Stunden;Beschreibung | `1;2026-10-05;AB_01;1;10.0;Konzept erstellt` |

`eintraege.csv` beginnt mit einer Kopfzeile, die die Spalten erklärt.
Jeder Mitarbeiter hat eine feste Kennung: `AB_01` = Arbeiter 1, `AB_02` = Arbeiter 2 usw.
Jede Änderung wird sofort gespeichert.

## .exe erstellen

Doppelklick auf `build-exe.bat` (nur unter Windows). Das Skript erzeugt:

- `dist\Stundenbuch\Stundenbuch.exe`: läuft auch ohne installiertes Java
- `dist\Stundenbuch.zip` (ca. 16 MB): diese Datei verschickt man

Die `.exe` allein funktioniert nicht, sie braucht den Ordner `runtime` daneben. Darum immer die ZIP weitergeben.

**Für den Empfänger:** ZIP entpacken (Rechtsklick, „Alle extrahieren“) und `Stundenbuch.exe` doppelklicken.
Weil die App nicht signiert ist, warnt Windows beim ersten Start („Der Computer wurde durch Windows geschützt“):
„Weitere Informationen“ und dann „Trotzdem ausführen“ klicken. Die Daten landen bei ihm in `Desktop\Stundenbuch-Daten`, egal wo er die ZIP entpackt.
Der Datenordner heißt bewusst anders als der App-Ordner `Stundenbuch`, damit man beim Löschen
oder Ersetzen der App nie versehentlich die Daten mitlöscht.

## Testen (Probelauf)

Getestet wird mit vorbereiteten Eingaben aus `testeingaben/` und einem eigenen Testordner, damit die echten Daten unberührt bleiben.
Die Daten landen dann in `testdaten\Desktop\Stundenbuch-Daten`.
In PowerShell muss `-Duser.home=...` in Anführungszeichen stehen:

```
Get-Content testeingaben\schritt10.txt | java "-Duser.home=testdaten" -jar target\stundenbuch-1.0.jar
```

## Klassen

| Klasse | Aufgabe |
|---|---|
| `Main` | startet das Menü |
| `Menue` | Ein- und Ausgabe im Terminal |
| `Zeiterfassung` | hält Mitarbeiter, Projekte und Einträge, rechnet und prüft die Regeln |
| `Speicher` | liest und schreibt die CSV-Dateien |
| `Mitarbeiter` | Datenklasse: id, name |
| `Projekt` | Datenklasse: id, ort, kunde |
| `Eintrag` | Datenklasse: id, datum, mitarbeiter, projekt, stunden, beschreibung |

## Weiterarbeiten (Notizen für später)

### Wo liegt was?

| Was | Wo |
|---|---|
| Projekt (Code) | `Desktop\Stundenerfassung_v01` |
| GitHub | https://github.com/bytylc24/stundenbuch (Branch `main`) |
| Fertige App zum Weitergeben | `dist\Stundenbuch.zip` (entsteht durch `build-exe.bat`) |
| Bauplan, nach dem die App entstanden ist | `STUNDENBUCH_SPEC.md` |
| Testeingaben für die Probeläufe | `testeingaben\schritt1.txt` bis `schritt10b.txt` |

Ist der Ordner weg oder du arbeitest an einem anderen PC: `git clone https://github.com/bytylc24/stundenbuch.git`

### Projekt öffnen

- **IntelliJ:** *File, Open*, den Projektordner (oder die `pom.xml`) wählen. Als SDK ein JDK ab 17 nehmen (z. B. `openjdk-25` unter `%USERPROFILE%\.jdks`).
- **Starten in IntelliJ:** `Main.java` öffnen, grüner Pfeil neben `main`.
- Java und Maven sind auf diesem PC **nicht im PATH**, sie kommen von IntelliJ:
  - JDK: `%USERPROFILE%\.jdks\openjdk-25.0.2`
  - Maven: `%LOCALAPPDATA%\Programs\IntelliJ IDEA 2025.2.2\plugins\maven\lib\maven3\bin\mvn.cmd`
  - Nach einem IntelliJ-Update ändert sich der Maven-Pfad. Dann auch oben in `build-exe.bat` anpassen.

### Was ändere ich wo?

| Ich will ... | Datei |
|---|---|
| Menütexte, neue Menüpunkte, Eingaben, Anzeige, Hilfetext | `Menue.java` |
| neue Berechnungen oder Regeln (z. B. Summen, Löschschutz) | `Zeiterfassung.java` |
| Speicherort, CSV-Format, Kopfzeile | `Speicher.java` |
| neue Felder (z. B. Telefonnummer beim Mitarbeiter) | Datenklasse **und** `Speicher.java` (Schreiben und Laden) **und** `Menue.java` (Eingabe) |
| Kennung `AB_01` ändern | `Mitarbeiter.java` (`KENNUNG_VORSILBE`) |

Regeln, die das Projekt sauber halten:
- `Zeiterfassung` benutzt nie `System.out`, nur `Menue` redet mit dem Benutzer.
- `Eintrag` speichert Objekte (`Mitarbeiter`, `Projekt`), keine Namen. Darum wirkt Umbenennen überall.
- Bei Änderungen am CSV-Format: **alte Dateien müssen weiter ladbar sein**, sonst sind bei allen Benutzern die Daten weg. So wurde es schon für `1` und `AB_01` gelöst (`Speicher.idAusKennung`).

### Nach einer Änderung

1. **Testen** mit einem Testordner (nie mit echten Daten), in PowerShell:
   ```
   mvn -q package
   Get-Content testeingaben\schritt10.txt | java "-Duser.home=testdaten" -jar target\stundenbuch-1.0.jar
   ```
   `schritt10.txt` ist der Gesamt-Probelauf. Für eine neue Funktion eine eigene Testdatei mit Falscheingaben anlegen.
2. **Committen und pushen:** `git add .`, `git commit -m "..."`, `git push`
3. **Neue .exe:** Doppelklick auf `build-exe.bat`, ergibt eine neue `dist\Stundenbuch.zip`.
4. **Update bei anderen:** Dort den Ordner `Stundenbuch` (die App) löschen und die neue ZIP entpacken.
   Der Ordner `Stundenbuch-Daten` bleibt unangetastet, die Daten bleiben erhalten.

### Mit Claude Code weiterarbeiten

Im Projektordner ein Terminal öffnen, `claude` starten und z. B. schreiben:
*„Lies README.md und STUNDENBUCH_SPEC.md. Ich möchte ... ändern.“*
