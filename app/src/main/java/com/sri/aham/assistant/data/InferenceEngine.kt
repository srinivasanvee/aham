package com.sri.aham.assistant.data

import kotlinx.coroutines.flow.Flow

interface InferenceEngine {
    suspend fun initialize()
    fun generate(prompt: String): Flow<String>
    fun close()
}
