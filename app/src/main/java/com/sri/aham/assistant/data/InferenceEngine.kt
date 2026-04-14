package com.sri.aham.assistant.data

import kotlinx.coroutines.flow.Flow

interface InferenceEngine {
    suspend fun initialize()
    /** Send [userMessage] and stream back response tokens. */
    fun generate(userMessage: String): Flow<String>
    fun close()
}
