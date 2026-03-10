package com.sri.aham.mantra.service

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Foreground service that owns the [ExoPlayer] instance and exposes it via a [MediaSession].
 *
 * ## Why a service?
 * Android will kill the player when the app goes to the background unless it runs inside a
 * foreground service. [MediaSessionService] handles starting the foreground notification
 * automatically whenever the player is active, so no manual `startForeground()` call is needed.
 *
 * ## Looping
 * [Player.REPEAT_MODE_ONE] makes ExoPlayer restart the current track the moment it ends,
 * producing seamless infinite looping without any gap or re-buffering.
 *
 * ## Client connection
 * [MantraViewModel][com.sri.aham.mantra.viewmodel.MantraViewModel] connects to this service
 * via a [androidx.media3.session.MediaController] bound to the [MediaSession]. All playback
 * commands (play, pause, stop, seek) flow through that controller.
 *
 * ## Lifecycle
 * - Created when the first [androidx.media3.session.MediaController] connects.
 * - Stays alive while a controller is connected OR while the player is playing (foreground).
 * - Destroyed when all controllers disconnect and playback has stopped.
 */
class MantraPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .build()
            .apply {
                // Restart the track automatically when it reaches the end.
                repeatMode = Player.REPEAT_MODE_ONE
            }
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onDestroy() {
        // Release the session and player together to avoid resource leaks.
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    // -------------------------------------------------------------------------
    // MediaSessionService contract
    // -------------------------------------------------------------------------

    /**
     * Returns the active [MediaSession] so connecting controllers can issue commands.
     * Returning null rejects the connection request.
     */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession
}
