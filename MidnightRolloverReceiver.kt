package com.tasnimanas.nofap.liquidglass.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

class MidnightRolloverReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        StreakWidgetProvider.pushImmediateUpdate(context)
        scheduleNextMidnightRollover(context)
    }

    companion object {
        private const val ACTION_MIDNIGHT_ROLLOVER =
            "com.tasnimanas.nofap.liquidglass.ACTION_MIDNIGHT_ROLLOVER"

        fun scheduleNextMidnightRollover(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val nextMidnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 5)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                9001,
                Intent(context, MidnightRolloverReceiver::class.java).apply {
                    action = ACTION_MIDNIGHT_ROLLOVER
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Uses inexact idle-safe window so Android requires zero special exact-alarm permissions and zero wake locks
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC,
                nextMidnight,
                pendingIntent
            )
        }
    }
}
