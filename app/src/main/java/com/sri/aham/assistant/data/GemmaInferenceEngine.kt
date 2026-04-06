package com.sri.aham.assistant.data

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

class GemmaInferenceEngine(private val context: Context) : InferenceEngine {

    private var lm: LlmInference? = null

    /**
     * Loads the model from internal storage. Blocking — call on a background thread.
     * maxTopK must be >= the topK used in session options.
     */
    override suspend fun initialize() = withContext(Dispatchers.IO) {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(ModelManager.modelFile(context).absolutePath)
            .setMaxTokens(1024)
            .setMaxTopK(40)
            .setPreferredBackend(LlmInference.Backend.CPU)
            .build()
        lm = LlmInference.createFromOptions(context, options)
    }

    /**
     * Streams response tokens for [prompt] as a [Flow].
     * Creates a fresh session per call so context doesn't bleed between turns
     * (history is embedded in the prompt itself via [buildPrompt]).
     */
    override fun generate(prompt: String): Flow<String> = callbackFlow {
        val inference = checkNotNull(lm) { "Engine not initialized — call initialize() first" }

        val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTopK(40)
            .setTemperature(0.7f)
            .setRandomSeed(101)
            .build()

        val session = LlmInferenceSession.createFromOptions(inference, sessionOptions)
        session.addQueryChunk(prompt)

        session.generateResponseAsync(ProgressListener<String> { partial, done ->
            partial?.let { trySend(it) }
            if (done) {
                session.close()
                close()
            }
        })

        awaitClose { session.close() }
    }

    override fun close() {
        lm?.close()
        lm = null
    }
}
