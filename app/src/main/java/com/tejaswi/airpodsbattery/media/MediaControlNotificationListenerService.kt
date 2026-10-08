package com.tejaswi.airpodsbattery.media

import android.service.notification.NotificationListenerService
import android.media.session.MediaController
import android.media.session.PlaybackState

class MediaControlNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        instance = this
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
    }

    fun pauseActiveMedia(): Boolean {
        val controllers = try {
            activeSessions
        } catch (_: SecurityException) {
            return false
        } catch (_: Exception) {
            return false
        }

        var paused = false
        for (controller in controllers) {
            val state = controller.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING) {
                try {
                    controller.transportControls.pause()
                    paused = true
                } catch (_: Exception) {
                    // Some media sessions can disappear between discovery and pause.
                }
            }
        }
        return paused
    }

    companion object {
        @Volatile
        private var instance: MediaControlNotificationListenerService? = null

        fun isConnected(): Boolean = instance != null

        fun pausePlayback(): Boolean = instance?.pauseActiveMedia() == true
    }
}
