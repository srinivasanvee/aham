package com.sri.aham.assistant.data

import android.content.Context
import java.io.File

object ModelManager {

    /**
     * Direct download URL for the Gemma 4 model (.litertlm format).
     *
     * How to obtain:
     *  1. Visit kaggle.com/models/google/gemma — sign in (free) and accept the license.
     *  2. Filter framework → "LiteRT", pick "gemma4-1b-it-int4" (smallest, ~600 MB).
     *  3. Download the .litertlm file.
     *  4. Host it somewhere with a direct link (GitHub Release asset, GCS bucket, etc.)
     *     and paste the URL below.
     *
     * Dev shortcut (no hosting needed):
     *   adb push gemma4-1b-it-int4.litertlm /data/data/com.sri.aham/files/models/
     */
    const val MODEL_DOWNLOAD_URL: String =
        "https://huggingface.co/huggingworld/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm?download=true"

    const val MODEL_FILENAME = "gemma-4-E4B-it.litertlm"

    fun modelFile(context: Context): File =
        File(context.filesDir, "models/$MODEL_FILENAME")

    fun isModelReady(context: Context): Boolean = modelFile(context).exists()
}
