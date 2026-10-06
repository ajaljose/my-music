# Harmonia - Modern Native Android Music Player

Harmonia is a complete, production-quality, offline native Android music player built with modern Android development best practices.

---

## 🌟 Key Features

- **100% Native Kotlin & Jetpack Compose:** Built using Material 3 design system with dynamic theming (Material You Monet), fluid transitions, and sleek dark/light mode support.
- **AndroidX Media3 & ExoPlayer Playback:** High-performance audio engine supporting **MP3, M4A, AAC, FLAC, OGG, and WAV**.
- **Background Playback & Foreground Service:** Music continues playing smoothly when the app is minimized, the screen is locked, or other apps are opened.
- **MediaSession & Lock Screen Controls:** Full integration with Android's system media controls, rich notifications with album art and seek controls.
- **Audio Focus & Headset Integration:** Handles audio focus ducking/pauses during phone calls and other app interruptions. Automatically pauses when wired or Bluetooth headphones are disconnected (`ACTION_AUDIO_BECOMING_NOISY`).
- **Device MediaStore Scanner:** Automatically discovers all local audio files across internal and external storage using ContentResolver and direct content URIs without unnecessary file copying.
- **Room Database Architecture:** Persists song metadata, playlists, custom ordering, favorites, play counts, and playback history.
- **Comprehensive Library Browsing:**
  - **Home:** Recently played tracks, recently added songs, frequently played statistics, quick access chips, and featured albums.
  - **Songs:** Complete song list with instant debounce search, sort by Title, Artist, Album, Date Added, and Duration.
  - **Albums:** Beautiful album cards grid and full Album Detail screen with Play and Shuffle actions.
  - **Artists:** Artist directory with track and album listings.
  - **Playlists:** Create, rename, delete custom playlists, add/remove songs, plus built-in Favorites and Recently Played playlists.
  - **Folders:** Browse the device's real directory hierarchy (e.g. `Music/Downloads`, `Music/Albums`).
- **Full-Screen Now Playing & Mini Player:**
  - Persistent bottom Mini Player with progress bar, play/pause, next track, and favorite toggle.
  - Full-screen Now Playing view with large album artwork, swipe gestures to change tracks, live draggable seek bar, shuffle, repeat (Off / All / One), and queue management.
- **Sleep Timer:** Set timer for 15, 30, 45, 60, 90 minutes, custom minutes, or stop at the end of the current song.
- **Hilt Dependency Injection & Clean MVVM:** Clear separation between Data, Domain, Playback, and UI layers.

---

## 📋 Requirements

- **Android Studio:** Android Studio Ladybug (2024.2.1+) or newer.
- **JDK:** Java 17 or Java 21 (Android Studio's bundled JBR is recommended).
- **Android SDK:**
  - `minSdk`: **26** (Android 8.0 Oreo)
  - `targetSdk`: **35** (Android 15)
  - `compileSdk`: **35**

---

## 🚀 Getting Started in Android Studio

1. Launch **Android Studio**.
2. Select **Open** and choose the `my-music` directory.
3. Allow Gradle to sync dependencies automatically.
4. Set up an Android Virtual Device (AVD) or connect a physical device.
5. Click **Run ▶** (`Shift + F10`) to build and launch Harmonia.

---

## 📱 Running on Android Emulator

1. In Android Studio, open **Device Manager** (`Tools → Device Manager`).
2. Create or start an emulator running **Android 8.0 (API 26)** to **Android 15 (API 35)**.
3. Click **Run ▶** to deploy the app to the emulator.
4. **Adding Sample Music to Emulator:**
   - You can drag and drop any `.mp3` or audio file directly onto the running emulator window.
   - Alternatively, use ADB from your terminal:
     ```bash
     adb push /path/to/your/song.mp3 /sdcard/Music/
     adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Music/song.mp3
     ```
   - Open Harmonia and tap **Rescan Music Library** or grant storage permissions when prompted.

---

## 📲 Installing on a Physical Android Phone

1. **Enable Developer Options:**
   - Go to **Settings → About Phone**.
   - Tap **Build Number** 7 times until you see *"You are now a developer!"*.
2. **Enable USB Debugging:**
   - Go to **Settings → System → Developer Options**.
   - Turn on **USB Debugging**.
3. **Connect Device:**
   - Connect your phone to your computer with a USB cable.
   - When the prompt *"Allow USB debugging?"* appears on your phone screen, check *"Always allow"* and tap **Allow**.
4. **Run from Android Studio:**
   - Select your physical device in the target device dropdown in Android Studio.
   - Click **Run ▶** to install and launch the application directly.

---

## 📦 Building the APK

### Via Command Line (Gradle)

To build the standalone **Debug APK**:
```bash
./gradlew assembleDebug
```
The generated APK will be located at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

To install the built debug APK directly to a connected device:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

To build a **Release APK**:
```bash
./gradlew assembleRelease
```
The release APK will be located at:
```text
app/build/outputs/apk/release/app-release-unsigned.apk
```

### Via Android Studio GUI

1. Open Android Studio.
2. Go to **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
3. Once the build finishes, click the **locate** popup notification to open the folder containing `app-debug.apk`.

---

## 🏗️ Project Architecture

```text
app/src/main/java/com/example/musicplayer/
├── HarmoniaApp.kt                 # Application class & Notification channels
├── MainActivity.kt                # Activity entrypoint & Runtime permissions
│
├── data/
│   ├── local/
│   │   ├── HarmoniaDatabase.kt    # Room database
│   │   ├── dao/                   # SongDao, PlaylistDao, PlaybackHistoryDao
│   │   └── entities/              # SongEntity, PlaylistEntity, CrossRef, History
│   ├── media/
│   │   └── MediaStoreScanner.kt   # ContentResolver audio scanner
│   ├── preferences/
│   │   └── PlayerPreferences.kt   # DataStore theme & playback preferences
│   └── repository/
│       └── MusicRepositoryImpl.kt # Repository implementation
│
├── domain/
│   ├── model/                     # Song, Album, Artist, Playlist, Folder, PlaybackState
│   └── repository/                # MusicRepository interface
│
├── playback/
│   ├── manager/
│   │   └── MusicPlaybackManager.kt# Playback state coordinator & MediaController bridge
│   ├── receiver/
│   │   └── BecomingNoisyReceiver.kt # Audio unplug detection
│   └── service/
│       ├── MusicService.kt        # Media3 MediaLibraryService foreground service
│       └── MusicServiceCallback.kt# MediaLibrarySession callbacks
│
├── di/
│   └── AppModule.kt               # Hilt Dependency Injection module
│
└── ui/
    ├── MainScreen.kt              # App scaffold, mini player, now playing transition
    ├── MainViewModel.kt           # Top-level state and player actions
    ├── theme/                     # Material 3 colors, typography, and theme
    ├── components/                # Reusable Compose UI components
    ├── navigation/                # Screen routes, NavGraph, Bottom navigation bar
    ├── home/                      # Home dashboard & statistics
    ├── songs/                     # Song catalog, search & sort
    ├── albums/                    # Albums grid & album detail view
    ├── artists/                   # Artists directory & artist detail view
    ├── playlists/                 # Custom playlists & favorites management
    ├── folders/                   # Hierarchical directory explorer
    ├── nowplaying/                # Full player screen & queue sheet
    └── settings/                  # Appearance, sleep timer, & library settings
```

---

## 🔒 Permissions Handled

- **`READ_MEDIA_AUDIO`:** Android 13+ (API 33+) permission for reading local audio.
- **`READ_EXTERNAL_STORAGE`:** Android 8.0 to Android 12 (API 26-32) fallback.
- **`POST_NOTIFICATIONS`:** Android 13+ permission for media playback controls notification.
- **`FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PLAYBACK`:** Required for uninterrupted background audio service on Android 14+ (API 34+).
- **`WAKE_LOCK`:** Prevents CPU sleep during playback when the screen is turned off.

---

## 📄 License

Open-source project built with Google Android Jetpack libraries and Apache 2.0 / MIT compatible components.
