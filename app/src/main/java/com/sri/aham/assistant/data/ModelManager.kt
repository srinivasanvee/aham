package com.sri.aham.assistant.data

import android.content.Context
import java.io.File

object ModelManager {

    /**
     * Qwen3-0.6B from litert-community HuggingFace org.
     * Publicly accessible — no authentication required.
     * Compatible with litertlm-android:0.10.0 (~0.8 GB download).
     *
     * NOTE: Switch to Gemma 4 once litertlm-android:0.10.1 lands on Google Maven —
     * Gemma 4 models require the new engine in 0.10.1+ (current: 0.10.0).
     *
     * Source: https://huggingface.co/litert-community/Qwen3-0.6B
     */
    const val MODEL_DOWNLOAD_URL: String =
        "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B.litertlm?download=true"

    const val MODEL_FILENAME = "Qwen3-0.6B.litertlm"

    fun modelFile(context: Context): File =
        File(context.filesDir, "models/$MODEL_FILENAME")

    fun isModelReady(context: Context): Boolean = modelFile(context).exists()
}
