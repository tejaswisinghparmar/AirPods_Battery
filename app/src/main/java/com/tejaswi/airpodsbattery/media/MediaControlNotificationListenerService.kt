package com.tejaswi.airpodsbattery.media

import android.content.ComponentName
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService

class MediaControlNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() { instance = this }
    override fun onListenerDisconnected() { if (instance === this) instance = null }

    private fun activeControllers(): List<MediaController> {
        return try {
            val manager = getSystemService(MediaSessionManager::class.java)
            manager.getActiveSessions(ComponentName(this, MediaControlNotificationListenerService::class.java))
        } catch (_: SecurityException) { emptyList() }
          catch (_: Exception) { emptyList() }
    }

    fun pauseActiveMedia(): Boolean {
        var changed = false
        for (controller in activeControllers()) {
            if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
                try { controller.transportControls.pause(); changed = true } catch (_: Exception) {}
            }
        }
        return changed
    }

    fun resumeActiveMedia(): Boolean {
        var changed = false
        for (controller in activeControllers()) {
            if (controller.playbackState?.state == PlaybackState.STATE_PAUSED) {
                try { controller.transportControls.play(); changed = true } catch (_: Exception) {}
            }
        }
        return changed
    }

    companion object {
        @Volatile private var instance: MediaControlNotificationListenerService? = null
        fun isConnected(): Boolean = instance != null
        fun pausePlayback(): Boolean = instance?.pauseActiveMedia() == true
        fun resumePlayback(): Boolean = instance?.resumeActiveMedia() == true
    }
}
