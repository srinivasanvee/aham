package com.sri.aham.assistant.data

import android.content.Context
import java.io.File

object ModelManager {

    /**
     * Gemma 3 1B IT (INT4) from litert-community HuggingFace org.
     * Publicly accessible — no authentication required.
     * Officially recommended model for litertlm-android:0.10.0 (~584 MB).
     * Tokenizer embedded in the bundle (SentencePiece).
     *
     * NOTE: Switch to Gemma 4 once litertlm-android:0.10.1 lands on Google Maven.
     *
     * Source: https://huggingface.co/litert-community/Gemma3-1B-IT
     */
    const val MODEL_DOWNLOAD_URL: String =
        "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.litertlm?download=true"

    const val MODEL_FILENAME = "gemma3-1b-it-int4.litertlm"

    fun modelFile(context: Context): File =
        File(context.filesDir, "models/$MODEL_FILENAME")

    fun isModelReady(context: Context): Boolean = modelFile(context).exists()
}
