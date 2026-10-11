package com.example.polyglottyper.domain.repository

import com.example.polyglottyper.domain.model.Achievement
import com.example.polyglottyper.domain.model.GameAggregates
import com.example.polyglottyper.domain.model.GameMode
import com.example.polyglottyper.domain.model.KeyboardLayout
import com.example.polyglottyper.domain.model.Language
import com.example.polyglottyper.domain.model.LanguageStats
import com.example.polyglottyper.domain.model.LayoutStats
import com.example.polyglottyper.domain.model.TypingResult
import com.example.polyglottyper.domain.model.WpmPoint
import kotlinx.coroutines.flow.Flow

interface WordRepository {
    /** [seed] makes generation deterministic (used by the Daily Challenge). */
    fun generateWords(language: Language, count: Int, seed: Long? = null): List<String>
}

interface StatsRepository {
    suspend fun saveResult(result: TypingResult)
    fun observeRecentResults(limit: Int = 50): Flow<List<TypingResult>>
    fun observeLanguageStats(): Flow<List<LanguageStats>>
    fun observeLayoutStats(): Flow<List<LayoutStats>>
    /** Most recent WPM readings, newest first. */
    fun observeWpmHistory(limit: Int = 30): Flow<List<WpmPoint>>
    suspend fun aggregates(): GameAggregates
}

interface AchievementsRepository {
    val unlocked: Flow<Set<Achievement>>
    /** Re-evaluates all rules against the latest stats and persists new unlocks. */
    suspend fun onGameCompleted()
}

interface UserPreferencesRepository {
    val preferredLanguage: Flow<Language>
    val preferredMode: Flow<GameMode>

    /** Null means "Automatic" — resolve with [KeyboardLayout.recommendedFor]. */
    val preferredLayout: Flow<KeyboardLayout?>
    val showKeyboard: Flow<Boolean>
    val remindersEnabled: Flow<Boolean>

    val totalXp: Flow<Int>
    val currentStreak: Flow<Int>
    val bestStreak: Flow<Int>
    /** Epoch day of the last completed Daily, or null. */
    val dailyCompletedOn: Flow<Long?>
    val unlockedAchievementIds: Flow<Set<String>>

    suspend fun setLanguage(language: Language)
    suspend fun setMode(mode: GameMode)
    suspend fun setLayout(layout: KeyboardLayout?)   // pass null for Automatic
    suspend fun setShowKeyboard(visible: Boolean)
    suspend fun setRemindersEnabled(enabled: Boolean)
    suspend fun addXp(amount: Int)
    /** Marks the given day's Daily done and rolls the streak. Returns (current, best). */
    suspend fun recordDailyCompleted(todayEpochDay: Long): Pair<Int, Int>
    suspend fun unlockAchievements(ids: Set<String>)
}
