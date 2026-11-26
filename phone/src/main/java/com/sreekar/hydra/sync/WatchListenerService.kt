package com.sreekar.hydra.sync

import com.google.android.gms.wearable.*
import com.sreekar.hydra.data.WaterRepository
import com.sreekar.hydra.reminder.ReminderManager
import com.sreekar.hydra.reminder.VibrationService
import com.sreekar.hydra.shared.Sync
import kotlinx.coroutines.*

/**
 * Phone side: receives messages from Galaxy Watch Ultra.
 * - /hydra/drink : watch tapped I Drank -> log it, stop vibration both sides
 * - /hydra/stop_vibrate : watch Quick Button / Stop button -> stop vibration
 *   If reason=quick_button, also log 250ml (pressing Quick Button = I drank).
 */
class WatchListenerService : WearableListenerService() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onMessageReceived(event: MessageEvent) {
        val repo = WaterRepository(this)
        val reminder = ReminderManager(this)
        when (event.path) {
            Sync.PATH_DRINK -> {
                val ml = String(event.data).trim().toIntOrNull() ?: 250
                scope.launch {
                    repo.logDrink(ml, "watch")
                    reminder.dismiss()
                    reminder.scheduleNextHourly(repo)
                    pushProgress(repo)
                }
            }
            Sync.PATH_STOP_VIBRATE -> {
                val reason = String(event.data).trim() // "quick_button", "stop_button", "drank"
                scope.launch {
                    VibrationService.stop(this@WatchListenerService)
                    reminder.dismiss()
                    // Quick Button press = user drank — log it. Stop button alone = just stop buzzing.
                    if (reason == "quick_button" || reason == "drank") {
                        repo.logDrink(repo.defaultDrinkMl, "watch-quick-button")
                        reminder.scheduleNextHourly(repo)
                    }
                    pushProgress(repo)
                }
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // Goal changed on watch? Phone is source of truth, ignore — but keep for future.
        super.onDataChanged(dataEvents)
    }

    private suspend fun pushProgress(repo: WaterRepository) {
        var total = 0
        repo.todayTotal().collect { total = it; return@collect }
        val req = PutDataMapRequest.create(Sync.PATH_PROGRESS).apply {
            dataMap.putInt(Sync.KEY_TOTAL, total)
            dataMap.putInt(Sync.KEY_GOAL, repo.goalMl)
            dataMap.putLong(Sync.KEY_TIMESTAMP, System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        Wearable.getDataClient(this).putDataItem(req)
    }
}
