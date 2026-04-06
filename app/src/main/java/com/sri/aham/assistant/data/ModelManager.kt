package com.sri.aham.assistant.data

import android.content.Context
import java.io.File

object ModelManager {

    /**
     * Official Google-packaged Gemma 3 1B model in .litertlm format.
     * This is the correct format for MediaPipe LlmInference — it includes
     * the SentencePiece tokenizer embedded in the file (~700 MB).
     *
     * Source: https://huggingface.co/google/gemma-3-1b-it-litert-lm
     * Note: Requires accepting Google's Gemma terms of service on HuggingFace.
     * If the download is gated, push the model manually:
     *   adb push gemma3-1b-it-int4.litertlm /data/data/com.sri.aham/files/models/
     */
    const val MODEL_DOWNLOAD_URL: String =
        "https://huggingface.co/google/gemma-3-1b-it-litert-lm/resolve/main/gemma3-1b-it-int4.litertlm?download=true"

    const val MODEL_FILENAME = "gemma3-1b-it-int4.litertlm"

    fun modelFile(context: Context): File =
        File(context.filesDir, "models/$MODEL_FILENAME")

    fun isModelReady(context: Context): Boolean = modelFile(context).exists()
}
