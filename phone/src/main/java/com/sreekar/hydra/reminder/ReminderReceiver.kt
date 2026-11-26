package com.sreekar.hydra.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sreekar.hydra.data.WaterRepository
import kotlinx.coroutines.*

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scope = CoroutineScope(Dispatchers.IO)
        val repo = WaterRepository(context)
        val manager = ReminderManager(context)
        when (intent.action) {
            ReminderManager.ACTION_DRANK -> {
                scope.launch {
                    repo.logDrink(repo.defaultDrinkMl, "phone")
                    manager.dismiss() // also stops vibration on phone + watch
                    manager.scheduleNextHourly(repo)
                }
            }
            ReminderManager.ACTION_STOP_VIBRATE -> {
                // Stop buzzing without logging — user pressed Stop, not I Drank
                VibrationService.stop(context)
                manager.dismiss()
            }
            ReminderManager.ACTION_SNOOZE -> {
                VibrationService.stop(context)
                manager.dismiss()
                manager.scheduleExact(System.currentTimeMillis() + ReminderManager.REPEAT_DELAY_MS)
            }
            else -> {
                // Alarm fired — show loud reminder + start continuous vibration
                scope.launch {
                    var remaining = 0
                    repo.todayTotal().collect { total ->
                        remaining = (repo.goalMl - total).coerceAtLeast(0)
                        manager.showReminder(remaining)
                        return@collect
                    }
                }
            }
        }
    }
}
