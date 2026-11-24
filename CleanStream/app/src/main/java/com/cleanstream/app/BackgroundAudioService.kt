package com.cleanstream.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Foreground Service that keeps the audio session alive when the user
 * switches apps or locks the screen. Android requires a visible notification
 * for any foreground service, which is what keeps the audio stream active.
 *
 * Without this service, Android pauses media once the app is backgrounded.
 */
class BackgroundAudioService : Service() {

    private val notificationChannelId = "cleanstream_bg_play"
    private val foregroundNotificationId = 1001

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannelIfNeeded()
        val notification = buildForegroundNotification()
        startForeground(foregroundNotificationId, notification)
        // START_STICKY: system will restart this service if it's killed
        return START_STICKY
    }

    /** IBinder is null — this is a started service, not a bound service. */
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    /** Creates the notification channel required on Android O (API 26) and above. */
    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Background Playback"
            val channel = NotificationChannel(
                notificationChannelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW  // LOW = silent, no sound/vibration
            ).apply {
                description = "Keeps audio playing when CleanStream is in the background"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /** Builds the persistent notification shown while audio plays in the background. */
    private fun buildForegroundNotification() = run {
        // Tapping the notification brings the user back to the app
        val returnToAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val tapToReturnPendingIntent = PendingIntent.getActivity(
            this, 0, returnToAppIntent, pendingIntentFlags
        )
        NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("CleanStream")
            .setContentText("Playing audio in background")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(tapToReturnPendingIntent)
            .setOngoing(true)         // Prevents the user from dismissing the notification
            .setSilent(true)          // No sound/vibration on each update
            .build()
    }

    companion object {
        /** Convenience method to start the background audio service. */
        fun start(context: Context) {
            val serviceIntent = Intent(context, BackgroundAudioService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }

        /** Convenience method to stop the background audio service. */
        fun stop(context: Context) {
            context.stopService(Intent(context, BackgroundAudioService::class.java))
        }
    }
}
