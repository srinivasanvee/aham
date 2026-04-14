package com.sri.aham.mantra.viewmodel

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.sri.aham.mantra.data.MantraRepository
import com.sri.aham.mantra.model.Mantra
import com.sri.aham.mantra.model.MantraCategory
import com.sri.aham.mantra.service.MantraPlayerService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// UI state
// ---------------------------------------------------------------------------

/**
 * Complete UI state for the Mantra / Meditation screen.
 *
 * @param mantras                All available mantras from [MantraRepository].
 * @param selected               Currently loaded mantra; null when none chosen.
 * @param isPlaying              True while audio is actively producing sound.
 * @param isReady                True once the [MediaController] handshake completes.
 *                               Transport buttons are disabled until this is true.
 * @param loopCount              How many full loops the current track has completed.
 *                               Resets to 0 when a new mantra is selected.
 * @param timerMinutes           The duration selected for the sleep timer (null = off).
 * @param timerRemainingSeconds  Countdown value in seconds; null when timer is not running.
 * @param isFadingOut            True during the ~3-second volume fade before auto-stop.
 */
data class MantraUiState(
    val allMantras: List<Mantra> = emptyList(),
    val selectedTab: MantraCategory = MantraCategory.GUIDED,
    val selected: Mantra? = null,
    val isPlaying: Boolean = false,
    val isReady: Boolean = false,
    val loopCount: Int = 0,
    val timerMinutes: Int? = null,
    val timerRemainingSeconds: Long? = null,
    val isFadingOut: Boolean = false,
) {
    val mantras: List<Mantra> get() = allMantras.filter { it.category == selectedTab }
}

/** Available sleep-timer durations. null means "no timer". */
val SleepTimerOptions = listOf(null, 5, 10, 15, 30, 60)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

/**
 * ViewModel for the Mantra / Meditation screen.
 *
 * ## Responsibilities
 * - Manages the [MediaController] connection to [MantraPlayerService].
 * - Exposes [MantraUiState] as a [StateFlow] driven by real player events.
 * - Implements the **sleep timer**: a coroutine countdown that fades volume to zero
 *   and stops playback when the user-chosen duration elapses.
 * - Tracks **loop count** by listening for position discontinuities caused by
 *   [Player.REPEAT_MODE_ONE] restarting the track.
 *
 * ## Why AndroidViewModel?
 * [SessionToken] and [MediaController] require an [Application] context that outlives
 * any Activity or Composable. [AndroidViewModel] provides this safely.
 */
class MantraViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MantraUiState(allMantras = MantraRepository.getAll()))
    val uiState: StateFlow<MantraUiState> = _uiState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    /** Active sleep-timer coroutine job; cancelled when timer is cleared or reset. */
    private var timerJob: Job? = null

    // -------------------------------------------------------------------------
    // Connection management
    // -------------------------------------------------------------------------

    /**
     * Asynchronously connects to [MantraPlayerService].
     * Safe to call repeatedly — no-op if already connected.
     */
    fun connect() {
        if (controller != null) return
        val ctx = getApplication<Application>()
        val token = SessionToken(ctx, ComponentName(ctx, MantraPlayerService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                controller = future.get()
                controller?.addListener(playerListener)
                syncState()
            },
            ContextCompat.getMainExecutor(ctx),
        )
    }

    /**
     * Releases the [MediaController] but does NOT stop the service.
     * Audio keeps playing after the UI navigates away.
     */
    fun disconnect() {
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
        _uiState.update { it.copy(isReady = false) }
    }

    // -------------------------------------------------------------------------
    // Playback commands
    // -------------------------------------------------------------------------

    /**
     * Loads [mantra] into the player and starts looped playback from the beginning.
     * Resets [MantraUiState.loopCount] to 0.
     *
     * No-op if [Mantra.audioRes] is null — add the file to `res/raw/` and set the
     * resource ID in [MantraRepository] to enable playback.
     */
    fun selectAndPlay(mantra: Mantra) {
        val audioRes = mantra.audioRes ?: return
        val ctrl = controller ?: return
        val ctx = getApplication<Application>()
        val uri = Uri.parse("android.resource://${ctx.packageName}/$audioRes")
        ctrl.setMediaItem(MediaItem.fromUri(uri))
        ctrl.prepare()
        ctrl.play()
        _uiState.update { it.copy(selected = mantra, loopCount = 0) }
    }

    /** Resumes playback after [pause]. */
    fun play() {
        controller?.play()
    }

    /** Pauses playback; position and timer are preserved. */
    fun pause() {
        controller?.pause()
    }

    /**
     * Stops playback, rewinds to the start, and cancels any active sleep timer.
     */
    fun stop() {
        cancelTimer()
        restoreVolume()
        controller?.run {
            stop()
            seekTo(0)
        }
        _uiState.update { it.copy(isPlaying = false) }
    }

    // -------------------------------------------------------------------------
    // Sleep timer
    // -------------------------------------------------------------------------

    /**
     * Sets or clears the sleep timer.
     *
     * @param minutes Duration in minutes, or null to cancel an existing timer.
     *
     * When [minutes] is not null a coroutine countdown starts immediately.
     * When the countdown reaches zero, [fadeOutAndStop] is called which gradually
     * lowers the player volume to 0 before stopping.
     */
    fun setTimer(minutes: Int?) {
        timerJob?.cancel()
        timerJob = null
        if (minutes == null) {
            restoreVolume()
            _uiState.update { it.copy(timerMinutes = null, timerRemainingSeconds = null, isFadingOut = false) }
            return
        }
        val totalSeconds = minutes * 60L
        _uiState.update { it.copy(timerMinutes = minutes, timerRemainingSeconds = totalSeconds) }
        timerJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1_000)
                remaining--
                _uiState.update { it.copy(timerRemainingSeconds = remaining) }
            }
            fadeOutAndStop()
        }
    }

    /**
     * Cancels the active sleep timer without stopping playback.
     * Volume is restored if a fade-out was in progress.
     */
    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        restoreVolume()
        _uiState.update { it.copy(timerMinutes = null, timerRemainingSeconds = null, isFadingOut = false) }
    }

    // -------------------------------------------------------------------------
    // Tab selection
    // -------------------------------------------------------------------------

    /** Switches the active tab and stops playback if the selected track is in a different category. */
    fun setTab(category: MantraCategory) {
        _uiState.update { state ->
            val stopNeeded = state.selected != null && state.selected.category != category
            if (stopNeeded) {
                cancelTimer()
                restoreVolume()
                controller?.stop()
            }
            state.copy(
                selectedTab = category,
                selected = if (stopNeeded) null else state.selected,
                isPlaying = if (stopNeeded) false else state.isPlaying,
            )
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Fades the player volume from its current level to 0 over ~3 seconds
     * (30 steps × 100 ms), then calls [stop].
     *
     * Called at the end of the sleep-timer countdown. After stop() the volume
     * is immediately restored so the next session starts at full volume.
     */
    private suspend fun fadeOutAndStop() {
        val ctrl = controller ?: return
        _uiState.update { it.copy(isFadingOut = true) }
        val steps = 30
        repeat(steps) { step ->
            ctrl.volume = 1f - (step + 1).toFloat() / steps
            delay(100)
        }
        stop()
        // stop() already calls cancelTimer() → isFadingOut reset there
    }

    /** Resets player volume to unity (1.0) after a fade-out or timer cancellation. */
    private fun restoreVolume() {
        controller?.volume = 1f
    }

    /** Reads live player state and pushes it into [_uiState]. */
    private fun syncState() {
        val ctrl = controller ?: return
        _uiState.update { it.copy(isPlaying = ctrl.isPlaying, isReady = true) }
    }

    /**
     * Player event listener — drives [MantraUiState] from real player events so
     * that notification controls, Bluetooth buttons, and lock-screen actions are
     * all reflected correctly in the UI.
     */
    private val playerListener = object : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        /**
         * Fired by [Player.REPEAT_MODE_ONE] every time the track restarts.
         * [Player.DISCONTINUITY_REASON_AUTO_TRANSITION] covers both looping and
         * playlist advancement — for a single-item repeat this is always a loop.
         */
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                _uiState.update { it.copy(loopCount = it.loopCount + 1) }
            }
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        disconnect()
        super.onCleared()
    }
}
