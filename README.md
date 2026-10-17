# files — Persönlicher Dateimanager für Android

files ist eine native Android-App für den privaten Sideload-Einsatz. Die App kombiniert Dateibrowser, Papierkorb, ZIP-Handling und In-App-Audioplayer in einer schlanken, dunkel gestalteten Oberfläche mit Fokus auf schnelle Bedienung und vollständigen Zugriff auf lokale Ordner im Dateisystem.

Die App wurde für den Betrieb auf Android-Geräten wie dem Google Pixel 10 Pro entwickelt. Sie ist zum Download in diesem Repository verfügbar unter `app-debug/app-debug.apk` und kann z.B. über die "SDK-Plattformtools für Windows herunterladen" von https://developer.android.com/tools/releases/platform-tools?hl=de installiert werden.

## Überblick

- Verzeichnis- und Dateiverwaltung mit lokaler Dateisystem-Integration
- Schnellzugriffe für wichtige Ordner wie Alle Dateien, Downloads und Musik
- Papierkorb mit Wiederherstellen und Leerfunktion
- Suche, Sortierung und Filterung von Dateien
- Native ZIP-Komprimierung und -Entpackung ohne WebView-Limits
- In-App-Audio-Player mit Mediensteuerung über Kopfhörer und Bluetooth
- Schwerpunkt auf einfache, touchfreundliche Bedienung für Android

## Hauptfunktionen

| Funktion | Status |
| --- | --- |
| Verzeichnisse durchsuchen | ✅ |
| Schnellzugriff-Kacheln | ✅ |
| Dateien öffnen mit nativen Android-Intents | ✅ |
| Papierkorb aktivierbar/deaktivierbar | ✅ |
| Papierkorb leeren und Wiederherstellen | ✅ |
| Umbenennen von Dateien und Ordnern | ✅ |
| Teilen von Dateien | ✅ |
| Suche nach Dateinamen | ✅ |
| Sortierung nach Name, Datum, Größe oder Typ | ✅ |
| Versteckte Dateien ein-/ausblenden | ✅ |
| Ordner erstellen | ✅ |
| ZIP-Dateien entpacken | ✅ |
| Ordner als ZIP archivieren | ✅ |
| In-App-Audioplayer für Musikdateien | ✅ |
| Wiedergabeliste aus dem aktuellen Ordner | ✅ |
| Lautstärke anpassen | ✅ |
| Kopfhörer-/Bluetooth-Mediakontrolle | ✅ |
| Letzte geöffneten Dateien merken | ✅ |

## Bedienkonzept

Die Oberfläche ist bewusst minimal und mobil optimiert:

- Dunkles Theme mit warmem Akzent: Schwarz/Anthrazit mit Bernsteingelb
- Große Touch-Targets ab 44 × 44 px
- Starke Rundungen für Buttons und Karten
- Fokus auf schnelle, einhändige Bedienung
- Native Android-Integrationen statt Web-Wrapper-Workarounds

## In-App-Audioplayer

Der Musikbereich ist über den Schnellzugriff Musik erreichbar. Beim Öffnen einer Audiodatei startet die Wiedergabe direkt in der App.

Besondere Merkmale:

- Player-Leiste am unteren Rand mit Steuerungselementen
- Aktuell spielender Titel wird visuell hervorgehoben
- Autoplay zum nächsten Titel nach Abschluss
- Wiederholungslogik am Ende der Playlist
- Playlist aus dem aktuellen Musikordner
- Lautstärke über separate Steuerung
- Media-Session für Kopfhörer- und Bluetooth-Tasten

### Unterstützte Media-Tasten

| Taste | Aktion |
| --- | --- |
| Play/Pause | Wiedergabe pausieren bzw. fortsetzen |
| Nächster Titel | Nächstes Lied in der Playlist |
| Vorheriger Titel | Vorheriges Lied in der Playlist |

## ZIP-Funktionen

### Ordner komprimieren

Ein Ordner kann über das Kontextmenü als ZIP-Datei gespeichert werden.

- ZIP-Datei wird im selben Verzeichnis wie der Ordner erstellt
- Dateiname entspricht dem Ordnernamen, etwa Fotos.zip
- Unterordner und Dateien werden rekursiv berücksichtigt
- Erfolgs- und Fehlerzustände werden über Toasts angezeigt
- Umsetzung über das native ZIP-Plugin mit Java ZipOutputStream

### ZIP-Dateien entpacken

ZIP-Dateien lassen sich direkt durch Antippen entpacken.

- Entpackung erfolgt im selben Verzeichnis
- Unterordner werden automatisch angelegt
- Fortschrittsanzeige über Toasts
- Fehlerzustände wie ungültiges Archiv oder fehlende Berechtigung werden abgefangen
- Umsetzung über Java ZipInputStream

## Papierkorb und Dateiverwaltung

Die App erlaubt einen bewussten Umgang mit gelöschten Dateien:

- Papierkorb optional aktivierbar bzw. deaktivierbar
- Wiederherstellen einzelner Elemente
- Gesamten Papierkorb leeren
- Schnellzugriff auf aktuelle Dateistruktur
- Versteckte Dateien optional ein- oder ausblenden

## Ordner erstellen

Neue Ordner können direkt aus wichtigen Schnellzugriffen wie Alle Dateien und Downloads angelegt werden.

- Dialog mit Standardnamen Neuer Ordner
- Ordner wird im aktuellen Verzeichnis angelegt
- Sofortige Aktualisierung der Dateiliste
- Toast-Bestätigung nach erfolgreichem Anlegen

## Voraussetzungen

Für die lokale Entwicklung und den Build werden die folgenden Tools benötigt:

- Node.js 18 oder höher
- Java JDK 21
- Android SDK / Android Debug Bridge (ADB)
- Setzen von JAVA_HOME auf die JDK 21 Installation

Beispiel:

```bash
export JAVA_HOME=/usr/lib/jvm/jdk-21.0.7-oracle-x64
export PATH=$JAVA_HOME/bin:$PATH
```

## Schnellstart

### Erstes Setup

```bash
npm install
npx cap add android
npx cap sync
cd android
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Wiederholte Builds nach Änderungen

```bash
npx cap sync
cd android
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Android-Berechtigungen

In der App werden für den Zugriff auf das geschützte Dateisystem folgende Berechtigungen verwendet:

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="29"/>
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"/>
```

Beim ersten Start öffnet die App die Android-Einstellungen zur Freigabe der Berechtigung für Alle Dateien verwalten. Nach der Aktivierung ist volles Lesen und Schreiben in lokal verfügbare Ordner wie /sdcard/ möglich.

## Technische Hinweise

- App ist für den privaten Sideload-Einsatz konzipiert
- Kein Play-Store-Release, keine Standard-Play-Store-Kompatibilität
- MANAGE_EXTERNAL_STORAGE ist für normale Play-Store-Apps nicht zulässig
- Gradle benötigt zwingend JDK 21; JDK 23 oder 24 sind nicht kompatibel
- Die Weboberfläche lebt in www/index.html, die native Integration in Android liegt unter android/

## Projektstruktur

```text
.
├── android/                  # Native Android-Implementierung
├── www/                     # Web-UI und JavaScript-Logik
├── capacitor.config.json    # Capacitor-Konfiguration
├── package.json             # Projekt- und Build-Skripte
├── README.md                # Projektbeschreibung
├── LICENSE                  # Lizenz
└── .gitignore
```

Wichtige native Erweiterungen:

- MainActivity.java: Android-Startlogik und Media-Session-Integration
- ZipPlugin.java: ZIP-Komprimierung/Entpackung nativ in Java

## Lizenz und Erwerb

Die Software wird als kommerzielle App für den privaten Sideload-Betrieb vertrieben.

### Preis

3,88 EUR

### Zahlungsinformationen

Name: Stephan Epp  
IBAN: DE24 5003 1900 0012 5603 20  
BIC: BBVADEFFXXX

## Hinweis

Die App wurde speziell für private Nutzung und lokale Geräteverwaltung entwickelt. Für öffentliche Store-Veröffentlichungen oder Standard-Play-Store-Distributionen ist eine separate Prüfung und ggf. Anpassung der Berechtigungen und Release-Strategie erforderlich.

