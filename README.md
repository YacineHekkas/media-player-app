# 🎵 Flutter Audio Player with Native Android Background Service

This project demonstrates how to integrate a **native Android audio playback service** into a **Flutter app**, enabling **background audio playback**, **media notifications**, and **session controls**, while persisting data using **Sqflite (SQLite)**.

## 📱 Features

- 🔁 Seamless integration of native Android (`Kotlin`) code with Flutter
- 🎧 Background audio playback using `MediaPlayer` and `Service`
- 📟 Custom media notifications (Play/Pause/Next/Previous/Stop)
- 🔌 MediaSession support for lock screen / headphone controls
- 📂 Reads audio from assets (supports fallback paths)
- 💾 Sqflite integration for saving playback data locally

---

## 📦 Tech Stack

| Layer            | Technology                         |
|------------------|-------------------------------------|
| UI (Frontend)    | Flutter                             |
| Native Backend   | Kotlin (Android `Service`)          |
| Audio Playback   | Android `MediaPlayer`, `MediaSessionCompat` |
| Notifications    | Android `NotificationCompat`        |
| Persistence      | Sqflite (SQLite via Flutter plugin) |

---

## 🧩 Integration Overview

### 🧠 Native Android Service

- Implemented in Kotlin (`AudioService.kt`)
- Handles audio playback using `MediaPlayer`
- Exposes actions via `Intent` (e.g., `ACTION_PLAY`, `ACTION_PAUSE`)
- Runs as a **foreground service** for reliable background execution
- Sends playback status to Flutter using **BroadcastReceiver**

### 📲 Flutter Integration

- Flutter communicates with native Android via `MethodChannel` or `Platform Channels`
- Playback commands are sent from Dart to native using intents
- Receives updates (e.g., `isPlaying`) via platform broadcasts

### 📁 Asset Management

- Audio files are loaded from:
  - `flutter_assets/assets/audio/test.mp3` (primary)
  - `assets/audio/test.mp3` (fallback)
  - Android `res/raw` (last resort)

---

## 💾 Sqflite Usage

Sqflite is used to:

- Store playback history or recently played tracks
- Save bookmarks, playback position, or user settings
- Perform local queries using raw SQL or helper methods

### 🛠️ Example Database Setup

```dart
import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';

Future<Database> initDatabase() async {
  final dbPath = await getDatabasesPath();
  return openDatabase(
    join(dbPath, 'audio_player.db'),
    onCreate: (db, version) {
      return db.execute(
        'CREATE TABLE playback(id INTEGER PRIMARY KEY, title TEXT, position INTEGER)',
      );
    },
    version: 1,
  );
}
````

---

## 🚀 Getting Started

### 1. Clone the Repo

```bash
git clone https://github.com/YOUR_USERNAME/your-repo-name.git
cd your-repo-name
```

### 2. Add Dependencies

In `pubspec.yaml`:

```yaml
dependencies:
  flutter:
    sdk: flutter
  sqflite: ^2.3.0
  path_provider: ^2.0.14
```

---

### 3. Android Setup

* Ensure `AudioService.kt` is added in `android/app/src/main/java/...`
* Register the service in `AndroidManifest.xml`:

```xml
<service
    android:name=".AudioService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="mediaPlayback"/>
```

* Add notification permission (Android 13+):

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
```

---

## 🎮 Controls

| Action    | Trigger                                  |
| --------- | ---------------------------------------- |
| Play      | `Intent` with action `ACTION_PLAY`       |
| Pause     | `Intent` with action `ACTION_PAUSE`      |
| Stop      | `Intent` with action `ACTION_STOP`       |
| Next/Prev | Implemented optionally in `AudioService` |

---

## 📢 Communication Bridge (Flutter ↔ Native)

Use `MethodChannel` or send `Intent` from Dart to Android:

```dart
const platform = MethodChannel('com.example.tp_mobile/audio');

Future<void> playAudio() async {
  await platform.invokeMethod('play');
}
```

On the Android side, handle this call and trigger appropriate actions.

---

## 🧪 Testing

* Verify background playback by starting audio, then closing the app.
* Check notification controls on lock screen or Bluetooth devices.
* Ensure Sqflite records are created and updated correctly.

---
