@echo off
rem Baut dist\Stundenbuch\Stundenbuch.exe (läuft ohne installiertes Java)
rem und packt den Ordner als dist\Stundenbuch.zip zum Weitergeben.

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
copy target\stundenbuch-1.0.jar target\jpackage-input\ >nul

echo [3/4] Erstelle Stundenbuch.exe ...
rem --add-modules: nur was die App braucht (java.base), dazu deutsche Formate
rem (jdk.localedata, sonst 15.0 statt 15,0) und Terminal-Kodierungen (jdk.charsets, für Umlaute)
jpackage --type app-image ^
  --name Stundenbuch ^
  --app-version 1.0 ^
  --input target\jpackage-input ^
  --main-jar stundenbuch-1.0.jar ^
  --main-class stundenbuch.Main ^
  --add-modules java.base,jdk.localedata,jdk.charsets ^
  --jlink-options "--strip-native-commands --strip-debug --no-man-pages --no-header-files --include-locales=de" ^
  --win-console ^
  --dest dist || goto :fehler

echo [4/4] Packe dist\Stundenbuch.zip ...
powershell -NoProfile -Command "Compress-Archive -Path dist\Stundenbuch -DestinationPath dist\Stundenbuch.zip -Force" || goto :fehler

echo.
echo Fertig: dist\Stundenbuch\Stundenbuch.exe
echo Zum Weitergeben: dist\Stundenbuch.zip
pause
exit /b 0

:fehler
echo Es ist ein Fehler aufgetreten.
pause
exit /b 1
