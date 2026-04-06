# Aham Assistant — On-Device AI with Voice

An offline-first AI assistant powered by **Gemma 4** (via Google AI Edge SDK) with a full voice pipeline (STT + TTS). The assistant is contextually aware of the Aham app — it can answer questions, guide meditation sessions, and eventually control app features through natural language.

---

## Architecture Overview

```
User Voice
    │
    ▼
[SpeechRecognizer]  ──►  text prompt
                              │
                              ▼
                    [AssistantViewModel]
                              │
                    ┌─────────┴──────────┐
                    ▼                    ▼
          [GemmaInferenceEngine]   [ConversationStore]
          (Google AI Edge SDK)     (DataStore / Room)
                    │
                    ▼
              LLM response text
                    │
          ┌─────────┴──────────┐
          ▼                    ▼
     [AssistantScreen]   [TextToSpeech]
     (Compose Chat UI)   (Android TTS)
```

**Key components:**
| Layer | Technology |
|---|---|
| LLM Inference | Google AI Edge SDK (`com.google.ai.edge.aicore` / MediaPipe Tasks GenAI) |
| Model | Gemma 4 1B or 2B INT4 quantized (`.task` format) |
| STT | Android `SpeechRecognizer` (offline mode) |
| TTS | Android `TextToSpeech` |
| UI | Jetpack Compose + Material 3 |
| State | `AssistantViewModel` (AndroidViewModel) |
| Persistence | `DataStore<Preferences>` for settings, Room for history (Phase 4+) |

---

## Phase 1 — LLM Foundation (Text-Only Chat)

**Goal:** Load Gemma 4 on-device and respond to typed messages.

### Dependencies

```toml
# libs.versions.toml
[versions]
ai-edge-genai = "0.4.0"        # Google AI Edge SDK (check for latest)

[libraries]
ai-edge-genai = { group = "com.google.mediapipe", name = "tasks-genai", version.ref = "ai-edge-genai" }
```

```kotlin
// app/build.gradle.kts
implementation(libs.ai.edge.genai)
```

### Model Download & Storage

Gemma 4 1B INT4 (~600 MB) is downloaded once on first launch and stored in internal storage. It is **not bundled** in the APK.

```kotlin
// assistant/data/ModelManager.kt
object ModelManager {
    const val MODEL_URL = "https://storage.googleapis.com/mediapipe-models/..." // official bucket
    const val MODEL_FILENAME = "gemma4-1b-it-int4.task"

    fun modelPath(context: Context): String =
        "${context.filesDir}/models/$MODEL_FILENAME"

    fun isModelReady(context: Context): Boolean =
        File(modelPath(context)).exists()
}
```

**Manifest — Internet permission (for initial download only):**
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### Key Classes

#### `GemmaInferenceEngine.kt`
```kotlin
// assistant/data/GemmaInferenceEngine.kt
class GemmaInferenceEngine(private val context: Context) {

    private var llmInference: LlmInference? = null

    fun initialize() {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(ModelManager.modelPath(context))
            .setMaxTokens(1024)
            .setTemperature(0.7f)
            .setTopK(40)
            .build()
        llmInference = LlmInference.createFromOptions(context, options)
    }

    suspend fun generate(prompt: String, onToken: (String) -> Unit) = withContext(Dispatchers.IO) {
        llmInference?.generateResponseAsync(prompt) { token, done ->
            onToken(token ?: "")
        }
    }

    fun close() { llmInference?.close() }
}
```

#### `AssistantViewModel.kt`
```kotlin
data class AssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val modelState: ModelState = ModelState.NotReady,
    val downloadProgress: Float = 0f,
)

enum class ModelState { NotReady, Downloading, Loading, Ready, Error }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,           // USER / ASSISTANT
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
)
```

#### `AssistantScreen.kt`
```
┌────────────────────────────────┐
│  Aham Assistant                │
│                                │
│  ┌──────────────────────────┐  │
│  │ [Assistant]              │  │
│  │ Hello! I'm here to       │  │
│  │ guide your practice.     │  │
│  └──────────────────────────┘  │
│  ┌──────────────────────────┐  │
│  │ [You]                    │  │
│  │ Play the Gayatri mantra  │  │
│  └──────────────────────────┘  │
│                                │
│  ┌──────────────────────────┐  │
│  │  Type a message...   [🎤]│  │  ← mic button added Phase 2
│  └──────────────────────────┘  │
└────────────────────────────────┘
```

### System Prompt

```kotlin
val SYSTEM_PROMPT = """
You are Aham, a mindful AI companion in a personal wellness app.
The app includes: a Mantra/Meditation player, a Pomodoro focus timer, and more features coming soon.
Keep responses warm, concise, and grounded in mindfulness. Avoid lengthy lists.
If the user asks to play a mantra, start a timer, or navigate the app, acknowledge it naturally.
""".trimIndent()
```

### File Structure

```
assistant/
├── data/
│   ├── GemmaInferenceEngine.kt   # LLM wrapper
│   ├── ModelManager.kt           # Download + path management
│   └── ModelDownloader.kt        # WorkManager-based download
├── model/
│   └── ChatMessage.kt            # Data class
├── viewmodel/
│   └── AssistantViewModel.kt     # UI state + inference
└── ui/
    ├── AssistantScreen.kt        # Chat UI
    ├── MessageBubble.kt          # Composable for each message
    └── ModelSetupScreen.kt       # First-run download/loading UI
```

### Manifest Additions

```xml
<!-- Background model download -->
<uses-permission android:name="android.permission.INTERNET" />
```

---

## Phase 2 — Voice Input (STT)

**Goal:** User can speak instead of type. Mic button activates offline speech recognition.

### Key Details

- Uses Android `SpeechRecognizer` with `EXTRA_PREFER_OFFLINE = true`
- Falls back to online recognition if offline engine unavailable
- Mic permission requested at runtime (Android 13+)
- Waveform animation while listening

### Additions

#### `VoiceInputManager.kt`
```kotlin
// assistant/data/VoiceInputManager.kt
class VoiceInputManager(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    fun startListening(onResult: (String) -> Unit, onError: (Int) -> Unit) {
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer?.setRecognitionListener(SimpleRecognitionListener(onResult, onError))
        recognizer?.startListening(intent)
    }

    fun stopListening() { recognizer?.stopListening() }
    fun destroy() { recognizer?.destroy() }
}
```

**Manifest:**
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

**UiState additions:**
```kotlin
val isListening: Boolean = false,
val partialSpeech: String = "",   // shown live while user speaks
```

**UI:** Mic button pulses (scale animation) while listening. Partial transcript shown in text field in real time. Tapping mic again stops listening and submits.

---

## Phase 3 — Voice Output (TTS)

**Goal:** Assistant speaks its responses using Android TTS.

### Key Details

- Android `TextToSpeech` with language set to `Locale.US` (or user preference)
- TTS starts speaking tokens as they stream in (sentence-by-sentence for natural rhythm)
- User can tap a speaker icon to toggle voice responses on/off
- Setting persisted in `DataStore`

### Additions

#### `TtsManager.kt`
```kotlin
// assistant/data/TtsManager.kt
class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            isReady = true
        }
    }

    fun speak(text: String) {
        if (!isReady) return
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, UUID.randomUUID().toString())
    }

    fun stop() { tts.stop() }
    fun shutdown() { tts.shutdown() }
}
```

**Streaming TTS strategy:** Buffer LLM tokens until a sentence-ending punctuation (`.`, `?`, `!`) is reached, then pass the sentence to `speak()`. This avoids choppy single-word utterances.

**UiState additions:**
```kotlin
val voiceResponseEnabled: Boolean = true,
val isSpeaking: Boolean = false,
```

**Fully hands-free mode:** When `isListening` ends and the response is complete, automatically restart listening — enabling a continuous conversation loop without touching the screen.

---

## Phase 4 — Context, Personality & Conversation History

**Goal:** The assistant remembers the conversation, understands the user's practice context, and gives consistent, personalized responses.

### Conversation History

- Sliding window of last N messages included in each prompt (avoid context overflow)
- Formatted as `<start_of_turn>user\n...\n<end_of_turn>` (Gemma instruction format)
- Full history persisted in **Room database** between app sessions

```kotlin
// Gemma instruction format
fun buildPrompt(history: List<ChatMessage>, newUserMessage: String): String {
    val sb = StringBuilder("<bos>")
    sb.append("<start_of_turn>system\n$SYSTEM_PROMPT\n<end_of_turn>\n")
    history.takeLast(10).forEach { msg ->
        val role = if (msg.role == Role.USER) "user" else "model"
        sb.append("<start_of_turn>$role\n${msg.text}\n<end_of_turn>\n")
    }
    sb.append("<start_of_turn>user\n$newUserMessage\n<end_of_turn>\n")
    sb.append("<start_of_turn>model\n")
    return sb.toString()
}
```

### Room Database

```kotlin
// assistant/data/db/ChatMessageEntity.kt + ChatDao.kt + AssistantDatabase.kt
@Entity("chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val role: String,
    val text: String,
    val timestamp: Long,
    val sessionId: String,
)
```

### New Dependencies

```toml
[versions]
room = "2.6.1"

[libraries]
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx      = { group = "androidx.room", name = "room-ktx",     version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
```

---

## Phase 5 — Deep App Integration (Intent Control)

**Goal:** The assistant can actually control app features through natural language.

### Intent Actions

```kotlin
sealed class AssistantIntent {
    data class PlayMantra(val name: String?) : AssistantIntent()
    object StopMantra : AssistantIntent()
    data class StartPomodoro(val minutes: Int?) : AssistantIntent()
    data class SetSleepTimer(val minutes: Int) : AssistantIntent()
    object NavigateHome : AssistantIntent()
    object None : AssistantIntent()        // plain conversation
}
```

### Intent Detection Strategy

After each LLM response, run a lightweight regex/keyword pass to detect intents. If the model is instructed in the system prompt to emit structured tags (e.g., `[ACTION:PLAY_MANTRA name=Gayatri]`), parse those tags before rendering the message to the user.

```kotlin
// assistant/data/IntentParser.kt
fun parseIntent(response: String): Pair<AssistantIntent, String> {
    val actionRegex = Regex("""\[ACTION:(\w+)(?:\s+(\w+=\S+))*\]""")
    val match = actionRegex.find(response) ?: return Pair(AssistantIntent.None, response)
    val cleanResponse = response.replace(match.value, "").trim()
    return when (match.groupValues[1]) {
        "PLAY_MANTRA"     -> Pair(AssistantIntent.PlayMantra(/* parse name */), cleanResponse)
        "STOP_MANTRA"     -> Pair(AssistantIntent.StopMantra, cleanResponse)
        "START_POMODORO"  -> Pair(AssistantIntent.StartPomodoro(/* parse minutes */), cleanResponse)
        else              -> Pair(AssistantIntent.None, cleanResponse)
    }
}
```

The `AssistantViewModel` dispatches intents to the relevant ViewModels via a shared `AppIntentHandler` (or via `SharedFlow` on a top-level `AppViewModel`).

---

## Phase Summary

| Phase | Feature | Key Work |
|---|---|---|
| 1 | Text chat + Gemma 4 on-device | AI Edge SDK, model download, chat UI |
| 2 | Voice input (STT) | `SpeechRecognizer`, mic permission, waveform animation |
| 3 | Voice output (TTS) | `TextToSpeech`, streaming sentence buffer, hands-free mode |
| 4 | Conversation history + personality | Room DB, Gemma prompt format, session memory |
| 5 | App intent control | Intent parsing, cross-feature ViewModel dispatch |

---

## Storage & Size Estimates

| Asset | Size |
|---|---|
| Gemma 4 1B INT4 model | ~600 MB |
| Gemma 4 2B INT4 model | ~1.3 GB |
| Room DB (conversation history) | < 5 MB |

The model is downloaded once via `WorkManager` on first launch (or on explicit user action) and cached in `context.filesDir/models/`. A setup screen shows download progress and storage requirements upfront.

---

## Hardware Requirements

| Requirement | Minimum |
|---|---|
| Android | 13+ (minSdk 33 — already met) |
| RAM | 4 GB (6 GB recommended for 2B model) |
| Storage | 1 GB free |
| GPU/NPU | Delegates to GPU if available, fallback to CPU |

---

## Notes

- Gemma 4 uses the `<start_of_turn>` / `<end_of_turn>` instruction format — different from Gemma 1.x.
- The Google AI Edge SDK handles GPU/NPU delegation automatically; no manual delegate config needed.
- If the device lacks sufficient RAM, gracefully degrade to a smaller model or show a clear "unsupported device" message.
- The model download is gated behind a "Set up Assistant" opt-in screen — never downloaded silently.
