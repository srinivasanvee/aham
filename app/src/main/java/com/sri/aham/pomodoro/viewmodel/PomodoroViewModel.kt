package com.sri.aham.pomodoro.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sri.aham.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PomodoroPhase { WORK, SHORT_BREAK, LONG_BREAK }

data class PomodoroUiState(
    val phase: PomodoroPhase = PomodoroPhase.WORK,
    val remainingSeconds: Long = 25 * 60,
    val totalSeconds: Long = 25 * 60,
    val isRunning: Boolean = false,
    val sessionsCompleted: Int = 0,
    val workMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val sessionsUntilLongBreak: Int = 4,
)

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    private val notificationManager =
        NotificationManagerCompat.from(application.applicationContext)

    companion object {
        private const val CHANNEL_ID = "pomodoro_channel"
        private const val NOTIF_ID = 1001
    }

    init {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Pomodoro Timer",
            NotificationManager.IMPORTANCE_HIGH,
        )
        notificationManager.createNotificationChannel(channel)
    }

    fun start() {
        if (_uiState.value.isRunning) return
        _uiState.update { it.copy(isRunning = true) }
        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0) {
                delay(1_000)
                _uiState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
            }
            onPhaseComplete()
        }
    }

    fun pause() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(isRunning = false) }
    }

    fun reset() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(isRunning = false, remainingSeconds = it.totalSeconds) }
    }

    fun skip() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(isRunning = false) }
        onPhaseComplete()
    }

    fun setWorkMinutes(minutes: Int) {
        if (_uiState.value.isRunning) return
        val clamped = minutes.coerceIn(1, 60)
        _uiState.update { state ->
            val newTotal = clamped * 60L
            state.copy(
                workMinutes = clamped,
                totalSeconds = if (state.phase == PomodoroPhase.WORK) newTotal else state.totalSeconds,
                remainingSeconds = if (state.phase == PomodoroPhase.WORK) newTotal else state.remainingSeconds,
            )
        }
    }

    fun setShortBreakMinutes(minutes: Int) {
        if (_uiState.value.isRunning) return
        val clamped = minutes.coerceIn(1, 30)
        _uiState.update { state ->
            val newTotal = clamped * 60L
            state.copy(
                shortBreakMinutes = clamped,
                totalSeconds = if (state.phase == PomodoroPhase.SHORT_BREAK) newTotal else state.totalSeconds,
                remainingSeconds = if (state.phase == PomodoroPhase.SHORT_BREAK) newTotal else state.remainingSeconds,
            )
        }
    }

    fun setLongBreakMinutes(minutes: Int) {
        if (_uiState.value.isRunning) return
        val clamped = minutes.coerceIn(1, 60)
        _uiState.update { state ->
            val newTotal = clamped * 60L
            state.copy(
                longBreakMinutes = clamped,
                totalSeconds = if (state.phase == PomodoroPhase.LONG_BREAK) newTotal else state.totalSeconds,
                remainingSeconds = if (state.phase == PomodoroPhase.LONG_BREAK) newTotal else state.remainingSeconds,
            )
        }
    }

    private fun onPhaseComplete() {
        val current = _uiState.value
        val (nextPhase, nextSessions) = when (current.phase) {
            PomodoroPhase.WORK -> {
                val newSessions = current.sessionsCompleted + 1
                val next = if (newSessions % current.sessionsUntilLongBreak == 0)
                    PomodoroPhase.LONG_BREAK else PomodoroPhase.SHORT_BREAK
                next to newSessions
            }
            PomodoroPhase.SHORT_BREAK, PomodoroPhase.LONG_BREAK ->
                PomodoroPhase.WORK to current.sessionsCompleted
        }

        val nextTotal = when (nextPhase) {
            PomodoroPhase.WORK -> current.workMinutes * 60L
            PomodoroPhase.SHORT_BREAK -> current.shortBreakMinutes * 60L
            PomodoroPhase.LONG_BREAK -> current.longBreakMinutes * 60L
        }

        _uiState.update {
            it.copy(
                phase = nextPhase,
                remainingSeconds = nextTotal,
                totalSeconds = nextTotal,
                isRunning = false,
                sessionsCompleted = nextSessions,
            )
        }

        postNotification(nextPhase)
    }

    private fun postNotification(nextPhase: PomodoroPhase) {
        val ctx: Context = getApplication()
        val text = when (nextPhase) {
            PomodoroPhase.WORK -> "Back to work! Stay focused."
            PomodoroPhase.SHORT_BREAK -> "Time for a short break! 🌿"
            PomodoroPhase.LONG_BREAK -> "Time for a long break! Rest well. 🌿"
        }
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Pomodoro")
            .setContentText(text)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(NOTIF_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted; silently skip
        }
    }
}
