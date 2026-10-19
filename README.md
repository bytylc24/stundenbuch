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

Im Ordner `Stundenbuch` im Benutzerordner, unter Windows z. B. `C:\Users\Name\Stundenbuch`.
Dort liegen drei CSV-Dateien (Trennzeichen `;`, UTF-8):

| Datei | Inhalt |
|---|---|
| `mitarbeiter.csv` | id;name |
| `projekte.csv` | id;ort;kunde |
| `eintraege.csv` | id;datum;mitarbeiterId;projektId;stunden;beschreibung |

Jede Änderung wird sofort gespeichert.

## .exe erstellen

Mit `build-exe.bat` (Windows) entsteht `dist\Stundenbuch\Stundenbuch.exe`, die auch ohne installiertes Java läuft.
Weitergeben muss man den ganzen Ordner `dist\Stundenbuch`, z. B. als ZIP.

## Testen (Probelauf)

Getestet wird mit vorbereiteten Eingaben aus `testeingaben/` und einem eigenen Testordner, damit die echten Daten unberührt bleiben.
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
