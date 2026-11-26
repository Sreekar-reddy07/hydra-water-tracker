package com.sreekar.hydra.reminder

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat

/**
 * Continuous vibration — ignores mute / vibrate / silent / DND.
 *
 * Why this works when a normal notification doesn't:
 * - Normal notifications respect the ringer mode. A direct Vibrator call does NOT.
 * - We call VibratorManager directly with a repeating waveform, not via Notification.
 * - Runs as a foreground service so Samsung / Android doesn't kill it.
 * - Stops ONLY when: user taps I Drank, presses Quick Button on watch,
 *   taps Stop on watch/phone, or snoozes.
 *
 * Pattern: 800ms buzz, 400ms pause, 800ms buzz, 400ms pause... repeats forever.
 * Battery note: vibration motor is low power vs screen/sound, but it will
 * keep going until stopped — that's the point. Max safety: auto-stop after 30 min.
 */
class VibrationService : Service() {
    companion object {
        const val ACTION_START = "com.sreekar.hydra.START_VIBRATE"
        const val ACTION_STOP = "com.sreekar.hydra.STOP_VIBRATE"
        const val NOTIF_ID = 1002
        const val MAX_VIBRATE_MS = 30 * 60 * 1000L // safety auto-stop after 30 min

        fun start(ctx: Context) {
            val i = Intent(ctx, VibrationService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
        }
        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, VibrationService::class.java).setAction(ACTION_STOP))
        }
    }

    private var vibrator: Vibrator? = null
    private var stopHandler: Handler? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopVibrating(); stopSelf(); return START_NOT_STICKY }
            ACTION_START -> {
                // Foreground notification — required for foreground service, keeps it alive
                val notif: Notification = NotificationCompat.Builder(this, ReminderManager.CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("💧 Hydra is vibrating — drink water!")
                    .setContentText("Press the Quick Button on your watch or tap I Drank to stop.")
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .build()
                startForeground(NOTIF_ID, notif)
                startVibrating()
                // Safety: stop after 30 min even if user never responds
                stopHandler = Handler(Looper.getMainLooper())
                stopHandler?.postDelayed({ stopVibrating(); stopSelf() }, MAX_VIBRATE_MS)
            }
        }
        return START_STICKY
    }

    private fun startVibrating() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        // Repeating waveform: wait 0, vibrate 800, pause 400, vibrate 800, pause 400...
        // repeat index 0 = repeat whole pattern forever.
        val pattern = longArrayOf(0, 800, 400, 800, 400)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopVibrating() {
        try { vibrator?.cancel() } catch (_: Exception) {}
        stopHandler?.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        stopVibrating()
        super.onDestroy()
    }
}
