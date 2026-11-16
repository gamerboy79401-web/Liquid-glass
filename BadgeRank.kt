package com.tasnimanas.nofap.liquidglass.model

data class BadgeRank(
    val title: String,
    val emoji: String,
    val requiredDays: Int,
    val subtitle: String
)

object BadgeCatalog {
    val ALL_BADGES = listOf(
        BadgeRank("Seedling", "🌱", 0, "The commitment is made. Every second counts."),
        BadgeRank("Initiate", "⚡", 1, "First 24 hours conquered. Momentum is building."),
        BadgeRank("Spark", "🔥", 3, "Day 3 milestone. Dopamine sensitivity begins resetting."),
        BadgeRank("Challenger", "🛡", 7, "1 Full Week clean! Self-control is strengthening."),
        BadgeRank("Warrior", "⚔️", 14, "2 Weeks of focus, sharper energy, and discipline."),
        BadgeRank("Knight", "👑", 30, "30-Day milestone unlocked. Neural pathways rewiring."),
        BadgeRank("Master", "💎", 60, "60 Days of relentless clarity and emotional resilience."),
        BadgeRank("Reboot Legend", "🚀", 90, "90-Day standard reboot complete!"),
        BadgeRank("Titan", "🌟", 180, "Half a year of unbreakable self-mastery."),
        BadgeRank("Giga Chad", "🦁", 365, "365+ Days. Complete lifestyle transformation.")
    )

    fun currentBadge(days: Int): BadgeRank {
        return ALL_BADGES.lastOrNull { days >= it.requiredDays } ?: ALL_BADGES.first()
    }

    fun nextBadge(days: Int): BadgeRank? {
        return ALL_BADGES.firstOrNull { days < it.requiredDays }
    }

    fun progressToNextBadge(days: Int): Int {
        val current = currentBadge(days)
        val next = nextBadge(days) ?: return 100
        val span = (next.requiredDays - current.requiredDays).coerceAtLeast(1)
        val earned = (days - current.requiredDays).coerceAtLeast(0)
        return ((earned * 100f) / span).toInt().coerceIn(0, 100)
    }
}
