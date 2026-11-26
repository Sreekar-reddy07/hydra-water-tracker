package com.sreekar.hydra.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.sreekar.hydra.data.WaterRepository
import com.sreekar.hydra.reminder.ReminderManager
import com.sreekar.hydra.reminder.VibrationService
import com.sreekar.hydra.shared.Sync
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = WaterRepository(this)
        val reminder = ReminderManager(this)
        reminder.createChannel()
        setContent {
            val total by repo.todayTotal().collectAsState(initial = 0)
            val drinks by repo.todayDrinks().collectAsState(initial = emptyList())
            val scope = rememberCoroutineScope()
            // Slider state — starts from saved goal, default 2000 ml
            var goalSlider by remember { mutableStateOf(repo.goalMl.toFloat()) }
            val goal = goalSlider.roundToInt()
            val remaining = (goal - total).coerceAtLeast(0)

            fun pushGoalToWatch(newGoal: Int) {
                scope.launch {
                    try {
                        val req = PutDataMapRequest.create(Sync.PATH_GOAL).apply {
                            dataMap.putInt(Sync.KEY_GOAL, newGoal)
                            dataMap.putLong(Sync.KEY_TIMESTAMP, System.currentTimeMillis())
                        }.asPutDataRequest().setUrgent()
                        Wearable.getDataClient(this@MainActivity).putDataItem(req)
                    } catch (_: Exception) {}
                }
            }

            MaterialTheme {
                Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💧 Hydra", style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(12.dp))
                    Text("$total / $goal ml", style = MaterialTheme.typography.headlineMedium)
                    LinearProgressIndicator(progress = if (goal == 0) 0f else total.toFloat() / goal, modifier = Modifier.fillMaxWidth().height(12.dp))
                    Text("$remaining ml left today", style = MaterialTheme.typography.bodyLarge)

                    Spacer(Modifier.height(20.dp))
                    // --- 1. DAILY GOAL SLIDER ---
                    Text("Daily goal: $goal ml", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = goalSlider,
                        onValueChange = { goalSlider = it },
                        onValueChangeFinished = {
                            val rounded = ((goalSlider / 100).roundToInt() * 100).coerceIn(
                                WaterRepository.MIN_GOAL_ML, WaterRepository.MAX_GOAL_ML
                            )
                            repo.goalMl = rounded
                            goalSlider = rounded.toFloat()
                            pushGoalToWatch(rounded)
                        },
                        valueRange = WaterRepository.MIN_GOAL_ML.toFloat()..WaterRepository.MAX_GOAL_ML.toFloat(),
                        steps = (WaterRepository.MAX_GOAL_ML - WaterRepository.MIN_GOAL_ML) / 100 - 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("0 ml", style = MaterialTheme.typography.bodySmall)
                        Text("2000 ml", style = MaterialTheme.typography.bodySmall)
                        Text("4000 ml", style = MaterialTheme.typography.bodySmall)
                    }
                    // Quick presets: 500, 1000, 1500 ... 4000 — two rows of 4
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        WaterRepository.GOAL_PRESETS.chunked(4).forEach { rowPresets ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowPresets.forEach { preset ->
                                    OutlinedButton(
                                        onClick = {
                                            repo.goalMl = preset
                                            goalSlider = preset.toFloat()
                                            pushGoalToWatch(preset)
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                    ) { Text("$preset", style = MaterialTheme.typography.bodyMedium) }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { scope.launch { repo.logDrink(100, "phone"); reminder.dismiss(); VibrationService.stop(this@MainActivity); reminder.scheduleNextHourly(repo) } }) { Text("+100 ml") }
                        Button(onClick = { scope.launch { repo.logDrink(250, "phone"); reminder.dismiss(); VibrationService.stop(this@MainActivity); reminder.scheduleNextHourly(repo) } }) { Text("+250 ml") }
                        Button(onClick = { scope.launch { repo.logDrink(500, "phone"); reminder.dismiss(); VibrationService.stop(this@MainActivity); reminder.scheduleNextHourly(repo) } }) { Text("+500 ml") }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("Today's drinks (${drinks.size})", style = MaterialTheme.typography.titleMedium)
                    drinks.take(10).forEach { d ->
                        val time = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US).format(java.util.Date(d.timestamp))
                        Text("${d.ml} ml  •  $time  •  ${d.source}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Reminder: every ${repo.reminderIntervalMin} min, 8am–10pm. Vibrates continuously until you press the watch Quick Button or tap I Drank.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { reminder.showReminder(remaining) }) { Text("Test vibrate + sound") }
                        OutlinedButton(onClick = { VibrationService.stop(this@MainActivity); reminder.dismiss() }) { Text("Stop vibration") }
                    }
                }
            }
        }
    }
}
