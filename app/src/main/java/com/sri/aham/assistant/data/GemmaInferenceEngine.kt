package com.sri.aham.assistant.data

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext

private const val SYSTEM_PROMPT =
    "You are Aham, a warm and mindful AI companion inside a personal wellness app. " +
    "The app features a Mantra/Meditation player and a Pomodoro focus timer. " +
    "Keep responses concise, grounded, and supportive."

class GemmaInferenceEngine(private val context: Context) : InferenceEngine {

    private var engine: Engine? = null
    private var conversation: com.google.ai.edge.litertlm.Conversation? = null

    override suspend fun initialize() = withContext(Dispatchers.IO) {
        val config = EngineConfig(
            modelPath = ModelManager.modelFile(context).absolutePath,
            backend = Backend.CPU(),
            cacheDir = context.cacheDir.absolutePath,
        )
        val e = Engine(config)
        e.initialize()
        engine = e
        conversation = e.createConversation(
            ConversationConfig(systemInstruction = Contents.of(SYSTEM_PROMPT))
        )
    }

    override fun generate(userMessage: String): Flow<String> = channelFlow {
        withContext(Dispatchers.IO) {
            val conv = checkNotNull(conversation) { "Engine not initialized" }
            conv.sendMessageAsync(Contents.of(userMessage))
                .collect { message ->
                    val token = message.contents.contents
                        .filterIsInstance<Content.Text>()
                        .joinToString("") { it.text }
                    if (token.isNotEmpty()) send(token)
                }
        }
    }

    override fun close() {
        conversation?.close()
        conversation = null
        engine?.close()
        engine = null
    }
}
