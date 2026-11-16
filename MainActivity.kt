package com.tasnimanas.nofap.liquidglass.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.tasnimanas.nofap.liquidglass.R
import com.tasnimanas.nofap.liquidglass.data.StreakRepository
import com.tasnimanas.nofap.liquidglass.model.BadgeCatalog
import com.tasnimanas.nofap.liquidglass.widget.StreakWidgetProvider
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val uiHandler = Handler(Looper.getMainLooper())
    private var isBreathingActive = false
    private var breathingStep = 0

    private lateinit var tabTrackerContainer: View
    private lateinit var tabBadgesContainer: View
    private lateinit var tabHistoryContainer: View
    private lateinit var tabSosContainer: View

    private lateinit var navTabTracker: TextView
    private lateinit var navTabBadges: TextView
    private lateinit var navTabHistory: TextView
    private lateinit var navTabSos: TextView

    private lateinit var tvCurrentRankBadge: TextView
    private lateinit var tvMainDaysCount: TextView
    private lateinit var tvLiveClockBreakdown: TextView
    private lateinit var tvNextBadgeHint: TextView
    private lateinit var pbNextBadge: ProgressBar
    private lateinit var tvGoalStatus: TextView
    private lateinit var pbGoalProgress: ProgressBar
    private lateinit var tvBestStreakCard: TextView
    private lateinit var tvPledgeStatus: TextView
    private lateinit var tvDailyQuote: TextView

    private lateinit var badgesListContainer: LinearLayout
    private lateinit var tvAnalyticsSummary: TextView
    private lateinit var historyListContainer: LinearLayout
    private lateinit var tvBreathingPhase: TextView
    private lateinit var btnToggleBreathing: TextView
    private lateinit var etJournalInput: EditText
    private lateinit var journalListContainer: LinearLayout

    private val liveTickerRunnable = object : Runnable {
        override fun run() {
            refreshDashboardUi()
            // Runs ONLY while MainActivity is visible in foreground (stopped in onPause -> 0% background CPU)
            uiHandler.postDelayed(this, 1000L)
        }
    }

    private val breathingRunnable = object : Runnable {
        override fun run() {
            if (!isBreathingActive) return
            val cycleSec = breathingStep % 19
            when {
                cycleSec < 4 -> {
                    tvBreathingPhase.text = "🌬 Inhale Slowly Through Nose... (${4 - cycleSec}s)"
                }
                cycleSec < 11 -> {
                    tvBreathingPhase.text = "⏸ Hold Breath Calmly... (${11 - cycleSec}s)"
                }
                else -> {
                    tvBreathingPhase.text = "💨 Exhale Gently Through Mouth... (${19 - cycleSec}s)"
                }
            }
            breathingStep++
            uiHandler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setupClickListeners()
        StreakRepository.syncWidgetsImmediately(this)

        val initialTab = intent?.getIntExtra(EXTRA_OPEN_TAB, TAB_TRACKER) ?: TAB_TRACKER
        switchTab(initialTab)
    }

    override fun onResume() {
        super.onResume()
        uiHandler.removeCallbacks(liveTickerRunnable)
        uiHandler.post(liveTickerRunnable)
        populateBadgesList()
        populateHistoryAndJournal()
    }

    override fun onPause() {
        super.onPause()
        // Immediately stop foreground UI ticker when screen closes so background CPU usage is 0%
        uiHandler.removeCallbacks(liveTickerRunnable)
        stopBreathingCoach()
    }

    private fun bindViews() {
        tabTrackerContainer = findViewById(R.id.tabTrackerContainer)
        tabBadgesContainer = findViewById(R.id.tabBadgesContainer)
        tabHistoryContainer = findViewById(R.id.tabHistoryContainer)
        tabSosContainer = findViewById(R.id.tabSosContainer)

        navTabTracker = findViewById(R.id.navTabTracker)
        navTabBadges = findViewById(R.id.navTabBadges)
        navTabHistory = findViewById(R.id.navTabHistory)
        navTabSos = findViewById(R.id.navTabSos)

        tvCurrentRankBadge = findViewById(R.id.tvCurrentRankBadge)
        tvMainDaysCount = findViewById(R.id.tvMainDaysCount)
        tvLiveClockBreakdown = findViewById(R.id.tvLiveClockBreakdown)
        tvNextBadgeHint = findViewById(R.id.tvNextBadgeHint)
        pbNextBadge = findViewById(R.id.pbNextBadge)
        tvGoalStatus = findViewById(R.id.tvGoalStatus)
        pbGoalProgress = findViewById(R.id.pbGoalProgress)
        tvBestStreakCard = findViewById(R.id.tvBestStreakCard)
        tvPledgeStatus = findViewById(R.id.tvPledgeStatus)
        tvDailyQuote = findViewById(R.id.tvDailyQuote)

        badgesListContainer = findViewById(R.id.badgesListContainer)
        tvAnalyticsSummary = findViewById(R.id.tvAnalyticsSummary)
        historyListContainer = findViewById(R.id.historyListContainer)
        tvBreathingPhase = findViewById(R.id.tvBreathingPhase)
        btnToggleBreathing = findViewById(R.id.btnToggleBreathing)
        etJournalInput = findViewById(R.id.etJournalInput)
        journalListContainer = findViewById(R.id.journalListContainer)
    }

    private fun setupClickListeners() {
        navTabTracker.setOnClickListener { switchTab(TAB_TRACKER) }
        navTabBadges.setOnClickListener { switchTab(TAB_BADGES) }
        navTabHistory.setOnClickListener { switchTab(TAB_HISTORY) }
        navTabSos.setOnClickListener { switchTab(TAB_SOS) }

        findViewById<View>(R.id.btnPinWidget).setOnClickListener {
            requestPinLiquidGlassWidget()
        }

        findViewById<View>(R.id.btnDailyCheckIn).setOnClickListener {
            StreakRepository.markDailyPledge(this)
            refreshDashboardUi()
            Toast.makeText(
                this,
                "✓ Daily Pledge committed! Widget synced in 0ms.",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<View>(R.id.btnPickStartDate).setOnClickListener {
            showCustomStartDateTimePicker()
        }

        findViewById<View>(R.id.btnChangeGoal).setOnClickListener {
            showChangeGoalDialog()
        }

        findViewById<View>(R.id.btnResetStreak).setOnClickListener {
            showResetStreakDialog()
        }

        btnToggleBreathing.setOnClickListener {
            if (isBreathingActive) {
                stopBreathingCoach()
            } else {
                isBreathingActive = true
                breathingStep = 0
                btnToggleBreathing.text = "Stop Breathing Exercise"
                uiHandler.post(breathingRunnable)
            }
        }

        findViewById<View>(R.id.btnSaveJournal).setOnClickListener {
            val text = etJournalInput.text?.toString().orEmpty()
            if (text.isNotBlank()) {
                StreakRepository.addJournalEntry(this, text)
                etJournalInput.setText("")
                populateHistoryAndJournal()
                Toast.makeText(this, "Journal note saved offline.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun switchTab(tabIndex: Int) {
        tabTrackerContainer.visibility = if (tabIndex == TAB_TRACKER) View.VISIBLE else View.GONE
        tabBadgesContainer.visibility = if (tabIndex == TAB_BADGES) View.VISIBLE else View.GONE
        tabHistoryContainer.visibility = if (tabIndex == TAB_HISTORY) View.VISIBLE else View.GONE
        tabSosContainer.visibility = if (tabIndex == TAB_SOS) View.VISIBLE else View.GONE

        val tabs = listOf(
            navTabTracker to (tabIndex == TAB_TRACKER),
            navTabBadges to (tabIndex == TAB_BADGES),
            navTabHistory to (tabIndex == TAB_HISTORY),
            navTabSos to (tabIndex == TAB_SOS)
        )
        for ((view, active) in tabs) {
            if (active) {
                view.setBackgroundResource(R.drawable.bg_liquid_glass_pill)
                view.setTextColor(ContextCompat.getColor(this, R.color.accent_cyan))
            } else {
                view.background = null
                view.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            }
        }

        if (tabIndex == TAB_BADGES) populateBadgesList()
        if (tabIndex == TAB_HISTORY || tabIndex == TAB_SOS) populateHistoryAndJournal()
    }

    private fun refreshDashboardUi() {
        val snap = StreakRepository.getSnapshot(this)
        val badge = BadgeCatalog.currentBadge(snap.days)
        val nextBadge = BadgeCatalog.nextBadge(snap.days)
        val nextPct = BadgeCatalog.progressToNextBadge(snap.days)
        val goalPct = ((snap.days * 100f) / snap.goalDays).toInt().coerceIn(0, 100)

        tvCurrentRankBadge.text = "${badge.emoji} Rank: ${badge.title}"
        tvMainDaysCount.text = snap.days.toString()
        tvLiveClockBreakdown.text = String.format(
            Locale.US,
            "%02dh : %02dm : %02ds",
            snap.hours,
            snap.minutes,
            snap.seconds
        )

        if (nextBadge != null) {
            val remaining = (nextBadge.requiredDays - snap.days).coerceAtLeast(1)
            tvNextBadgeHint.text = "Next Rank: ${nextBadge.emoji} ${nextBadge.title} in $remaining day(s) ($nextPct%)"
        } else {
            tvNextBadgeHint.text = "👑 Ultimate Rank Reached: Giga Chad!"
        }
        pbNextBadge.progress = nextPct

        tvGoalStatus.text = "${snap.days} / ${snap.goalDays} Days ($goalPct%)"
        pbGoalProgress.progress = goalPct
        tvBestStreakCard.text = "${snap.bestStreakDays} Days"
        tvPledgeStatus.text = if (snap.pledgedToday) "✓ Pledged for today" else "Tap Daily Pledge below"
        tvDailyQuote.text = "“${snap.quoteOfTheDay}”"
    }

    private fun populateBadgesList() {
        val snap = StreakRepository.getSnapshot(this)
        badgesListContainer.removeAllViews()
        val density = resources.displayMetrics.density

        for (badge in BadgeCatalog.ALL_BADGES) {
            val unlocked = snap.days >= badge.requiredDays
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(
                    if (unlocked) R.drawable.bg_liquid_glass_hero else R.drawable.bg_liquid_glass_card
                )
                val pad = (16 * density).toInt()
                setPadding(pad, pad, pad, pad)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (10 * density).toInt()
                }
            }

            val titleView = TextView(this).apply {
                text = "${badge.emoji}  ${badge.title}  •  Day ${badge.requiredDays}+  " +
                    if (unlocked) "[UNLOCKED]" else "[LOCKED]"
                textSize = 15f
                setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        if (unlocked) R.color.accent_cyan else R.color.text_muted
                    )
                )
            }

            val descView = TextView(this).apply {
                text = badge.subtitle
                textSize = 13f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
                setPadding(0, (4 * density).toInt(), 0, 0)
            }

            card.addView(titleView)
            card.addView(descView)
            badgesListContainer.addView(card)
        }
    }

    private fun populateHistoryAndJournal() {
        val snap = StreakRepository.getSnapshot(this)
        val history = StreakRepository.getHistory(this)
        val fmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val density = resources.displayMetrics.density

        val topTrigger = history.groupingBy { it.reason }.eachCount().maxByOrNull { it.value }?.key ?: "None logged yet"
        tvAnalyticsSummary.text = buildString {
            append("• Current Streak: ${snap.days} Days (${snap.hours}h ${snap.minutes}m)
")
            append("• Best All-Time Streak: ${snap.bestStreakDays} Days
")
            append("• Total Resets Logged: ${snap.totalRelapses}
")
            append("• Most Common Trigger: $topTrigger")
        }

        historyListContainer.removeAllViews()
        if (history.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No relapses logged yet. Keep your clean streak strong!"
                textSize = 13f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_muted))
            }
            historyListContainer.addView(emptyTv)
        } else {
            for (item in history) {
                val row = TextView(this).apply {
                    setBackgroundResource(R.drawable.bg_liquid_glass_card)
                    val pad = (14 * density).toInt()
                    setPadding(pad, pad, pad, pad)
                    text = "Streak: ${item.durationDays}d  •  Trigger: ${item.reason}
Ended: ${fmt.format(Date(item.endedAtMillis))}"
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = (8 * density).toInt()
                    }
                }
                historyListContainer.addView(row)
            }
        }

        journalListContainer.removeAllViews()
        val notes = StreakRepository.getJournalEntries(this)
        for (entry in notes) {
            val noteTv = TextView(this).apply {
                setBackgroundResource(R.drawable.bg_liquid_glass_pill)
                val pad = (12 * density).toInt()
                setPadding(pad, pad, pad, pad)
                text = "${fmt.format(Date(entry.timestampMillis))}
${entry.note}"
                textSize = 13f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (8 * density).toInt()
                }
            }
            journalListContainer.addView(noteTv)
        }
    }

    private fun showCustomStartDateTimePicker() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = StreakRepository.getSnapshot(this).startTimeMillis

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        val picked = Calendar.getInstance().apply {
                            set(year, month, dayOfMonth, hourOfDay, minute, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis

                        if (picked > System.currentTimeMillis()) {
                            Toast.makeText(this, "Start date cannot be in the future.", Toast.LENGTH_SHORT).show()
                        } else {
                            StreakRepository.setStartDate(this, picked)
                            refreshDashboardUi()
                            populateBadgesList()
                            Toast.makeText(this, "Start date updated! Widget synced in 0ms.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    false
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    private fun showChangeGoalDialog() {
        val presets = arrayOf("7 Days (1 Week)", "14 Days (2 Weeks)", "30 Days (1 Month)", "60 Days (2 Months)", "90 Days (Standard Reboot)", "180 Days (Half Year)", "365 Days (Full Year)")
        val values = intArrayOf(7, 14, 30, 60, 90, 180, 365)

        AlertDialog.Builder(this)
            .setTitle("Select Target Streak Goal")
            .setItems(presets) { _, which ->
                StreakRepository.setGoalDays(this, values[which])
                refreshDashboardUi()
                Toast.makeText(this, "Goal updated to ${values[which]} days! Widget synced.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showResetStreakDialog() {
        val reasons = arrayOf(
            "Stress / Anxiety",
            "Late Night Browsing",
            "Boredom / Alone",
            "Social Media Trigger",
            "Lack of Sleep",
            "Other"
        )
        AlertDialog.Builder(this)
            .setTitle("Reset Streak & Log Trigger?")
            .setItems(reasons) { _, which ->
                StreakRepository.resetStreak(this, reasons[which])
                refreshDashboardUi()
                populateBadgesList()
                populateHistoryAndJournal()
                Toast.makeText(this, "Streak reset logged. Keep going—widget synced in 0ms.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestPinLiquidGlassWidget() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = getSystemService(AppWidgetManager::class.java)
            val provider = ComponentName(this, StreakWidgetProvider::class.java)
            if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                appWidgetManager.requestPinAppWidget(provider, null, null)
                return
            }
        }
        Toast.makeText(
            this,
            "Long-press your Home Screen -> Widgets -> NoFap Glass to add & resize the widget.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun stopBreathingCoach() {
        isBreathingActive = false
        uiHandler.removeCallbacks(breathingRunnable)
        btnToggleBreathing.text = "Start 4-7-8 Breathing"
        tvBreathingPhase.text = "Tap below to start 4-7-8 Tactical Breathing"
    }

    companion object {
        const val EXTRA_OPEN_TAB = "extra_open_tab"
        const val TAB_TRACKER = 0
        const val TAB_BADGES = 1
        const val TAB_HISTORY = 2
        const val TAB_SOS = 3
    }
}
