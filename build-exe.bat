@echo off
rem Baut dist\Stundenbuch\Stundenbuch.exe (die Oberflaeche mit JavaFX, laeuft ohne installiertes Java)
rem und packt den Ordner als dist\Stundenbuch-GUI.zip zum Weitergeben.

rem Falls java/mvn nicht im PATH sind: hier liegen JDK und Maven (aus IntelliJ)
set "JDK_ORDNER=%USERPROFILE%\.jdks\openjdk-25.0.2"
set "MAVEN_ORDNER=%LOCALAPPDATA%\Programs\IntelliJ IDEA 2025.2.2\plugins\maven\lib\maven3"

where jpackage >nul 2>nul
if errorlevel 1 set "PATH=%JDK_ORDNER%\bin;%PATH%"
where mvn >nul 2>nul
if errorlevel 1 set "PATH=%MAVEN_ORDNER%\bin;%PATH%"

echo [1/4] Baue die Jar ...
call mvn -q clean package || goto :fehler

echo [2/4] Bereite Dateien vor ...
if exist dist rmdir /s /q dist
mkdir target\jpackage-input
copy target\stundenbuch-2.0-gui.jar target\jpackage-input\ >nul

echo [3/4] Erstelle Stundenbuch.exe ...
rem Die Jar enthaelt JavaFX schon, darum reichen die normalen Java-Module (java.desktop und
rem java.logging braucht JavaFX, jdk.unsupported auch). jdk.localedata: deutsche Formate (15,0 statt 15.0),
rem jdk.charsets: Kodierungen. Kein --win-console: es oeffnet sich nur das Programmfenster.
jpackage --type app-image ^
  --name Stundenbuch ^
  --app-version 2.0 ^
  --input target\jpackage-input ^
  --main-jar stundenbuch-2.0-gui.jar ^
  --main-class stundenbuch.gui.Start ^
  --add-modules java.base,java.desktop,java.logging,java.xml,jdk.unsupported,jdk.localedata,jdk.charsets ^
  --java-options "--enable-native-access=ALL-UNNAMED" ^
  --jlink-options "--strip-native-commands --strip-debug --no-man-pages --no-header-files --include-locales=de" ^
  --dest dist || goto :fehler

echo [4/4] Packe dist\Stundenbuch-GUI.zip ...
powershell -NoProfile -Command "Compress-Archive -Path dist\Stundenbuch -DestinationPath dist\Stundenbuch-GUI.zip -Force" || goto :fehler

echo.
echo Fertig: dist\Stundenbuch\Stundenbuch.exe
echo Zum Weitergeben: dist\Stundenbuch-GUI.zip
pause
exit /b 0

:fehler
echo Es ist ein Fehler aufgetreten.
pause
exit /b 1