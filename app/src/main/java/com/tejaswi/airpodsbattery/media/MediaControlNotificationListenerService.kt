package com.tejaswi.airpodsbattery.media

import android.content.ComponentName
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService

class MediaControlNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        instance = this
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
    }

    fun pauseActiveMedia(): Boolean {
        val controllers = try {
            val manager = getSystemService(MediaSessionManager::class.java)
            manager.getActiveSessions(
                ComponentName(this, MediaControlNotificationListenerService::class.java)
            )
        } catch (_: SecurityException) {
            return false
        } catch (_: Exception) {
            return false
        }

        var paused = false
        for (controller: MediaController in controllers) {
            if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
                try {
                    controller.transportControls.pause()
                    paused = true
                } catch (_: Exception) {
                    // The media session can disappear between discovery and pause.
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
