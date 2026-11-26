package com.sreekar.hydra.wear.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.google.android.gms.wearable.*
import com.sreekar.hydra.shared.Sync
import com.sreekar.hydra.wear.sync.WatchVibrationService
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

/**
 * Galaxy Watch Ultra — main screen.
 * - Big "I Drank" = log 250ml + stop vibration (phone + watch)
 * - "Stop buzz" = stop vibration WITHOUT logging (just silence it)
 * - Quick Button (middle orange button) does same as I Drank — see WatchActivity.onKeyDown
 * - Goal comes from phone slider (default 2000ml), updates via Data Layer
 */
@Composable
fun WatchMain(nodeClient: NodeClient, messageClient: MessageClient) {
    var total by remember { mutableStateOf(0) }
    var goal by remember { mutableStateOf(2000) }
    var justDrank by remember { mutableStateOf(false) }
    var isVibrating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun sendToPhone(path: String, payload: String) {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, path, payload.toByteArray()).await()
                }
            } catch (_: Exception) {}
        }
    }

    fun logDrink(ml: Int) {
        WatchVibrationService.stop(context)
        isVibrating = false
        sendToPhone(Sync.PATH_DRINK, "$ml")
        sendToPhone(Sync.PATH_STOP_VIBRATE, "drank")
        total += ml
        justDrank = true
    }

    fun stopVibrationOnly() {
        WatchVibrationService.stop(context)
        isVibrating = false
        sendToPhone(Sync.PATH_STOP_VIBRATE, "stop_button")
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("$total / $goal ml", style = MaterialTheme.typography.body1)
        CircularProgressIndicator(
            progress = if (goal == 0) 0f else total.toFloat() / goal,
            modifier = Modifier.size(72.dp).padding(8.dp),
            strokeWidth = 6.dp
        )
        Text("${(goal - total).coerceAtLeast(0)} ml left", style = MaterialTheme.typography.caption1)
        if (isVibrating) {
            Text("🔔 Vibrating… press Quick Button", style = MaterialTheme.typography.caption1)
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { logDrink(250) },
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Text(if (justDrank) "✓ Logged!" else "💧 I Drank", style = MaterialTheme.typography.button)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { logDrink(100) }, modifier = Modifier.weight(1f)) { Text("+100") }
            Button(onClick = { logDrink(500) }, modifier = Modifier.weight(1f)) { Text("+500") }
        }
        Spacer(Modifier.height(8.dp))
        // Stop vibration without logging — for when you just want silence
        OutlinedButton(
            onClick = { stopVibrationOnly() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("⏹ Stop vibration") }
        Text("Quick Button = I Drank", style = MaterialTheme.typography.caption1)
    }
}
