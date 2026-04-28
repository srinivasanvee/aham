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

For AI assistant development on the emulator, set up Ollama instead of the on-device Gemma model:
```bash
adb reverse tcp:11434 tcp:11434
ollama serve
ollama pull gemma3:1b
```
Then set `USE_OLLAMA_ON_EMULATOR = true` in `AssistantViewModel.kt`.

## Architecture

**Single-activity MVVM** with Jetpack Compose and Navigation Compose. No Hilt (planned). No fragments.

**Navigation:** All routes defined in `navigation/AppNavGraph.kt` as `Route` object constants. Screens receive `onNavigate: (String) -> Unit` and don't reference other features' routes directly.

**Feature packages** are self-contained: `assistant/`, `mantra/`, `pomodoro/`. Each follows `data/ → model/ → viewmodel/ → ui/` layering.

**State:** Each feature exposes a `UiState` data class via `StateFlow` from its ViewModel. ViewModels are created via the `viewModel()` composable (no DI).

## Feature Status

| Feature | Status |
|---|---|
| Mantra Player (looping, sleep timer, tabs, Om animation) | Complete |
| Pomodoro Timer (work/break cycles, notifications) | Complete |
| AI Assistant — text chat (Gemma 3 1B INT4, download, streaming) | Complete |
| AI Assistant — STT (Phase 2), TTS (Phase 3), history/Room (Phase 4), intent control (Phase 5) | Planned — see `ASSISTANT.md` |

## Key Subsystems

### AI Assistant (`assistant/`)

`InferenceEngine` is an interface with three implementations selected in `AssistantViewModel`:
- **`GemmaInferenceEngine`** — production; uses LiteRT-LM SDK (Google AI Edge), model stored in `context.filesDir/models/`
- **`OllamaInferenceEngine`** — emulator dev; calls local Ollama via `adb reverse`
- **`MockInferenceEngine`** — emulator fallback; keyword-matched canned responses

Model download is ~584 MB from HuggingFace (`litert-community/Gemma3-1B-IT`). The `ModelSetupScreen` handles the first-run flow; `AssistantScreen` is shown only when `modelState == READY`.

ProGuard rules in `app/proguard-rules.pro` preserve LiteRT-LM and TensorFlow internals — don't remove them.

### Mantra Player (`mantra/`)

`MantraPlayerService` extends `MediaSessionService` and owns the `ExoPlayer` instance. Audio is bundled in `res/raw/`. The ViewModel connects via `MediaController` + `SessionToken` inside a `DisposableEffect`. Sleep timer uses a coroutine with a 30-step volume fade. Loop count increments on `DISCONTINUITY_REASON_AUTO_TRANSITION`.

To add a new audio track: add the file to `res/raw/`, then add an entry to `MantraRepository` with the correct `Mantra.category`.

### Pomodoro (`pomodoro/`)

Self-contained timer state machine in `PomodoroViewModel`. Posts `NotificationCompat` notifications at phase transitions via a `NotificationChannel` created in `init`.

## Release Pipeline

Tag `main` with a `v*` tag to trigger the GitHub Actions deploy workflow, which signs the AAB and uploads it to the Play Store internal track.

- `versionCode` comes from `GITHUB_RUN_NUMBER`; `versionName` from the Git tag (strip leading `v`)
- Signing reads from `keystore.properties` locally or GitHub Secrets in CI
- See `TODO.md` for one-time Play Store setup steps

## Dependencies of Note

- **`com.google.ai.edge:litertlm-android:0.10.0`** — on-device LLM inference; only `arm64-v8a` and `x86_64` ABIs are included
- **`androidx.media3:*:1.4.1`** — ExoPlayer + MediaSession for background audio
- **`androidx.navigation:navigation-compose:2.8.5`** — Compose NavHost
- Compose BOM: `2024.09.00`
