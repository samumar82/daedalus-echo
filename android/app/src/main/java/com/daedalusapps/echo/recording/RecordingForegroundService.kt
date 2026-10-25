package com.daedalusapps.echo.recording

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.daedalusapps.echo.R

/**
 * Keeps microphone capture alive while the display is off or the app is no longer
 * foregrounded. Android shows an ongoing notification while recording.
 */
class RecordingForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audio recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps an active recording running in the background"
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val name = intent?.getStringExtra(EXTRA_FILENAME) ?: "Recording"
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Daedalus Echo — Recording")
            .setContentText(name)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_NOT_STICKY
    }

    companion object {
        private const val CHANNEL_ID = "daedalus_recording_channel"
        private const val NOTIFICATION_ID = 4202
        private const val EXTRA_FILENAME = "extra_filename"
        private const val ACTION_STOP = "com.daedalusapps.echo.STOP_RECORDING_SERVICE"

        fun start(context: Context, filename: String) {
            val intent = Intent(context, RecordingForegroundService::class.java)
                .putExtra(EXTRA_FILENAME, filename)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, RecordingForegroundService::class.java).apply {
                    action = ACTION_STOP
                }
            )
        }
    }
}
