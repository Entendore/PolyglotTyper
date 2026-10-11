package com.example.polyglottyper.ui.common

import androidx.annotation.StringRes
import com.example.polyglottyper.R
import com.example.polyglottyper.domain.model.Achievement

/** Presentation mapping: domain achievement → emoji + localized strings. */
data class AchievementInfo(
    val emoji: String,
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
)

fun achievementInfo(achievement: Achievement): AchievementInfo = when (achievement) {
    Achievement.FIRST_GAME -> AchievementInfo("🎯", R.string.ach_first_game_t, R.string.ach_first_game_d)
    Achievement.GAMES_10 -> AchievementInfo("🔟", R.string.ach_games_10_t, R.string.ach_games_10_d)
    Achievement.GAMES_50 -> AchievementInfo("🏅", R.string.ach_games_50_t, R.string.ach_games_50_d)
    Achievement.WPM_40 -> AchievementInfo("🚀", R.string.ach_wpm_40_t, R.string.ach_wpm_40_d)
    Achievement.WPM_60 -> AchievementInfo("⚡", R.string.ach_wpm_60_t, R.string.ach_wpm_60_d)
    Achievement.WPM_80 -> AchievementInfo("👑", R.string.ach_wpm_80_t, R.string.ach_wpm_80_d)
    Achievement.PERFECT_10 -> AchievementInfo("💯", R.string.ach_perfect_t, R.string.ach_perfect_d)
    Achievement.POLYGLOT_3 -> AchievementInfo("🌍", R.string.ach_poly3_t, R.string.ach_poly3_d)
    Achievement.POLYGLOT_ALL -> AchievementInfo("🏆", R.string.ach_polyall_t, R.string.ach_polyall_d)
    Achievement.LAYOUT_EXPLORER -> AchievementInfo("⌨️", R.string.ach_layouts_t, R.string.ach_layouts_d)
    Achievement.STREAK_3 -> AchievementInfo("🔥", R.string.ach_streak3_t, R.string.ach_streak3_d)
    Achievement.STREAK_7 -> AchievementInfo("🌋", R.string.ach_streak7_t, R.string.ach_streak7_d)
    Achievement.RAIN_50 -> AchievementInfo("☔", R.string.ach_rain50_t, R.string.ach_rain50_d)
    Achievement.RAIN_SURVIVOR -> AchievementInfo("⏱️", R.string.ach_rain2m_t, R.string.ach_rain2m_d)
}
