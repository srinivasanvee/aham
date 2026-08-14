# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires keystore.properties or env vars)
./gradlew bundleRelease

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.sri.aham.ExampleUnitTest"

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

## Architecture

**Single-activity MVVM** with Jetpack Compose and Navigation Compose. No Hilt (planned). No fragments.

**Navigation:** All routes defined in `navigation/AppNavGraph.kt` as `Route` object constants. Screens receive `onNavigate: (String) -> Unit` and don't reference other features' routes directly.

**Feature packages** are self-contained: `breathwork/`, `mantra/`, and `pomodoro/`. Each follows the relevant subset of `data/ → model/ → viewmodel/ → ui/` layering.

**State:** Each feature exposes a `UiState` data class via `StateFlow` from its ViewModel. ViewModels are created via the `viewModel()` composable (no DI).

## Feature Status

| Feature | Status |
|---|---|
| Mantra Player (looping, sleep timer, tabs, Om animation) | Complete |
| Pomodoro Timer (work/break cycles, notifications) | Complete |
| Breath Work (paced breathing, exhale hold timer, recovery hold) | Complete |

## Key Subsystems

### Mantra Player (`mantra/`)

`MantraPlayerService` extends `MediaSessionService` and owns the `ExoPlayer` instance. Audio is bundled in `res/raw/`. The ViewModel connects via `MediaController` + `SessionToken` inside a `DisposableEffect`. Sleep timer uses a coroutine with a 30-step volume fade. Loop count increments on `DISCONTINUITY_REASON_AUTO_TRANSITION`.

To add a new audio track: add the file to `res/raw/`, then add an entry to `MantraRepository` with the correct `Mantra.category`.

### Pomodoro (`pomodoro/`)

Self-contained timer state machine in `PomodoroViewModel`. Posts `NotificationCompat` notifications at phase transitions via a `NotificationChannel` created in `init`.

### Breath Work (`breathwork/`)

`BreathWorkSession` is a pure, unit-tested state machine for three rounds of paced breathing, exhale retention, and recovery retention. `BreathWorkViewModel` owns the one-second ticker, while `BreathWorkScreen` renders cues, timing, controls, and safety guidance.

## Release Pipeline

Tag `main` with a `v*` tag to trigger the GitHub Actions deploy workflow, which signs the AAB and uploads it to the Play Store internal track.

- `versionCode` comes from `GITHUB_RUN_NUMBER`; `versionName` from the Git tag (strip leading `v`)
- Signing reads from `keystore.properties` locally or GitHub Secrets in CI
- See `TODO.md` for one-time Play Store setup steps

## Dependencies of Note

- **`androidx.media3:*:1.4.1`** — ExoPlayer + MediaSession for background audio
- **`androidx.navigation:navigation-compose:2.8.5`** — Compose NavHost
- Compose BOM: `2024.09.00`
