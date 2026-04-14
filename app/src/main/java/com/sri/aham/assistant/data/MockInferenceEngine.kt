package com.sri.aham.assistant.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Emulator-safe inference engine that streams canned responses.
 * Used automatically on emulators where SME2/XNNPACK instructions are unavailable.
 */
class MockInferenceEngine : InferenceEngine {

    override suspend fun initialize() {
        delay(800) // simulate model load time
    }

    override fun generate(userMessage: String): Flow<String> = flow {
        val response = pickResponse(userMessage)
        for (word in response.split(" ")) {
            emit("$word ")
            delay(60)
        }
    }

    override fun close() = Unit

    private fun pickResponse(userMessage: String): String {
        val lower = userMessage.lowercase()
        return when {
            lower.contains("mantra") || lower.contains("meditation") ->
                "The Gayatri mantra is a beautiful choice for morning practice. " +
                "You can find it in the Mantra Player — simply tap the card on the home screen to begin your session."
            lower.contains("pomodoro") || lower.contains("focus") || lower.contains("timer") ->
                "A focused work session can do wonders for clarity. " +
                "Head to the Pomodoro timer from the home screen, set your intention, and begin. " +
                "I'll be here when you're done."
            lower.contains("hello") || lower.contains("hi") || lower.contains("namaste") ->
                "Namaste! I'm your Aham companion. How can I support your practice today?"
            lower.contains("help") ->
                "I can help you with your meditation practice, suggest mantras, " +
                "or guide you through a focus session. What would you like to explore?"
            else ->
                "That's a wonderful reflection. Taking a moment to pause and breathe can bring " +
                "great clarity. Is there something specific about your practice I can help with today?"
        }
    }
}
