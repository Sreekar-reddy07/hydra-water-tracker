package com.sreekar.hydra.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sreekar.hydra.data.WaterRepository

/** Re-schedule the hourly reminder after a phone reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val repo = WaterRepository(context)
        val manager = ReminderManager(context)
        manager.createChannel()
        manager.scheduleNextHourly(repo)
    }
}
