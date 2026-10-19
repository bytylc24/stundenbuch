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

Im Ordner `Stundenbuch` auf dem Desktop, z. B. `C:\Users\Name\Desktop\Stundenbuch`
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
„Weitere Informationen“ und dann „Trotzdem ausführen“ klicken. Die Daten landen bei ihm in `Desktop\Stundenbuch`.

## Testen (Probelauf)

Getestet wird mit vorbereiteten Eingaben aus `testeingaben/` und einem eigenen Testordner, damit die echten Daten unberührt bleiben.
Die Daten landen dann in `testdaten\Desktop\Stundenbuch`.
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
