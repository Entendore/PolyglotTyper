package com.example.polyglottyper.domain.usecase

import com.example.polyglottyper.domain.model.Achievement
import com.example.polyglottyper.domain.model.GameMode
import com.example.polyglottyper.domain.model.RewardInfo
import com.example.polyglottyper.domain.model.TypingResult
import com.example.polyglottyper.domain.repository.AchievementsRepository
import com.example.polyglottyper.domain.repository.StatsRepository
import com.example.polyglottyper.domain.repository.UserPreferencesRepository
import com.example.polyglottyper.util.LevelMath
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * The single pipeline every game mode funnels through on completion:
 * persist → award XP → roll the daily streak → re-evaluate achievements.
 * Returns what the player earned so the UI can celebrate it.
 */
class RecordGameResult @Inject constructor(
    private val statsRepository: StatsRepository,
    private val userPreferences: UserPreferencesRepository,
    private val achievementsRepository: AchievementsRepository,
) {
    /**
     * [epochDay] is the day the game STARTED, captured by the caller. A Daily
     * finished after midnight must still count for the day it was seeded on.
     */
    suspend operator fun invoke(
        result: TypingResult,
        epochDay: Long = LocalDate.now().toEpochDay(),
    ): RewardInfo {
        // A session with zero submissions carries no information — recording it
        // would pollute stats, the sparkline, and achievement counters.
        if (result.correctWords == 0 && result.incorrectWords == 0) {
            return RewardInfo(xpEarned = 0, leveledUpTo = null, newAchievements = emptyList())
        }

        val xpBefore = userPreferences.totalXp.first()
        val unlockedBefore = userPreferences.unlockedAchievementIds.first()

        statsRepository.saveResult(result)

        val xpEarned = LevelMath.xpFor(result)
        userPreferences.addXp(xpEarned)

        if (result.mode is GameMode.DailyChallenge) {
            userPreferences.recordDailyCompleted(epochDay)
        }

        achievementsRepository.onGameCompleted()

        val newlyUnlocked = (userPreferences.unlockedAchievementIds.first() - unlockedBefore)
            .mapNotNull(Achievement::fromId)
        val leveledUpTo = LevelMath.levelFor(xpBefore + xpEarned)
            .takeIf { it > LevelMath.levelFor(xpBefore) }

        return RewardInfo(xpEarned, leveledUpTo, newlyUnlocked)
    }
}
