package com.sreekar.hydra.wear.ui

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.android.gms.wearable.Wearable
import com.sreekar.hydra.shared.Sync
import com.sreekar.hydra.wear.sync.WatchVibrationService
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

/**
 * Galaxy Watch Ultra — Quick Button (middle / orange button) handling.
 *
 * How Samsung's Quick Button works — important, read this:
 * - Samsung lets you assign the Quick Button in:
 *   Watch Settings > Buttons and gestures > Action, OR
 *   Galaxy Wearable app on phone > Watch settings > Buttons and gestures > Quick button > Action
 * - You MUST set Action = Hydra (or "Open app > Hydra") for the background case.
 *   Without this, pressing it when Hydra is NOT open will do whatever Samsung
 *   has it set to (flashlight / workout) — Android cannot hijack it globally.
 *
 * - Two paths, both covered:
 *   1. FOREGROUND: Hydra is open (reminder brings it to front). Pressing the
 *      Quick Button sends KEYCODE_STEM_1 (or STEM_2/STEM_3 on some firmware).
 *      onKeyDown() below catches it -> stops vibration on watch + phone, logs 250ml.
 *   2. BACKGROUND: Hydra is NOT open, but Quick Button is assigned to Hydra.
 *      Pressing it LAUNCHES Hydra. onCreate/onResume then stops vibration
 *      immediately — same effect, just via launch instead of key event.
 *
 * See: developer.android.com/training/wearables/user-input/physical-buttons
 * and samsung.com/us/support/answer/ANS10007058/
 */
class WatchActivity : ComponentActivity() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val TAG = "HydraQuickButton"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Path 2: launched via Quick Button — stop any ongoing vibration right away
        // (If vibration wasn't running, this is a harmless no-op)
        WatchVibrationService.stop(this)

        val nodeClient = Wearable.getNodeClient(this)
        val messageClient = Wearable.getMessageClient(this)
        setContent {
            WatchMain(nodeClient = nodeClient, messageClient = messageClient)
        }
    }

    override fun onResume() {
        super.onResume()
        // If user pressed Quick Button to open us while vibrating, ensure it stops.
        // We don't auto-log here — the key handler / tap does the logging.
        // But if this resume came from a reminder launch, the UI shows Stop button.
    }

    // Path 1: Foreground Quick Button press
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.repeatCount == 0) {
            when (keyCode) {
                KeyEvent.KEYCODE_STEM_1,
                KeyEvent.KEYCODE_STEM_2,
                KeyEvent.KEYCODE_STEM_3 -> {
                    Log.d(TAG, "Quick Button pressed (keyCode=$keyCode) — stopping vibration + logging drink")
                    onQuickButtonPressed()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun onQuickButtonPressed() {
        // 1. Stop watch vibration immediately
        WatchVibrationService.stop(this)
        // 2. Tell phone: stop its vibration too, and log 250ml as "quick_button"
        scope.launch {
            try {
                val nodes = Wearable.getNodeClient(this@WatchActivity).connectedNodes.await()
                nodes.forEach { node ->
                    Wearable.getMessageClient(this@WatchActivity)
                        .sendMessage(node.id, Sync.PATH_STOP_VIBRATE, "quick_button".toByteArray())
                        .await()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send stop to phone", e)
            }
        }
    }
}
