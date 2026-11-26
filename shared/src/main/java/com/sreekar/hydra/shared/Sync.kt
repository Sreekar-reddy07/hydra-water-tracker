package com.sreekar.hydra.shared

// Shared between phone and watch — Wear Data Layer paths
object Sync {
    const val PATH_DRINK = "/hydra/drink"       // watch -> phone: drank X ml
    const val PATH_PROGRESS = "/hydra/progress" // phone -> watch: today total + goal
    const val PATH_REMINDER = "/hydra/reminder" // phone -> watch: reminder fired
    const val PATH_START_VIBRATE = "/hydra/start_vibrate" // phone -> watch: start continuous vibration
    const val PATH_STOP_VIBRATE = "/hydra/stop_vibrate"   // watch -> phone OR phone -> watch: stop vibration
    const val PATH_GOAL = "/hydra/goal"         // phone -> watch: goal changed via slider
    const val KEY_ML = "ml"
    const val KEY_TOTAL = "total_ml"
    const val KEY_GOAL = "goal_ml"
    const val KEY_TIMESTAMP = "timestamp"
    const val KEY_REASON = "reason" // "drank", "quick_button", "stop_button"
}
data class DrinkEvent(val ml: Int, val timestamp: Long = System.currentTimeMillis())
data class DayProgress(val totalMl: Int, val goalMl: Int) {
    val remainingMl: Int get() = (goalMl - totalMl).coerceAtLeast(0)
    val percent: Float get() = if (goalMl == 0) 0f else totalMl.toFloat() / goalMl
}
