package com.sreekar.hydra.reminder

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sreekar.hydra.data.WaterRepository
import com.google.android.gms.tasks.Tasks

/**
 * LOUD reminder — key trick:
 * Channel uses AudioAttributes.USAGE_ALARM + setBypassDnd(true).
 * That plays on the ALARM stream, which ignores mute/vibrate + DND
 * (once user grants DND access). Full-screen intent wakes the screen.
 * Repeats every 5 min until a drink is logged.
 */
class ReminderManager(private val ctx: Context) {
    companion object {
        const val CHANNEL_ID = "hydra_alarm"
        const val NOTIF_ID = 1001
        const val ACTION_DRANK = "com.sreekar.hydra.DRANK"
        const val ACTION_SNOOZE = "com.sreekar.hydra.SNOOZE"
        const val ACTION_STOP_VIBRATE = "com.sreekar.hydra.STOP_VIBRATE"
        const val REPEAT_DELAY_MS = 5 * 60 * 1000L // 5 min nag
    }

    fun createChannel() {
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val ch = NotificationChannel(CHANNEL_ID, "Water Alarm", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Loud water reminder — bypasses mute/vibrate/DND"
            setSound(sound, attrs)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 1000)
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    fun showReminder(remainingMl: Int) {
        createChannel()
        // 2. CONTINUOUS VIBRATION — starts on BOTH phone and watch, ignores ringer mode
        VibrationService.start(ctx)
        sendStartVibrateToWatch()
        val drankIntent = PendingIntent.getBroadcast(ctx, 1,
            Intent(ctx, ReminderReceiver::class.java).setAction(ACTION_DRANK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stopIntent = PendingIntent.getBroadcast(ctx, 3,
            Intent(ctx, ReminderReceiver::class.java).setAction(ACTION_STOP_VIBRATE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val snoozeIntent = PendingIntent.getBroadcast(ctx, 2,
            Intent(ctx, ReminderReceiver::class.java).setAction(ACTION_SNOOZE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val openIntent = PendingIntent.getActivity(ctx, 0,
            Intent(ctx, Class.forName("com.sreekar.hydra.ui.MainActivity")),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("💧 Time to drink water!")
            .setContentText("You still need $remainingMl ml today. Tap I Drank on your watch or here.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(openIntent, true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 1000))
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(0, "I Drank 250ml", drankIntent)
            .addAction(0, "Stop vibration", stopIntent)
            .addAction(0, "Snooze 5 min", snoozeIntent)
            .build()

        ctx.getSystemService(NotificationManager::class.java).notify(NOTIF_ID, notif)
        // Schedule the nag: if no drink in 5 min, fire again
        scheduleExact(System.currentTimeMillis() + REPEAT_DELAY_MS)
    }

    fun dismiss() {
        ctx.getSystemService(NotificationManager::class.java).cancel(NOTIF_ID)
        VibrationService.stop(ctx)
        sendStopVibrateToWatch()
    }

    private fun sendStartVibrateToWatch() {
        // Fire-and-forget: tell watch to start its own continuous vibration
        Thread {
            try {
                val nodes = Tasks.await(com.google.android.gms.wearable.Wearable.getNodeClient(ctx).connectedNodes)
                nodes.forEach { node ->
                    com.google.android.gms.wearable.Wearable.getMessageClient(ctx)
                        .sendMessage(node.id, com.sreekar.hydra.shared.Sync.PATH_START_VIBRATE, byteArrayOf(1))
                }
            } catch (_: Exception) {}
        }.start()
    }

    private fun sendStopVibrateToWatch() {
        Thread {
            try {
                val nodes = Tasks.await(com.google.android.gms.wearable.Wearable.getNodeClient(ctx).connectedNodes)
                nodes.forEach { node ->
                    com.google.android.gms.wearable.Wearable.getMessageClient(ctx)
                        .sendMessage(node.id, com.sreekar.hydra.shared.Sync.PATH_STOP_VIBRATE, byteArrayOf(1))
                }
            } catch (_: Exception) {}
        }.start()
    }

    fun scheduleExact(triggerAt: Long) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = PendingIntent.getBroadcast(ctx, 0,
            Intent(ctx, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        }
    }

    fun scheduleNextHourly(repo: WaterRepository) {
        // Called after a drink: next reminder = now + interval (default 60 min), only 08:00-22:00
        val cal = java.util.Calendar.getInstance()
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        if (hour in 8..21) {
            scheduleExact(System.currentTimeMillis() + repo.reminderIntervalMin * 60 * 1000L)
        }
    }
}
