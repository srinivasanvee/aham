package com.sri.aham.mantra.viewmodel

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.sri.aham.mantra.data.MantraRepository
import com.sri.aham.mantra.model.Mantra
import com.sri.aham.mantra.service.MantraPlayerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * UI state snapshot for the Mantra screen.
 *
 * @param mantras   Full list of available mantras from [MantraRepository].
 * @param selected  The currently loaded [Mantra]; null when nothing is chosen.
 * @param isPlaying True while audio is actively producing sound.
 * @param isReady   True once the [MediaController] handshake with [MantraPlayerService]
 *                  completes. Controls/transport buttons are disabled until ready.
 */
data class MantraUiState(
    val mantras: List<Mantra> = emptyList(),
    val selected: Mantra? = null,
    val isPlaying: Boolean = false,
    val isReady: Boolean = false,
)

/**
 * ViewModel for the Mantra screen.
 *
 * Owns the connection to [MantraPlayerService] via a [MediaController] and keeps
 * [MantraUiState] in sync with real player events. Because all commands go through
 * the controller, audio continues playing even when the UI is fully destroyed.
 *
 * ## Connection lifecycle
 * Call [connect] when the screen enters the composition and [disconnect] when it leaves.
 * A `DisposableEffect` in [MantraScreen][com.sri.aham.mantra.ui.MantraScreen] handles this.
 *
 * ## Using AndroidViewModel
 * [AndroidViewModel] is used instead of plain [androidx.lifecycle.ViewModel] because
 * building a [SessionToken] and [MediaController] requires an [Application] context that
 * outlives any individual Activity or Composable.
 */
class MantraViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MantraUiState(mantras = MantraRepository.getAll()))
    val uiState: StateFlow<MantraUiState> = _uiState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    // -------------------------------------------------------------------------
    // Connection management
    // -------------------------------------------------------------------------

    /**
     * Starts an async connection to [MantraPlayerService].
     * Safe to call multiple times — subsequent calls are no-ops if already connected.
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
                // Reflect current player state in case the service was already active.
                syncState()
            },
            ContextCompat.getMainExecutor(ctx),
        )
    }

    /**
     * Releases the [MediaController] connection.
     * The service (and any active playback) is NOT stopped — it keeps running independently.
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
     * Loads [mantra] into the player and starts playback from the beginning.
     *
     * If [Mantra.audioRes] is null the call is a no-op — add the audio file to
     * `res/raw/` and set the resource ID in [MantraRepository] to enable playback.
     */
    fun selectAndPlay(mantra: Mantra) {
        val audioRes = mantra.audioRes ?: return
        val ctrl = controller ?: return
        val ctx = getApplication<Application>()
        val uri = Uri.parse("android.resource://${ctx.packageName}/$audioRes")
        ctrl.setMediaItem(MediaItem.fromUri(uri))
        ctrl.prepare()
        ctrl.play()
        _uiState.update { it.copy(selected = mantra) }
    }

    /** Resumes playback after a [pause]. */
    fun play() {
        controller?.play()
    }

    /** Pauses playback; position is preserved. */
    fun pause() {
        controller?.pause()
    }

    /** Stops playback and rewinds to the start of the track. */
    fun stop() {
        controller?.run {
            stop()
            seekTo(0)
        }
        _uiState.update { it.copy(isPlaying = false) }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /** Reads current player state and pushes it into [_uiState]. */
    private fun syncState() {
        val ctrl = controller ?: return
        _uiState.update { it.copy(isPlaying = ctrl.isPlaying, isReady = true) }
    }

    /**
     * Forwards player events to [_uiState] so the UI reacts to changes that originate
     * from the notification controls or media hardware buttons.
     */
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }
    }

    override fun onCleared() {
        disconnect()
        super.onCleared()
    }
}
