package com.sri.aham.breathwork.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sri.aham.breathwork.model.BreathWorkSession
import com.sri.aham.breathwork.model.BreathWorkState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BreathWorkViewModel : ViewModel() {
    private val session = BreathWorkSession()
    private val _uiState = MutableStateFlow(BreathWorkState())
    val uiState: StateFlow<BreathWorkState> = _uiState.asStateFlow()
    private var timerJob: Job? = null

    fun start() {
        _uiState.value = session.start(_uiState.value)
        startTicker()
    }

    fun pause() {
        timerJob?.cancel()
        timerJob = null
        _uiState.value = session.pause(_uiState.value)
    }

    fun reset() {
        timerJob?.cancel()
        timerJob = null
        _uiState.value = session.reset()
    }

    fun finishBreathing() {
        _uiState.value = session.finishBreathing(_uiState.value)
        startTicker()
    }

    fun releaseHold() {
        _uiState.value = session.releaseHold(_uiState.value)
        startTicker()
    }

    private fun startTicker() {
        if (!_uiState.value.isRunning || timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (_uiState.value.isRunning) {
                delay(1_000)
                _uiState.value = session.tick(_uiState.value)
            }
            timerJob = null
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
