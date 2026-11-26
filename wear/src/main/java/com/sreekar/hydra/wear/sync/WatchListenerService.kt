package com.sreekar.hydra.wear.sync

import com.google.android.gms.wearable.*
import com.sreekar.hydra.shared.Sync

/**
 * Watch side listener:
 * - /hydra/start_vibrate : phone reminder fired -> start continuous vibration on watch
 * - /hydra/stop_vibrate  : phone says stop -> stop watch vibration
 * - /hydra/goal, /hydra/progress : update goal/total (handled via DataClient in UI, kept here for completeness)
 */
class WatchListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            Sync.PATH_START_VIBRATE -> {
                WatchVibrationService.start(this)
            }
            Sync.PATH_STOP_VIBRATE -> {
                WatchVibrationService.stop(this)
            }
        }
    }
}
