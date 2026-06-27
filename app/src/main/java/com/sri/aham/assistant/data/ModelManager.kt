package com.sri.aham.assistant.data

import android.content.Context
import java.io.File

object ModelManager {

    /**
     * Gemma 4 1B IT (INT4) from litert-community on HuggingFace.
     * Requires: (1) Accept the Gemma license at huggingface.co/litert-community/Gemma4-1B-IT
     *           (2) Set HF_TOKEN below to a HuggingFace token with read access.
     * File size: ~600 MB. Compatible with litertlm-android:0.13.1.
     *
     * Alternative — push manually without a token:
     *   adb push gemma4-1b-it-int4.litertlm /data/data/com.sri.aham/files/models/
     */
    const val MODEL_DOWNLOAD_URL: String =
        "https://huggingface.co/litert-community/Gemma4-1B-IT/resolve/main/gemma4-1b-it-int4.litertlm?download=true"

    const val MODEL_FILENAME = "gemma4-1b-it-int4.litertlm"

    /**
     * HuggingFace token for downloading the gated Gemma 4 model.
     * Get one at https://huggingface.co/settings/tokens (read access is enough).
     * Leave empty to use the adb push approach instead.
     */
    const val HF_TOKEN = ""

    fun modelFile(context: Context): File =
        File(context.filesDir, "models/$MODEL_FILENAME")

    fun isModelReady(context: Context): Boolean = modelFile(context).exists()
}
