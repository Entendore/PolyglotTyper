package com.example.polyglottyper.domain.model

/** One-shot numeric rollup of the whole history, for achievement evaluation. */
data class GameAggregates(
    val totalGames: Int,
    val bestWpm: Int,
    val bestPerfectRun: Int,      // max correct words in a 100%-accuracy game
    val languagesPlayed: Int,
    val layoutsPlayed: Int,
    val bestRainSeconds: Int,
    val bestRainWords: Int,
)

data class PlayerSnapshot(
    val totalGames: Int,
    val bestWpm: Int,
    val bestPerfectRun: Int,
    val languagesPlayed: Int,
    val layoutsPlayed: Int,
    val bestStreak: Int,
    val bestRainSeconds: Int,
    val bestRainWords: Int,
)

/** Rules are predicates on a snapshot — trivially unit-testable. */
enum class Achievement(val id: String, val isUnlockedBy: (PlayerSnapshot) -> Boolean) {
    FIRST_GAME("first_game", { it.totalGames >= 1 }),
    GAMES_10("games_10", { it.totalGames >= 10 }),
    GAMES_50("games_50", { it.totalGames >= 50 }),
    WPM_40("wpm_40", { it.bestWpm >= 40 }),
    WPM_60("wpm_60", { it.bestWpm >= 60 }),
    WPM_80("wpm_80", { it.bestWpm >= 80 }),
    PERFECT_10("perfect_10", { it.bestPerfectRun >= 10 }),
    POLYGLOT_3("polyglot_3", { it.languagesPlayed >= 3 }),
    POLYGLOT_ALL("polyglot_all", { it.languagesPlayed >= Language.entries.size }),
    LAYOUT_EXPLORER("layout_explorer", { it.layoutsPlayed >= 3 }),
    STREAK_3("streak_3", { it.bestStreak >= 3 }),
    STREAK_7("streak_7", { it.bestStreak >= 7 }),
    RAIN_50("rain_50", { it.bestRainWords >= 50 }),
    RAIN_SURVIVOR("rain_survivor", { it.bestRainSeconds >= 120 });

    companion object {
        fun fromId(id: String): Achievement? = entries.firstOrNull { it.id == id }
    }
}

/** What the player earned from a finished game — drives the results celebration. */
data class RewardInfo(
    val xpEarned: Int,
    val leveledUpTo: Int?,                       // null = no level change
    val newAchievements: List<Achievement>,      // only unlocks from THIS game
)
