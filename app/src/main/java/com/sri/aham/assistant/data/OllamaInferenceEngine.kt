package com.sri.aham.assistant.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Inference engine that calls a local Ollama instance.
 * Designed for emulator testing via ADB port reverse:
 *
 *   adb reverse tcp:11434 tcp:11434
 *
 * Then start Ollama normally on your Mac (no need to expose on LAN):
 *   ollama serve
 *   ollama pull gemma3:1b
 */
class OllamaInferenceEngine(
    private val baseUrl: String = "http://localhost:11434",
    private val model: String = "gemma3:1b",
) : InferenceEngine {

    override suspend fun initialize() = withContext(Dispatchers.IO) {
        val conn = URL("$baseUrl/api/tags").openConnection() as HttpURLConnection
        conn.connectTimeout = 3_000
        conn.readTimeout = 3_000
        try {
            if (conn.responseCode != 200) {
                throw IOException("Ollama returned HTTP ${conn.responseCode}")
            }
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException(
                "Ollama not reachable. Run these commands once:\n" +
                "  adb reverse tcp:11434 tcp:11434\n" +
                "  ollama serve  (on your Mac)\n" +
                "  ollama pull $model",
                e,
            )
        } finally {
            conn.disconnect()
        }
    }

    override fun generate(prompt: String): Flow<String> = channelFlow {
        withContext(Dispatchers.IO) {
            val conn = URL("$baseUrl/api/generate").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10_000
            conn.readTimeout = 120_000

            val body = """{"model":${JSONObject.quote(model)},"prompt":${JSONObject.quote(prompt)},"stream":true}"""
            conn.outputStream.use { it.write(body.toByteArray()) }

            conn.inputStream.bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val l = line?.takeIf { it.isNotBlank() } ?: continue
                    val json = JSONObject(l)
                    val token = json.optString("response", "")
                    if (token.isNotEmpty()) send(token)
                    if (json.optBoolean("done")) break
                }
            }
            conn.disconnect()
        }
    }

    override fun close() = Unit
}
