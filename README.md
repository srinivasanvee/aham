# Aham — Personal Android App

A personal Android app built with Jetpack Compose and Material Design 3. "Aham" (अहम्) means "I am" in Sanskrit — the app is a personal companion for mindfulness, productivity, and daily living.

---

## Tech Stack

| Area | Technology |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 |
| Min SDK | 33 (Android 13) |
| Target SDK | 36 (Android 15) |
| Architecture | MVVM + Clean Architecture |
| Navigation | Jetpack Navigation Compose |
| Audio | AndroidX Media3 (ExoPlayer) |
| DI | Hilt (planned) |

---

## Features

### Mantra / Meditation Player *(in progress)*
Play sacred mantras or ambient meditation audio on seamless loop. Supports background playback via a foreground service so the audio continues while the user navigates away or locks the screen.

---

## Project Structure (planned)

```
app/src/main/java/com/sri/aham/
├── MainActivity.kt                  # Single-activity host
├── navigation/
│   └── AppNavGraph.kt               # Compose Navigation graph
├── home/
│   └── HomeScreen.kt                # Feature card grid
├── mantra/                          # Mantra / Meditation feature
│   ├── model/
│   │   └── Mantra.kt                # Data model
│   ├── data/
│   │   └── MantraRepository.kt      # Audio asset catalogue
│   ├── service/
│   │   └── MantraPlayerService.kt   # Foreground MediaService
│   ├── viewmodel/
│   │   └── MantraViewModel.kt       # UI state + playback control
│   └── ui/
│       └── MantraScreen.kt          # Compose UI
└── ui/theme/                        # Material 3 theme
```

---

## Mantra / Meditation Feature — Implementation Plan

### Goal
Allow the user to select a mantra (or upload their own audio), press Play, and have the track loop indefinitely. Playback continues in the background with a persistent notification and survives screen lock.

---

### Phase 1 — Core Playback (MVP)

**Dependencies to add (`libs.versions.toml` + `build.gradle.kts`):**
```toml
[versions]
media3 = "1.4.1"
navigation-compose = "2.8.5"

[libraries]
media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
media3-session   = { group = "androidx.media3", name = "media3-session",   version.ref = "media3" }
media3-ui        = { group = "androidx.media3", name = "media3-ui",        version.ref = "media3" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }
```

**Manifest additions (`AndroidManifest.xml`):**
```xml
<!-- Foreground service for background audio -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />   <!-- API 33+ -->

<service
    android:name=".mantra.service.MantraPlayerService"
    android:foregroundServiceType="mediaPlayback"
    android:exported="false" />
```

**Audio assets:** Place `.mp3` / `.ogg` files in `app/src/main/res/raw/`.
- `mantra_om.mp3`
- `mantra_gayatri.mp3`
- `mantra_maha_mrityunjaya.mp3`

---

**Key classes:**

#### `Mantra.kt` — Data model
```kotlin
data class Mantra(
    val id: String,
    val title: String,
    val description: String,
    @RawRes val audioRes: Int,          // bundled asset
    val durationSeconds: Int? = null
)
```

#### `MantraRepository.kt` — Catalogue
```kotlin
object MantraRepository {
    fun getAll(): List<Mantra> = listOf(
        Mantra("om",        "Om",              "Universal mantra",     R.raw.mantra_om),
        Mantra("gayatri",   "Gayatri Mantra",  "Vedic solar mantra",   R.raw.mantra_gayatri),
        Mantra("mrityunjaya","Maha Mrityunjaya","Lord Shiva mantra",    R.raw.mantra_maha_mrityunjaya),
    )
}
```

#### `MantraPlayerService.kt` — Foreground Service
- Wraps **Media3 `ExoPlayer`** with `MediaSession`
- Configures `player.repeatMode = Player.REPEAT_MODE_ONE` for seamless looping
- Exposes `MediaSessionService` so system/Bluetooth controls work
- Starts in foreground with a media-style notification (play/pause action)
- Binds to `MantraViewModel` via `MediaController`

#### `MantraViewModel.kt` — UI State
```kotlin
data class MantraUiState(
    val mantras: List<Mantra> = emptyList(),
    val selected: Mantra? = null,
    val isPlaying: Boolean = false,
    val loopCount: Int = 0,           // how many times track has looped
    val timerMinutes: Int? = null,    // auto-stop after N minutes (optional)
)
```
- Connects to `MantraPlayerService` via `MediaController`
- Exposes `play()`, `pause()`, `stop()`, `select(mantra)`, `setTimer(minutes)`

#### `MantraScreen.kt` — Compose UI
```
┌────────────────────────────────┐
│  Mantra Player                 │
│                                │
│  ┌──────────────────────────┐  │
│  │  Om              ▶ 0:32  │  │  ← selected mantra card
│  └──────────────────────────┘  │
│                                │
│     [  ◀◀  ]  [ ▶/⏸ ]  [ ⏹ ]   │  ← transport controls
│                                │
│     Loop count: 12             │
│     Timer: 30 min ▼            │
│                                │
│  ── Mantra List ──────────── │
│  ○ Om                         │
│  ● Gayatri Mantra             │  ← radio-style selection
│  ○ Maha Mrityunjaya           │
└────────────────────────────────┘
```

---

### Phase 2 — UX Enhancements

- **Breathing animation:** Animate a pulsing mandala or expanding circle in sync with the mantra tempo.
- **Loop counter:** Show how many full loops have completed.
- **Sleep timer:** Auto-pause after user-selected duration (5 / 10 / 15 / 30 / 60 min). Timer countdown shown in notification.
- **Volume fade-out:** Gracefully fade volume when sleep timer expires.
- **Persistent notification:** Media-style notification with mantra title, artwork, and transport controls.

---

### Phase 3 — Custom Audio

- Let the user pick an audio file from device storage (`ActivityResultContracts.GetContent`).
- Store the URI in `DataStore` (preferences) so it persists across sessions.
- Display user-added tracks alongside bundled ones.

---

### Phase 4 — Additional App Features (future)

| Feature | Description |
|---|---|
| Journal | Daily gratitude / reflection prompts |
| Affirmations | Rotate positive affirmations as widget |
| Breath Timer | Guided box breathing / 4-7-8 patterns |
| Pomodoro | Focus timer with break reminders |
| Quotes | Sacred text / motivational quote of the day |

---

## Getting Started

1. Clone the repo
2. Open in Android Studio Ladybug (2024.2+)
3. Sync Gradle
4. Run on a device/emulator with API 33+

---

## Contributing

This is a personal project. Suggestions welcome via issues.

---

*"Aham Brahmasmi" — I am the universe.*
