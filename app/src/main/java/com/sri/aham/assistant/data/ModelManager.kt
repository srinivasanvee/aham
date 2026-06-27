package com.sri.aham.assistant.data

import android.content.Context
import java.io.File

object ModelManager {

    /**
     * Gemma 4 E2B IT from litert-community on HuggingFace.
     * Requires accepting the Gemma license at huggingface.co/litert-community/gemma-4-E2B-it-litert-lm
     * and a HuggingFace token (entered in the app's setup screen).
     * File size: ~2.6 GB. Compatible with litertlm-android:0.13.1.
     */
    const val MODEL_DOWNLOAD_URL: String =
        "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm?download=true"

    const val MODEL_FILENAME = "gemma-4-E2B-it.litertlm"

    private const val PREFS_NAME = "assistant_prefs"
    private const val KEY_HF_TOKEN = "hf_token"

    fun modelFile(context: Context): File =
        File(context.filesDir, "models/$MODEL_FILENAME")

    fun isModelReady(context: Context): Boolean = modelFile(context).exists()

    fun getHfToken(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_HF_TOKEN, "") ?: ""

    fun saveHfToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HF_TOKEN, token)
            .apply()
    }
}
