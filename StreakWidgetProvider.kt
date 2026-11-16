package com.tasnimanas.nofap.liquidglass.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.SizeF
import android.util.TypedValue
import android.widget.RemoteViews
import android.widget.Toast
import com.tasnimanas.nofap.liquidglass.R
import com.tasnimanas.nofap.liquidglass.data.StreakRepository
import com.tasnimanas.nofap.liquidglass.data.StreakSnapshot
import com.tasnimanas.nofap.liquidglass.model.BadgeCatalog
import com.tasnimanas.nofap.liquidglass.ui.MainActivity

class StreakWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val snapshot = StreakRepository.getSnapshot(context)
        for (appWidgetId in appWidgetIds) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            updateSingleWidget(context, appWidgetManager, appWidgetId, options, snapshot)
        }
        MidnightRolloverReceiver.scheduleNextMidnightRollover(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val snapshot = StreakRepository.getSnapshot(context)
        updateSingleWidget(context, appWidgetManager, appWidgetId, newOptions, snapshot)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_INSTANT_WIDGET_SYNC -> {
                pushImmediateUpdate(context)
            }
            ACTION_WIDGET_CHECK_IN -> {
                StreakRepository.markDailyPledge(context)
                Toast.makeText(context, "✓ Daily Pledge Recorded!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val ACTION_INSTANT_WIDGET_SYNC = "com.tasnimanas.nofap.liquidglass.ACTION_INSTANT_WIDGET_SYNC"
        const val ACTION_WIDGET_CHECK_IN = "com.tasnimanas.nofap.liquidglass.ACTION_WIDGET_CHECK_IN"

        /**
         * 0ms synchronous IPC widget refresh called whenever the user modifies streak data.
         */
        fun pushImmediateUpdate(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val component = ComponentName(appContext, StreakWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return

            val snapshot = StreakRepository.getSnapshot(appContext)
            for (id in ids) {
                val options = manager.getAppWidgetOptions(id)
                updateSingleWidget(appContext, manager, id, options, snapshot)
            }
        }

        private fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            options: Bundle?,
            snapshot: StreakSnapshot
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ native multi-size responsive map: morphs shape & scales elements smoothly as user drags widget handles
                val sizeMap = mapOf(
                    SizeF(110f, 50f) to buildSizedRemoteViews(context, 110, 55, snapshot),
                    SizeF(180f, 65f) to buildSizedRemoteViews(context, 180, 65, snapshot),
                    SizeF(150f, 140f) to buildSizedRemoteViews(context, 150, 140, snapshot),
                    SizeF(210f, 180f) to buildSizedRemoteViews(context, 210, 180, snapshot),
                    SizeF(260f, 110f) to buildSizedRemoteViews(context, 260, 110, snapshot),
                    SizeF(320f, 130f) to buildSizedRemoteViews(context, 320, 130, snapshot),
                    SizeF(260f, 220f) to buildSizedRemoteViews(context, 260, 220, snapshot),
                    SizeF(340f, 320f) to buildSizedRemoteViews(context, 340, 320, snapshot)
                )
                appWidgetManager.updateAppWidget(appWidgetId, RemoteViews(sizeMap))
            } else {
                val minW = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 150) ?: 150
                val minH = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 140) ?: 140
                val views = buildSizedRemoteViews(context, minW, minH, snapshot)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private fun buildSizedRemoteViews(
            context: Context,
            widthDp: Int,
            heightDp: Int,
            snapshot: StreakSnapshot
        ): RemoteViews {
            val badge = BadgeCatalog.currentBadge(snapshot.days)
            val nextBadge = BadgeCatalog.nextBadge(snapshot.days)
            val goalPct = ((snapshot.days * 100f) / snapshot.goalDays).toInt().coerceIn(0, 100)

            // Native Chronometer base anchored to SystemClock.elapsedRealtime() -> 0% background battery drain
            val chronometerBase = SystemClock.elapsedRealtime() - snapshot.elapsedMillis

            // PendingIntents for 0ms interactive buttons
            val openAppIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val openSosIntent = PendingIntent.getActivity(
                context,
                1,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(MainActivity.EXTRA_OPEN_TAB, MainActivity.TAB_SOS)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val checkInIntent = PendingIntent.getBroadcast(
                context,
                2,
                Intent(context, StreakWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_CHECK_IN
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Choose layout shape based on widget dimensions & compute proportional font scale factor
            return when {
                // 1. Compact Pill Shape (2x1)
                heightDp < 105 && widthDp < 235 -> {
                    val scale = (widthDp / 140f).coerceIn(0.88f, 1.35f)
                    RemoteViews(context.packageName, R.layout.widget_streak_pill).apply {
                        setTextViewText(R.id.widgetBadgeEmoji, badge.emoji)
                        setTextViewText(R.id.widgetDaysValue, "${snapshot.days}d")
                        setTextViewText(R.id.widgetRankTitle, badge.title)
                        setChronometer(R.id.widgetChronometer, chronometerBase, null, true)

                        setTextViewTextSize(R.id.widgetBadgeEmoji, TypedValue.COMPLEX_UNIT_SP, 17f * scale)
                        setTextViewTextSize(R.id.widgetDaysValue, TypedValue.COMPLEX_UNIT_SP, 18f * scale)
                        setTextViewTextSize(R.id.widgetRankTitle, TypedValue.COMPLEX_UNIT_SP, 12f * scale)
                        setTextViewTextSize(R.id.widgetChronometer, TypedValue.COMPLEX_UNIT_SP, 11f * scale)

                        setOnClickPendingIntent(R.id.widgetRoot, openAppIntent)
                    }
                }

                // 2. Wide Panoramic Glass Bar (4x1 / 4x2)
                heightDp < 185 && widthDp >= 235 -> {
                    val scale = (minOf(widthDp / 260f, heightDp / 110f)).coerceIn(0.92f, 1.40f)
                    RemoteViews(context.packageName, R.layout.widget_streak_wide).apply {
                        setTextViewText(R.id.widgetDaysValue, snapshot.days.toString())
                        setTextViewText(R.id.widgetRankTitle, "${badge.emoji} Rank: ${badge.title}")
                        setChronometer(R.id.widgetChronometer, chronometerBase, null, true)
                        setProgressBar(R.id.widgetProgressBar, 100, goalPct, false)
                        setTextViewText(
                            R.id.widgetGoalSubtext,
                            "Goal: ${snapshot.days} / ${snapshot.goalDays}d ($goalPct%)"
                        )
                        setTextViewText(
                            R.id.widgetCheckInBtn,
                            if (snapshot.pledgedToday) "✓ Pledged" else "✓ Pledge"
                        )

                        setTextViewTextSize(R.id.widgetDaysValue, TypedValue.COMPLEX_UNIT_SP, 36f * scale)
                        setTextViewTextSize(R.id.widgetRankTitle, TypedValue.COMPLEX_UNIT_SP, 14f * scale)
                        setTextViewTextSize(R.id.widgetChronometer, TypedValue.COMPLEX_UNIT_SP, 12f * scale)
                        setTextViewTextSize(R.id.widgetGoalSubtext, TypedValue.COMPLEX_UNIT_SP, 11f * scale)

                        setOnClickPendingIntent(R.id.widgetRoot, openAppIntent)
                        setOnClickPendingIntent(R.id.widgetCheckInBtn, checkInIntent)
                    }
                }

                // 3. Rounded Squircle Glass Card (2x2 / 3x2)
                widthDp < 235 -> {
                    val scale = (minOf(widthDp / 150f, heightDp / 140f)).coerceIn(0.90f, 1.45f)
                    RemoteViews(context.packageName, R.layout.widget_streak_square).apply {
                        setTextViewText(R.id.widgetRankTitle, "${badge.emoji} ${badge.title}")
                        setTextViewText(R.id.widgetDaysValue, snapshot.days.toString())
                        setChronometer(R.id.widgetChronometer, chronometerBase, null, true)
                        setProgressBar(R.id.widgetProgressBar, 100, goalPct, false)

                        setTextViewTextSize(R.id.widgetRankTitle, TypedValue.COMPLEX_UNIT_SP, 11f * scale)
                        setTextViewTextSize(R.id.widgetDaysValue, TypedValue.COMPLEX_UNIT_SP, 34f * scale)
                        setTextViewTextSize(R.id.widgetDaysLabel, TypedValue.COMPLEX_UNIT_SP, 10f * scale)
                        setTextViewTextSize(R.id.widgetChronometer, TypedValue.COMPLEX_UNIT_SP, 11f * scale)

                        setOnClickPendingIntent(R.id.widgetRoot, openAppIntent)
                    }
                }

                // 4. Expanded Large Command Panel (3x3 / 4x4+)
                else -> {
                    val scale = (minOf(widthDp / 260f, heightDp / 230f)).coerceIn(0.95f, 1.45f)
                    RemoteViews(context.packageName, R.layout.widget_streak_large).apply {
                        setTextViewText(R.id.widgetRankTitle, "${badge.emoji} Rank: ${badge.title}")
                        setTextViewText(
                            R.id.widgetNextRankText,
                            if (nextBadge != null) "Next: ${nextBadge.emoji} ${nextBadge.title} (${nextBadge.requiredDays}d)"
                            else "Max Rank Unlocked!"
                        )
                        setTextViewText(R.id.widgetDaysValue, snapshot.days.toString())
                        setChronometer(R.id.widgetChronometer, chronometerBase, null, true)
                        setProgressBar(R.id.widgetProgressBar, 100, goalPct, false)
                        setTextViewText(
                            R.id.widgetGoalSubtext,
                            "Goal: ${snapshot.days} / ${snapshot.goalDays} Days ($goalPct%)"
                        )
                        setTextViewText(R.id.widgetBestStreakText, "Best: ${snapshot.bestStreakDays}d")
                        setTextViewText(R.id.widgetQuoteText, "“${snapshot.quoteOfTheDay}”")
                        setTextViewText(
                            R.id.widgetCheckInBtn,
                            if (snapshot.pledgedToday) "✓ Pledged Today" else "✓ Daily Pledge"
                        )

                        setTextViewTextSize(R.id.widgetRankTitle, TypedValue.COMPLEX_UNIT_SP, 14f * scale)
                        setTextViewTextSize(R.id.widgetDaysValue, TypedValue.COMPLEX_UNIT_SP, 52f * scale)
                        setTextViewTextSize(R.id.widgetDaysLabel, TypedValue.COMPLEX_UNIT_SP, 12f * scale)
                        setTextViewTextSize(R.id.widgetChronometer, TypedValue.COMPLEX_UNIT_SP, 14f * scale)
                        setTextViewTextSize(R.id.widgetQuoteText, TypedValue.COMPLEX_UNIT_SP, 11f * scale)

                        setOnClickPendingIntent(R.id.widgetRoot, openAppIntent)
                        setOnClickPendingIntent(R.id.widgetCheckInBtn, checkInIntent)
                        setOnClickPendingIntent(R.id.widgetSosBtn, openSosIntent)
                    }
                }
            }
        }
    }
}
