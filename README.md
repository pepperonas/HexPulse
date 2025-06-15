# HexPulse - Abalone für Android

Ein vollständiges Abalone-Brettspiel für Android mit KI-Gegnern und anpassbaren Themen.

## 📱 Über das Spiel

HexPulse ist eine Android-Implementierung des klassischen Strategiespiels Abalone. Das Spiel bietet eine intuitive Touch-Steuerung, verschiedene Schwierigkeitsgrade für KI-Gegner und mehrere visuelle Themen.

### Spielregeln
- **Ziel**: Schiebe 6 gegnerische Kugeln vom Brett
- **Bewegung**: Bewege 1-3 eigene Kugeln in einer Linie
- **Sumito**: Überwältige gegnerische Kugeln durch zahlenmäßige Überlegenheit
- **Gewinn**: Der erste Spieler, der 6 gegnerische Kugeln eliminiert, gewinnt

## ✨ Features

### Spielmodi
- **Spieler vs. Spieler**: Lokaler Mehrspieler-Modus
- **Spieler vs. KI**: Spiele gegen intelligente Computer-Gegner
- **Drei Schwierigkeitsgrade**: Leicht, Mittel, Schwer

### Technische Highlights
- **Hexagonales Koordinatensystem**: Präzise Brettlogik mit axialen Koordinaten
- **Erweiterte KI**: Minimax-Algorithmus mit Alpha-Beta-Pruning
- **Anpassbare Themes**: Classic, Dark, Ocean, Forest
- **3D-Grafiken**: Realistische Kugeldarstellung mit Schatten und Glanzlichtern
- **Touch-Steuerung**: Intuitive Kugelauswahl und Bewegung
- **Erweiterte Visualisierung**: Bewegungspfeile, Zielindikatoren und Vorschau-Kugeln
- **Flüssige Animationen**: 1-Sekunden-Bewegungsanimationen mit Easing-Funktionen
- **Vergrößertes Spielbrett**: Optimierte Größe für bessere Spielbarkeit

### KI-System
- **Asynchrone Verarbeitung**: Flüssiges Gameplay ohne UI-Blockierung
- **Intelligente Bewertung**: Positionsbewertung, Zentrumskontrolle, Zusammenhalt
- **Performance-Optimierung**: Zug-Caching und begrenzte Suchtiefe

## 🛠️ Technische Details

- **Sprache**: Java 11
- **Min SDK**: Android 24 (Android 7.0)
- **Target SDK**: Android 35
- **Build-System**: Gradle mit Kotlin DSL
- **Architektur**: MVVM mit Navigation Component
- **UI**: Custom Views mit Material Design

## 📋 Installation

### Voraussetzungen
- Android Studio Arctic Fox oder höher
- Android SDK 35
- Java Development Kit (JDK) 11+

### Build-Anweisungen
```bash
# Repository klonen
git clone [repository-url]
cd HexPulse

# Projekt bauen
./gradlew build

# Debug-APK erstellen
./gradlew assembleDebug

# Release-APK erstellen  
./gradlew assembleRelease

# Tests ausführen
./gradlew test

# Lint-Prüfung
./gradlew lint
```

### Installation auf Gerät
```bash
# Debug-Version installieren
./gradlew installDebug

# Instrumentierte Tests ausführen
./gradlew connectedAndroidTest
```

## 🎮 Spielanleitung

### Grundsteuerung
1. **Kugel auswählen**: Tippe auf eine deiner Kugeln (goldene Hervorhebung)
2. **Mehrere Kugeln**: Tippe weitere Kugeln an (max. 3 in einer Linie)
3. **Gültige Züge**: Grün hervorgehobene Felder mit Zielindikatoren
4. **Bewegungsvorschau**: Gestrichelte Pfeile zeigen Kugelverschiebungen
5. **Bewegung**: Tippe auf eine gültige Zielposition
6. **Animation**: Kugeln bewegen sich flüssig über das Brett (1 Sekunde)
7. **Vorschau-Kugeln**: Semi-transparente Kugeln zeigen Endpositionen beim Hovern
8. **Auswahl löschen**: Tippe "Clear" oder außerhalb der gültigen Bereiche

### Bewegungstypen
- **Einzelkugel**: Bewege eine Kugel in jede Richtung
- **Inline-Bewegung**: Bewege 2-3 Kugeln in Linienrichtung
- **Seitwärtsbewegung**: Bewege 2-3 Kugeln senkrecht zur Linie
- **Sumito**: Schiebe schwächere gegnerische Gruppen

### Spielmodi
- **PvP**: Abwechselndes Spiel zweier menschlicher Spieler
- **vs KI**: Schwarzer Spieler (du) gegen weißen KI-Gegner mit animierten Zügen
- **Reset**: Neues Spiel starten
- **Clear**: Aktuelle Auswahl aufheben
- **Während Animation**: Touch-Eingaben werden blockiert für flüssige Darstellung

## 🎨 Themes

Das Spiel bietet verschiedene visuelle Themen:

- **Classic**: Traditionelle blau-graue Farbgebung
- **Dark**: Dunkles Design für reduzierte Augenbelastung  
- **Ocean**: Meeresblau-inspirierte Farben
- **Forest**: Natürliche grün-braune Töne

## 🧠 KI-Schwierigkeitsgrade

### Leicht
- Suchtiefe: 1 Zug
- 50% zufällige Züge, 50% oberflächliche Bewertung
- Schnelle Reaktionszeit

### Mittel  
- Suchtiefe: 2 Züge
- Vollständige Minimax-Bewertung
- Ausgewogene Herausforderung

### Schwer
- Suchtiefe: 3 Züge
- Erweiterte Positionsbewertung
- Strategische Langzeitplanung

## 🏗️ Architektur

### Paketstruktur
```
io.celox.hexpulse/
├── game/              # Spiellogik
│   ├── AbaloneGame    # Kern-Spielengine
│   ├── AbaloneAI      # KI-Implementation  
│   ├── Hex            # Hexagonale Koordinaten
│   ├── Player         # Spieler-Enum
│   └── Theme          # Visual-Themes
├── ui/
│   ├── home/          # Hauptmenü
│   ├── gallery/       # Spielbildschirm
│   ├── slideshow/     # Einstellungen
│   └── views/         # Custom Views
└── MainActivity       # Navigation Setup
```

### Design Patterns
- **MVVM**: Trennung von UI und Geschäftslogik
- **Observer**: LiveData für UI-Updates
- **Strategy**: Austauschbare KI-Schwierigkeitsgrade
- **Custom Views**: Wiederverwendbare UI-Komponenten

## 🔧 Entwicklung

### Wichtige Klassen
- `AbaloneGame`: Spielregeln und Zustandsverwaltung
- `HexagonalBoardView`: Custom View für Brettdarstellung
- `AbaloneAI`: KI-Gegner mit Minimax-Algorithmus
- `Hex`: Hexagonale Koordinaten-Mathematik

### Testing
```bash
# Unit Tests
./gradlew test

# Instrumentierte Tests  
./gradlew connectedAndroidTest

# Einzelne Testklasse
./gradlew test --tests "io.celox.hexpulse.ExampleUnitTest"
```

### Code-Stil
- Java 11 Features wo möglich
- Material Design Guidelines
- Responsive Layout für verschiedene Bildschirmgrößen
- Performante Custom Views ohne Memory Leaks

### Animation-System
- **Easing-Funktionen**: Smooth cubic in-out für natürliche Bewegung
- **Frame-basiert**: 60 FPS Animation mit `invalidate()` Zyklen
- **Thread-safe**: UI-Thread Animation mit proper Lifecycle-Management
- **Interaktions-Blocking**: Touch-Events werden während Animation blockiert
- **Callback-System**: `onAnimationComplete()` für sequenzielle Spiellogik

## 📄 Lizenz

MIT License

Copyright (c) 2025 Martin Pfeffer

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## 👨‍💻 Entwickler

**Martin Pfeffer** - 2025

Entwickelt mit ❤️ für Strategiespiel-Enthusiasten.

## 🙏 Danksagungen

- Basierend auf dem klassischen Abalone-Brettspiel
- Inspiriert von der Python-Pygame-Implementation
- Verwendet Android Material Design Components
- KI-Algorithmus basiert auf klassischen Spieltheorie-Konzepten