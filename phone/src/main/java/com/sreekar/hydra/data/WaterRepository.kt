package com.sreekar.hydra.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

@Entity(tableName = "drinks")
data class Drink(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ml: Int,
    val timestamp: Long,
    val source: String // "phone" or "watch"
)

@Dao
interface DrinkDao {
    @Insert suspend fun insert(drink: Drink)
    @Query("SELECT * FROM drinks WHERE timestamp >= :start AND timestamp < :end ORDER BY timestamp DESC")
    fun getForDay(start: Long, end: Long): Flow<List<Drink>>
    @Query("SELECT COALESCE(SUM(ml),0) FROM drinks WHERE timestamp >= :start AND timestamp < :end")
    fun totalForDay(start: Long, end: Long): Flow<Int>
    @Query("SELECT * FROM drinks ORDER BY timestamp DESC LIMIT 100")
    fun recent(): Flow<List<Drink>>
}

@Database(entities = [Drink::class], version = 1, exportSchema = false)
abstract class HydraDb : RoomDatabase() { abstract fun dao(): DrinkDao }

class WaterRepository(context: Context) {
    private val db = Room.databaseBuilder(context, HydraDb::class.java, "hydra.db").build()
    private val dao = db.dao()
    private val prefs = context.getSharedPreferences("hydra", Context.MODE_PRIVATE)

    var goalMl: Int
        get() = prefs.getInt("goal_ml", 2000)
        set(v) { prefs.edit().putInt("goal_ml", v).apply() }

    companion object {
        const val MIN_GOAL_ML = 0
        const val MAX_GOAL_ML = 4000
        const val GOAL_STEP_ML = 100
        val GOAL_PRESETS = listOf(500, 1000, 1500, 2000, 2500, 3000, 3500, 4000)
    }

    var defaultDrinkMl: Int
        get() = prefs.getInt("drink_ml", 250)
        set(v) { prefs.edit().putInt("drink_ml", v).apply() }

    var reminderIntervalMin: Int
        get() = prefs.getInt("interval_min", 60)
        set(v) { prefs.edit().putInt("interval_min", v).apply() }

    suspend fun logDrink(ml: Int, source: String) {
        dao.insert(Drink(ml = ml, timestamp = System.currentTimeMillis(), source = source))
    }

    fun todayTotal(): Flow<Int> {
        val (s, e) = todayRange()
        return dao.totalForDay(s, e)
    }
    fun todayDrinks(): Flow<List<Drink>> {
        val (s, e) = todayRange()
        return dao.getForDay(s, e)
    }
    fun recent() = dao.recent()

    private fun todayRange(): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }
}
