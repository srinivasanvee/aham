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

### Mantra / Meditation Player ✅
Play sacred mantras or ambient meditation audio on seamless loop. Supports background playback via a foreground service so the audio continues while the user navigates away or locks the screen.

**Implemented (Phase 1 + 2):**
- Looped playback via ExoPlayer `REPEAT_MODE_ONE` in a foreground `MediaSessionService`
- Pulsing Om (ॐ) mandala animation — three concentric rings breathe with the chant
- Live loop counter — increments every time the track restarts
- Sleep timer — 5 / 10 / 15 / 30 / 60 min countdown with progress bar
- Volume fade-out — 3-second gradual fade before auto-stop at timer expiry
- Mantra selection list with animated row highlight
- Cross-fade title animation when switching mantras
- Persistent background notification with play/pause/stop controls (via Media3)
- Notification permission handled at manifest level (API 33+)

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

### Phase 1 — Core Playback ✅ Done

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

### Phase 2 — UX Enhancements ✅ Done

- ✅ **Breathing animation:** Three concentric rings pulse at 2 s / 2.5 s / 3 s offsets using `InfiniteTransition`. Freezes gracefully on pause.
- ✅ **Loop counter:** `Player.Listener.onPositionDiscontinuity` increments the counter on every loop restart.
- ✅ **Sleep timer:** Coroutine countdown (5 / 10 / 15 / 30 / 60 min) with `LinearProgressIndicator`.
- ✅ **Volume fade-out:** 30-step × 100 ms coroutine lowers `MediaController.volume` to 0 before auto-stop.
- ✅ **Persistent notification:** Managed automatically by `MediaSessionService` with play/pause/stop actions.

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
| **AI Assistant** | **Offline Gemma 4 voice assistant — see [ASSISTANT.md](ASSISTANT.md)** |

---

## CI / CD Pipeline

### Branching strategy
```
develop  ──►  PR  ──►  main  ──►  git tag v1.x.x  ──►  Play Store (internal track)
```
- All development happens on `develop` (or feature branches off it).
- A PR from `develop` → `main` triggers the **CI** workflow (tests + debug build).
- Merging does **not** auto-deploy — you control exactly when a release goes out.
- Pushing a version tag triggers the **Deploy** workflow.
- Promote to production manually from Play Console (or run `fastlane promote`).

### GitHub Actions workflows

| Workflow | File | Trigger | What it does |
|---|---|---|---|
| CI | `.github/workflows/ci.yml` | PR → `main` or `develop` | Unit tests + debug build |
| Deploy | `.github/workflows/deploy.yml` | `git push origin v*` (any version tag) | Signed AAB → internal track |

### One-time setup (do this before the first push to main)

**1. Create a release keystore**
```bash
keytool -genkey -v \
  -keystore ~/keys/aham-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias aham-key
```
Store it outside the repo — it must never be committed.

**2. Configure local signing**
```bash
cp keystore.properties.template keystore.properties
# Edit keystore.properties with your keystore path and passwords
```

**3. Create a Play Store service-account key**
- Play Console → Setup → API access → Link to a Google Cloud project
- Create a service account with **Release Manager** role
- Download the JSON key → save as `play-store-key.json` (gitignored)

**4. Add GitHub Secrets**
Go to: GitHub repo → Settings → Secrets and variables → Actions

```bash
# Encode and copy secrets to clipboard (macOS)
base64 -i ~/keys/aham-release.jks | pbcopy   # → KEYSTORE_BASE64
base64 -i play-store-key.json | pbcopy        # → PLAY_STORE_JSON_KEY_BASE64
```

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded `.jks` file |
| `KEYSTORE_PASSWORD` | Keystore store password |
| `KEY_ALIAS` | Key alias (`aham-key`) |
| `KEY_PASSWORD` | Key password |
| `PLAY_STORE_JSON_KEY_BASE64` | Base64-encoded service-account JSON |

**5. Install Fastlane locally** (optional, for running lanes from terminal)
```bash
gem install bundler
bundle install            # installs fastlane from Gemfile
bundle exec fastlane deploy  # test a local deploy
```

### Daily workflow
```bash
# Work on develop
git checkout develop
# ... make changes ...
git push origin develop

# Open PR → main in GitHub UI (CI runs automatically, must pass to merge)
# Merge PR when ready

# When you want to release — tag main and push
git checkout main
git pull
git tag v1.2.0
git push origin v1.2.0
# ↑ This alone triggers the deploy workflow → AAB lands in internal track

# When ready to ship to all users
bundle exec fastlane promote        # internal → production
# or use Play Console UI to roll out gradually
```

### versionCode and versionName
| Variable | Source | Example |
|---|---|---|
| `versionCode` | `GITHUB_RUN_NUMBER` (auto-increments, never touch manually) | `42` |
| `versionName` | Git tag with `v` prefix stripped | `v1.2.0` → `"1.2.0"` |

The AAB artifact uploaded to GitHub Actions is named `release-aab-1.2.0` for traceability.

---

## Getting Started

1. Clone the repo and switch to `develop`
   ```bash
   git checkout -b develop
   git push -u origin develop
   ```
2. Open in Android Studio Ladybug (2024.2+)
3. Sync Gradle
4. Run on a device/emulator with API 33+

---

## Contributing

This is a personal project. Suggestions welcome via issues.

---

*"Aham Brahmasmi" — I am the universe.*
