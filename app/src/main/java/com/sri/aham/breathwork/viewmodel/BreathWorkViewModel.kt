package com.sri.aham.breathwork.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sri.aham.breathwork.model.BreathWorkSession
import com.sri.aham.breathwork.model.BreathWorkState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BreathWorkViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("breath_work", 0)
    private val session = BreathWorkSession()
    private val _uiState = MutableStateFlow(BreathWorkState(
        breathsPerRound = preferences.getInt("breaths", 30).coerceIn(5, 60),
        totalRounds = preferences.getInt("rounds", 3).coerceIn(1, 5),
        cueSeconds = preferences.getInt("pace", 2).coerceIn(2, 4),
    ))
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
        _uiState.value = session.reset(_uiState.value)
    }

    fun setBreathsPerRound(breaths: Int) {
        _uiState.value = session.setBreathsPerRound(_uiState.value, breaths)
        saveSettings()
    }

    fun configure(rounds: Int, cueSeconds: Int) {
        _uiState.value = session.configure(_uiState.value, rounds, cueSeconds)
        saveSettings()
    }

    private fun saveSettings() {
        preferences.edit().putInt("breaths", _uiState.value.breathsPerRound)
            .putInt("rounds", _uiState.value.totalRounds)
            .putInt("pace", _uiState.value.cueSeconds).apply()
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
