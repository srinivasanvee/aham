package com.sri.aham.assistant.data

import android.content.Context
import java.io.File

object ModelManager {

    /**
     * Gemma 4 E2B (2B parameters) from the official litert-community HuggingFace org.
     * Publicly accessible — no authentication or license token required.
     * Uses the LiteRT-LM format (.litertlm) with tokenizer embedded (~2.6 GB).
     *
     * Source: https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm
     */
    const val MODEL_DOWNLOAD_URL: String =
        "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm?download=true"

    const val MODEL_FILENAME = "gemma-4-E2B-it.litertlm"

    fun modelFile(context: Context): File =
        File(context.filesDir, "models/$MODEL_FILENAME")

    fun isModelReady(context: Context): Boolean = modelFile(context).exists()
}
