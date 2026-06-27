package com.sri.aham.assistant.data

import android.content.Context
import java.io.File

object ModelManager {

    /**
     * Gemma 4 E2B IT from litert-community on HuggingFace.
     * Requires: (1) Accept the Gemma license at huggingface.co/litert-community/gemma-4-E2B-it-litert-lm
     *           (2) Set HF_TOKEN below to a HuggingFace token with read access.
     * File size: ~2.6 GB. Compatible with litertlm-android:0.13.1.
     *
     * Alternative — push manually without a token:
     *   adb push gemma-4-E2B-it.litertlm /data/data/com.sri.aham/files/models/
     */
    const val MODEL_DOWNLOAD_URL: String =
        "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm?download=true"

    const val MODEL_FILENAME = "gemma-4-E2B-it.litertlm"

    /**
     * HuggingFace token for downloading the gated Gemma 4 model.
     * Get one at https://huggingface.co/settings/tokens (read access is enough).
     * Leave empty to use the adb push approach instead.
     */
    const val HF_TOKEN = "" // Set your HuggingFace token here locally (do not commit)

    fun modelFile(context: Context): File =
        File(context.filesDir, "models/$MODEL_FILENAME")

    fun isModelReady(context: Context): Boolean = modelFile(context).exists()
}
