package com.tasnimanas.nofap.liquidglass.data

import android.content.Context
import com.tasnimanas.nofap.liquidglass.widget.MidnightRolloverReceiver
import com.tasnimanas.nofap.liquidglass.widget.StreakWidgetProvider
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class StreakHistoryItem(
    val endedAtMillis: Long,
    val durationDays: Int,
    val reason: String
)

data class JournalEntry(
    val timestampMillis: Long,
    val note: String
)

data class StreakSnapshot(
    val startTimeMillis: Long,
    val elapsedMillis: Long,
    val days: Int,
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
    val goalDays: Int,
    val bestStreakDays: Int,
    val totalRelapses: Int,
    val pledgedToday: Boolean,
    val quoteOfTheDay: String
)

object StreakRepository {
    private const val PREFS_NAME = "nofap_liquid_glass_prefs"
    private const val KEY_START_TIME = "streak_start_time_millis"
    private const val KEY_GOAL_DAYS = "target_goal_days"
    private const val KEY_BEST_DAYS = "best_streak_days"
    private const val KEY_TOTAL_RELAPSES = "total_relapses"
    private const val KEY_LAST_PLEDGE_EPOCH_DAY = "last_pledge_epoch_day"
    private const val KEY_HISTORY_JSON = "streak_history_json"
    private const val KEY_JOURNAL_JSON = "streak_journal_json"

    private val QUOTES = listOf(
        "Discipline is choosing between what you want now and what you want most.",
        "Every urge conquered rewires your brain for focus, confidence, and self-mastery.",
        "You don't drown by falling in the water; you drown by staying there. Keep moving forward.",
        "Short-term impulse costs long-term peace. Protect your streak and your energy.",
        "True strength is quiet self-command when no one is watching.",
        "Your future self is counting on the decision you make in the next five minutes."
    )

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSnapshot(context: Context): StreakSnapshot {
        val p = prefs(context)
        val now = System.currentTimeMillis()
        var start = p.getLong(KEY_START_TIME, 0L)
        if (start <= 0L || start > now) {
            start = now
            p.edit().putLong(KEY_START_TIME, start).apply()
        }

        val elapsed = (now - start).coerceAtLeast(0L)
        val days = TimeUnit.MILLISECONDS.toDays(elapsed).toInt()
        val hours = (TimeUnit.MILLISECONDS.toHours(elapsed) % 24).toInt()
        val minutes = (TimeUnit.MILLISECONDS.toMinutes(elapsed) % 60).toInt()
        val seconds = (TimeUnit.MILLISECONDS.toSeconds(elapsed) % 60).toInt()

        val goalDays = p.getInt(KEY_GOAL_DAYS, 90).coerceAtLeast(1)
        val savedBest = p.getInt(KEY_BEST_DAYS, 0)
        val bestDays = maxOf(savedBest, days)
        if (bestDays > savedBest) {
            p.edit().putInt(KEY_BEST_DAYS, bestDays).apply()
        }

        val todayEpochDay = TimeUnit.MILLISECONDS.toDays(now)
        val pledgedToday = p.getLong(KEY_LAST_PLEDGE_EPOCH_DAY, -1L) == todayEpochDay
        val quote = QUOTES[(todayEpochDay.toInt().coerceAtLeast(0)) % QUOTES.size]

        return StreakSnapshot(
            startTimeMillis = start,
            elapsedMillis = elapsed,
            days = days,
            hours = hours,
            minutes = minutes,
            seconds = seconds,
            goalDays = goalDays,
            bestStreakDays = bestDays,
            totalRelapses = p.getInt(KEY_TOTAL_RELAPSES, 0),
            pledgedToday = pledgedToday,
            quoteOfTheDay = quote
        )
    }

    fun setStartDate(context: Context, newStartMillis: Long) {
        val safeStart = newStartMillis.coerceAtMost(System.currentTimeMillis())
        prefs(context).edit().putLong(KEY_START_TIME, safeStart).commit()
        syncWidgetsImmediately(context)
    }

    fun setGoalDays(context: Context, newGoalDays: Int) {
        prefs(context).edit().putInt(KEY_GOAL_DAYS, newGoalDays.coerceAtLeast(1)).commit()
        syncWidgetsImmediately(context)
    }

    fun markDailyPledge(context: Context): Boolean {
        val todayEpochDay = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
        prefs(context).edit().putLong(KEY_LAST_PLEDGE_EPOCH_DAY, todayEpochDay).commit()
        syncWidgetsImmediately(context)
        return true
    }

    fun resetStreak(context: Context, reason: String) {
        val snap = getSnapshot(context)
        val p = prefs(context)
        val historyArr = JSONArray(p.getString(KEY_HISTORY_JSON, "[]") ?: "[]")
        val item = JSONObject().apply {
            put("endedAtMillis", System.currentTimeMillis())
            put("durationDays", snap.days)
            put("reason", reason.ifBlank { "General Relapse" })
        }
        historyArr.put(item)

        p.edit()
            .putLong(KEY_START_TIME, System.currentTimeMillis())
            .putInt(KEY_BEST_DAYS, maxOf(snap.bestStreakDays, snap.days))
            .putInt(KEY_TOTAL_RELAPSES, snap.totalRelapses + 1)
            .putString(KEY_HISTORY_JSON, historyArr.toString())
            .commit()

        syncWidgetsImmediately(context)
    }

    fun getHistory(context: Context): List<StreakHistoryItem> {
        val raw = prefs(context).getString(KEY_HISTORY_JSON, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<StreakHistoryItem>()
        for (i in arr.length() - 1 downTo 0) {
            val obj = arr.optJSONObject(i) ?: continue
            list.add(
                StreakHistoryItem(
                    endedAtMillis = obj.optLong("endedAtMillis", 0L),
                    durationDays = obj.optInt("durationDays", 0),
                    reason = obj.optString("reason", "Relapse")
                )
            )
        }
        return list
    }

    fun addJournalEntry(context: Context, note: String) {
        if (note.isBlank()) return
        val p = prefs(context)
        val arr = JSONArray(p.getString(KEY_JOURNAL_JSON, "[]") ?: "[]")
        arr.put(JSONObject().apply {
            put("timestampMillis", System.currentTimeMillis())
            put("note", note.trim())
        })
        p.edit().putString(KEY_JOURNAL_JSON, arr.toString()).apply()
    }

    fun getJournalEntries(context: Context): List<JournalEntry> {
        val raw = prefs(context).getString(KEY_JOURNAL_JSON, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<JournalEntry>()
        for (i in arr.length() - 1 downTo 0) {
            val obj = arr.optJSONObject(i) ?: continue
            list.add(
                JournalEntry(
                    timestampMillis = obj.optLong("timestampMillis", 0L),
                    note = obj.optString("note", "")
                )
            )
        }
        return list
    }

    /**
     * Pushes a direct, synchronous 0ms AppWidgetManager update and schedules
     * the battery-free midnight rollover alarm.
     */
    fun syncWidgetsImmediately(context: Context) {
        StreakWidgetProvider.pushImmediateUpdate(context)
        MidnightRolloverReceiver.scheduleNextMidnightRollover(context)
    }
}
