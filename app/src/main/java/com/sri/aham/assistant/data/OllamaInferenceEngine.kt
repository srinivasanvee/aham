package com.sri.aham.assistant.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val SYSTEM_PROMPT =
    "You are Aham, a warm and mindful AI companion inside a personal wellness app. " +
    "The app features a Mantra/Meditation player and a Pomodoro focus timer. " +
    "Keep responses concise, grounded, and supportive."

/**
 * Inference engine that calls a local Ollama instance via its chat API.
 * Uses ADB port reverse so Ollama stays private on the Mac (no LAN exposure):
 *
 *   adb reverse tcp:11434 tcp:11434
 *   ollama serve
 *   ollama pull gemma3:1b
 */
class OllamaInferenceEngine(
    private val baseUrl: String = "http://localhost:11434",
    private val model: String = "gemma3:1b",
) : InferenceEngine {

    private val history = mutableListOf<Pair<String, String>>() // role → content

    override suspend fun initialize() = withContext(Dispatchers.IO) {
        val conn = URL("$baseUrl/api/tags").openConnection() as HttpURLConnection
        conn.connectTimeout = 3_000
        conn.readTimeout = 3_000
        try {
            if (conn.responseCode != 200) throw IOException("Ollama returned HTTP ${conn.responseCode}")
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException(
                "Ollama not reachable. Run:\n  adb reverse tcp:11434 tcp:11434\n  ollama serve\n  ollama pull $model",
                e,
            )
        } finally {
            conn.disconnect()
        }
    }

    override fun generate(userMessage: String): Flow<String> = channelFlow {
        withContext(Dispatchers.IO) {
            // Build messages array with system prompt + history + new user message
            val messages = JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
                history.forEach { (role, content) ->
                    put(JSONObject().put("role", role).put("content", content))
                }
                put(JSONObject().put("role", "user").put("content", userMessage))
            }

            val body = JSONObject()
                .put("model", model)
                .put("messages", messages)
                .put("stream", true)
                .toString()

            val conn = URL("$baseUrl/api/chat").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10_000
            conn.readTimeout = 120_000
            conn.outputStream.use { it.write(body.toByteArray()) }

            val responseBuilder = StringBuilder()
            conn.inputStream.bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val l = line?.takeIf { it.isNotBlank() } ?: continue
                    val json = JSONObject(l)
                    val token = json.optJSONObject("message")?.optString("content", "") ?: ""
                    if (token.isNotEmpty()) {
                        responseBuilder.append(token)
                        send(token)
                    }
                    if (json.optBoolean("done")) break
                }
            }
            conn.disconnect()

            // Store exchange in history for context
            history.add("user" to userMessage)
            history.add("assistant" to responseBuilder.toString())
        }
    }

    override fun close() {
        history.clear()
    }
}
