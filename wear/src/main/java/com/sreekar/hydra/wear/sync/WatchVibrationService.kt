package com.sreekar.hydra.wear.sync

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import com.sreekar.hydra.shared.Sync

/**
 * Watch Ultra continuous vibration — same idea as phone:
 * Direct Vibrator call ignores ringer mode (mute / vibrate / sound).
 * Pattern repeats until Quick Button / I Drank / Stop is pressed.
 * Safety auto-stop after 10 min on watch (smaller battery).
 */
class WatchVibrationService : Service() {
    companion object {
        const val ACTION_START = "com.sreekar.hydra.wear.START_VIBRATE"
        const val ACTION_STOP = "com.sreekar.hydra.wear.STOP_VIBRATE"

        fun start(ctx: Context) {
            val i = Intent(ctx, WatchVibrationService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
        }
        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, WatchVibrationService::class.java).setAction(ACTION_STOP))
        }
    }

    private var vibrator: Vibrator? = null
    private var handler: Handler? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopVibrating(); stopSelf(); return START_NOT_STICKY }
            ACTION_START -> {
                // Foreground not strictly required on Wear for short service, but keeps it alive
                try {
                    val notif = android.app.Notification.Builder(this, "hydra_watch")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("💧 Drink water!")
                        .setContentText("Press Quick Button to stop vibration")
                        .build()
                    // Channel may not exist on watch yet — try/catch, vibration still works without it
                    startForeground(2002, notif)
                } catch (_: Exception) {}
                startVibrating()
                handler = Handler(Looper.getMainLooper())
                handler?.postDelayed({ stopVibrating(); stopSelf() }, 10 * 60 * 1000L)
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
        // Strong, unmistakable: 700ms buzz, 300ms pause, repeat
        val pattern = longArrayOf(0, 700, 300, 700, 300)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopVibrating() {
        try { vibrator?.cancel() } catch (_: Exception) {}
        handler?.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        stopVibrating()
        super.onDestroy()
    }
}
