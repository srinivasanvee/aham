package com.sri.aham.assistant.viewmodel

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sri.aham.assistant.data.GemmaInferenceEngine
import com.sri.aham.assistant.data.InferenceEngine
import com.sri.aham.assistant.data.MockInferenceEngine
import com.sri.aham.assistant.data.OllamaInferenceEngine
import com.sri.aham.assistant.data.ModelManager
import com.sri.aham.assistant.model.ChatMessage
import com.sri.aham.assistant.model.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Set to true to use Ollama (via adb reverse) instead of MockInferenceEngine on emulator. */
private const val USE_OLLAMA_ON_EMULATOR = true

private fun isEmulator(): Boolean =
    Build.FINGERPRINT.startsWith("generic") ||
    Build.FINGERPRINT.contains("emulator") ||
    Build.MODEL.contains("Emulator") ||
    Build.MODEL.contains("Android SDK") ||
    Build.MANUFACTURER.contains("Genymotion") ||
    Build.BRAND.startsWith("generic") ||
    Build.PRODUCT.contains("sdk")

enum class ModelState { NOT_READY, DOWNLOADING, LOADING, READY, ERROR }

data class AssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isGenerating: Boolean = false,
    val modelState: ModelState = ModelState.NOT_READY,
    val downloadProgress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val errorMessage: String? = null,
)

class AssistantViewModel(app: Application) : AndroidViewModel(app) {

    private val engine: InferenceEngine = when {
        !isEmulator()          -> GemmaInferenceEngine(app)
        USE_OLLAMA_ON_EMULATOR -> OllamaInferenceEngine()
        else                   -> MockInferenceEngine()
    }
    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    init {
        if (isEmulator() || ModelManager.isModelReady(app)) loadModel()
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty() || _uiState.value.isGenerating) return
        if (_uiState.value.modelState != ModelState.READY) return

        // Snapshot history before the new exchange so the prompt is consistent
        val historySnapshot = _uiState.value.messages

        val userMsg = ChatMessage(role = MessageRole.USER, text = text)
        val placeholder = ChatMessage(role = MessageRole.ASSISTANT, text = "", isStreaming = true)

        _uiState.update {
            it.copy(
                messages = it.messages + userMsg + placeholder,
                inputText = "",
                isGenerating = true,
            )
        }

        viewModelScope.launch {
            val prompt = buildPrompt(historySnapshot, text)
            var accumulated = ""

            engine.generate(prompt)
                .catch { e ->
                    updateLastMessage("Sorry, something went wrong: ${e.message}", streaming = false)
                    _uiState.update { it.copy(isGenerating = false) }
                }
                .collect { token ->
                    accumulated += token
                    updateLastMessage(accumulated, streaming = true)
                }

            updateLastMessage(accumulated, streaming = false)
            _uiState.update { it.copy(isGenerating = false) }
        }
    }

    fun downloadModel() {
        val url = ModelManager.MODEL_DOWNLOAD_URL
        if (url.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = "No download URL configured. Push the model file manually via adb (see ModelManager.kt).",
                )
            }
            return
        }

        _uiState.update {
            it.copy(modelState = ModelState.DOWNLOADING, downloadProgress = 0f, downloadedBytes = 0L, totalBytes = 0L)
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val modelFile = ModelManager.modelFile(getApplication())
                modelFile.parentFile?.mkdirs()

                // Follow redirects (HuggingFace redirects to CDN)
                var connection = URL(url).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connect()
                var redirects = 0
                while (connection.responseCode in 300..399 && redirects < 5) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    connection = URL(location).openConnection() as HttpURLConnection
                    connection.instanceFollowRedirects = true
                    connection.connect()
                    redirects++
                }

                // Validate response — gated HuggingFace models return 401/403 with HTML body
                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    connection.disconnect()
                    val hint = when (responseCode) {
                        401, 403 -> "The model is gated. Accept the Gemma license on huggingface.co/google/gemma-3-1b-it-litert-lm, then retry. " +
                            "Or push the file manually: adb push gemma3-1b-it-int4.litertlm /data/data/com.sri.aham/files/models/"
                        404 -> "Model file not found at the download URL."
                        else -> "Server returned HTTP $responseCode."
                    }
                    throw IOException(hint)
                }

                // Reject HTML responses (auth redirect masquerading as 200)
                val contentType = connection.contentType ?: ""
                if (contentType.startsWith("text/")) {
                    connection.disconnect()
                    throw IOException(
                        "Download returned HTML instead of a model file — the model may be gated. " +
                        "Accept the Gemma license on huggingface.co then retry, or push the file via adb."
                    )
                }

                val total = connection.contentLengthLong
                var downloaded = 0L
                var lastReportedProgress = -1

                connection.inputStream.use { input ->
                    FileOutputStream(modelFile).use { output ->
                        val buffer = ByteArray(64 * 1024) // 64 KB
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            // Throttle: update UI every 1% (or every 10 MB when total unknown)
                            val progressPct = if (total > 0) ((downloaded * 100) / total).toInt() else -1
                            val shouldUpdate = if (total > 0) {
                                progressPct != lastReportedProgress
                            } else {
                                downloaded % (10 * 1024 * 1024) < (64 * 1024)
                            }
                            if (shouldUpdate) {
                                lastReportedProgress = progressPct
                                _uiState.update {
                                    it.copy(
                                        downloadProgress = if (total > 0) downloaded.toFloat() / total else 0f,
                                        downloadedBytes = downloaded,
                                        totalBytes = total,
                                    )
                                }
                            }
                        }
                    }
                }
                loadModel()
            } catch (e: Exception) {
                // Delete partial/corrupt file so next attempt starts fresh
                ModelManager.modelFile(getApplication()).delete()
                _uiState.update {
                    it.copy(
                        modelState = ModelState.NOT_READY,
                        errorMessage = "Download failed: ${e.message}",
                    )
                }
            }
        }
    }

    fun loadModel() {
        _uiState.update { it.copy(modelState = ModelState.LOADING) }
        viewModelScope.launch {
            try {
                engine.initialize()
                val greeting = ChatMessage(
                    role = MessageRole.ASSISTANT,
                    text = "Namaste! I'm your Aham companion. How can I support your practice today?",
                )
                _uiState.update { it.copy(modelState = ModelState.READY, messages = listOf(greeting)) }
            } catch (e: UnsatisfiedLinkError) {
                _uiState.update {
                    it.copy(
                        modelState = ModelState.ERROR,
                        errorMessage = "Model runtime not supported on this device/emulator. Try running on a physical ARM device.",
                    )
                }
            } catch (e: Exception) {
                val message = when {
                    e.message?.contains("SIGILL", ignoreCase = true) == true ||
                    e.message?.contains("illegal instruction", ignoreCase = true) == true ->
                        "CPU not supported. Please run on a physical device."
                    else -> "Failed to load model: ${e.message}"
                }
                _uiState.update { it.copy(modelState = ModelState.ERROR, errorMessage = message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        engine.close()
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun updateLastMessage(text: String, streaming: Boolean) {
        _uiState.update { state ->
            val msgs = state.messages.toMutableList()
            if (msgs.isNotEmpty()) {
                msgs[msgs.lastIndex] = msgs[msgs.lastIndex].copy(text = text, isStreaming = streaming)
            }
            state.copy(messages = msgs)
        }
    }
}

// ---------------------------------------------------------------------------
// Prompt building
// ---------------------------------------------------------------------------

private const val SYSTEM_PROMPT = """You are Aham, a warm and mindful AI companion inside a personal wellness app.
The app features: a Mantra/Meditation player for looping sacred mantras, and a Pomodoro focus timer.
Keep responses concise, grounded, and supportive. Avoid bullet lists unless specifically asked.
When the user asks to play a mantra or start a timer, acknowledge it naturally and let them know you've noted it."""

fun buildPrompt(history: List<ChatMessage>, userMessage: String): String = buildString {
    append("<bos>")
    append("<start_of_turn>system\n$SYSTEM_PROMPT\n<end_of_turn>\n")
    history.takeLast(10).forEach { msg ->
        val role = if (msg.role == MessageRole.USER) "user" else "model"
        append("<start_of_turn>$role\n${msg.text}\n<end_of_turn>\n")
    }
    append("<start_of_turn>user\n$userMessage\n<end_of_turn>\n")
    append("<start_of_turn>model\n")
}
