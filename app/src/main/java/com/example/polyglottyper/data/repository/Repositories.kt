package com.example.polyglottyper.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.polyglottyper.data.local.GameResultDao
import com.example.polyglottyper.data.local.GameResultEntity
import com.example.polyglottyper.data.words.WordLists
import com.example.polyglottyper.domain.model.Achievement
import com.example.polyglottyper.domain.model.GameAggregates
import com.example.polyglottyper.domain.model.GameMode
import com.example.polyglottyper.domain.model.KeyboardLayout
import com.example.polyglottyper.domain.model.Language
import com.example.polyglottyper.domain.model.LanguageStats
import com.example.polyglottyper.domain.model.LayoutStats
import com.example.polyglottyper.domain.model.PlayerSnapshot
import com.example.polyglottyper.domain.model.TypingResult
import com.example.polyglottyper.domain.model.WpmPoint
import com.example.polyglottyper.domain.repository.AchievementsRepository
import com.example.polyglottyper.domain.repository.StatsRepository
import com.example.polyglottyper.domain.repository.UserPreferencesRepository
import com.example.polyglottyper.domain.repository.WordRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.random.Random

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

/** Stored when the player selects "Automatic" for the keyboard layout. */
private const val AUTO_LAYOUT_ID = "auto"

@Singleton
class WordRepositoryImpl @Inject constructor() : WordRepository {
    override fun generateWords(language: Language, count: Int, seed: Long?): List<String> {
        val pool = WordLists.forLanguage(language)
        val random = seed?.let(::Random) ?: Random.Default
        return List(count) { pool.random(random) }
    }
}

@Singleton
class StatsRepositoryImpl @Inject constructor(
    private val dao: GameResultDao,
) : StatsRepository {

    // Block body: the DAO insert returns the row id, but the interface
    // contract is Unit — we simply ignore it.
    override suspend fun saveResult(result: TypingResult) {
        dao.insert(result.toEntity())
    }

    override fun observeRecentResults(limit: Int): Flow<List<TypingResult>> =
        dao.observeRecentResults(limit).map { rows -> rows.map { it.toDomain() } }

    override fun observeLanguageStats(): Flow<List<LanguageStats>> =
        dao.observeLanguageAggregates().map { rows ->
            rows.map {
                LanguageStats(
                    languageCode = it.languageCode,
                    bestWpm = it.bestWpm,
                    avgAccuracy = it.avgAccuracy.roundToInt(),
                    gamesPlayed = it.gamesPlayed,
                )
            }
        }

    override fun observeLayoutStats(): Flow<List<LayoutStats>> =
        dao.observeLayoutAggregates().map { rows ->
            rows.map {
                LayoutStats(
                    layoutId = it.layoutId,
                    bestWpm = it.bestWpm,
                    avgAccuracy = it.avgAccuracy.roundToInt(),
                    gamesPlayed = it.gamesPlayed,
                )
            }
        }

    override fun observeWpmHistory(limit: Int): Flow<List<WpmPoint>> =
        dao.observeWpmHistory(limit)

    override suspend fun aggregates() = GameAggregates(
        totalGames = dao.countAll(),
        bestWpm = dao.maxWpm() ?: 0,
        bestPerfectRun = dao.maxPerfectRun() ?: 0,
        languagesPlayed = dao.countLanguages(),
        layoutsPlayed = dao.countLayouts(),
        bestRainSeconds = dao.maxRainSeconds() ?: 0,
        bestRainWords = dao.maxRainWords() ?: 0,
    )

    private fun TypingResult.toEntity() = GameResultEntity(
        languageCode = language.code,
        layoutId = layout.id,
        modeId = mode.id,
        wpm = wpm,
        accuracy = accuracy,
        correctWords = correctWords,
        incorrectWords = incorrectWords,
        durationSeconds = durationSeconds,
        timestamp = timestamp,
    )

    private fun GameResultEntity.toDomain() = TypingResult(
        id = id,
        language = Language.fromCode(languageCode),
        layout = KeyboardLayout.fromId(layoutId),
        mode = GameMode.fromId(modeId),
        wpm = wpm,
        accuracy = accuracy,
        correctWords = correctWords,
        incorrectWords = incorrectWords,
        durationSeconds = durationSeconds,
        timestamp = timestamp,
    )
}

@Singleton
class AchievementsRepositoryImpl @Inject constructor(
    private val statsRepository: StatsRepository,
    private val prefs: UserPreferencesRepository,
) : AchievementsRepository {

    override val unlocked: Flow<Set<Achievement>> =
        prefs.unlockedAchievementIds.map { ids ->
            Achievement.entries.filter { it.id in ids }.toSet()
        }

    override suspend fun onGameCompleted() {
        val agg = statsRepository.aggregates()
        val snapshot = PlayerSnapshot(
            totalGames = agg.totalGames,
            bestWpm = agg.bestWpm,
            bestPerfectRun = agg.bestPerfectRun,
            languagesPlayed = agg.languagesPlayed,
            layoutsPlayed = agg.layoutsPlayed,
            bestStreak = prefs.bestStreak.first(),
            bestRainSeconds = agg.bestRainSeconds,
            bestRainWords = agg.bestRainWords,
        )
        val earned = Achievement.entries.filter { it.isUnlockedBy(snapshot) }.map { it.id }.toSet()
        val alreadyUnlocked = prefs.unlockedAchievementIds.first()
        prefs.unlockAchievements(earned - alreadyUnlocked)
    }
}

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : UserPreferencesRepository {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val MODE = stringPreferencesKey("game_mode")
        val LAYOUT = stringPreferencesKey("keyboard_layout")
        val SHOW_KEYBOARD = booleanPreferencesKey("show_keyboard")
        val REMINDERS = booleanPreferencesKey("daily_reminders_enabled")
        val XP = intPreferencesKey("total_xp")
        val STREAK = intPreferencesKey("daily_streak")
        val BEST_STREAK = intPreferencesKey("best_streak")
        val DAILY_DONE = longPreferencesKey("daily_completed_on")
        val ACHIEVEMENTS = stringSetPreferencesKey("achievements")
    }

    private val preferences = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    // ── Flows ────────────────────────────────────────────────

    override val preferredLanguage: Flow<Language> =
        preferences.map { it[Keys.LANGUAGE]?.let(Language::fromCode) ?: Language.ENGLISH }

    override val preferredMode: Flow<GameMode> =
        preferences.map { it[Keys.MODE]?.let(GameMode::fromId) ?: GameMode.Timed(60) }

    override val preferredLayout: Flow<KeyboardLayout?> =
        preferences.map { prefs ->
            when (val id = prefs[Keys.LAYOUT]) {
                null, AUTO_LAYOUT_ID -> null                      // Automatic
                else -> KeyboardLayout.fromId(id)                 // explicit choice
            }
        }

    override val showKeyboard: Flow<Boolean> =
        preferences.map { it[Keys.SHOW_KEYBOARD] ?: true }

    override val remindersEnabled: Flow<Boolean> =
        preferences.map { it[Keys.REMINDERS] ?: true }

    override val totalXp: Flow<Int> = preferences.map { it[Keys.XP] ?: 0 }
    override val currentStreak: Flow<Int> = preferences.map { it[Keys.STREAK] ?: 0 }
    override val bestStreak: Flow<Int> = preferences.map { it[Keys.BEST_STREAK] ?: 0 }
    override val dailyCompletedOn: Flow<Long?> = preferences.map { it[Keys.DAILY_DONE] }
    override val unlockedAchievementIds: Flow<Set<String>> =
        preferences.map { it[Keys.ACHIEVEMENTS] ?: emptySet() }

    // ── Writes ───────────────────────────────────────────────

    override suspend fun setLanguage(language: Language) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    override suspend fun setMode(mode: GameMode) {
        context.dataStore.edit { it[Keys.MODE] = mode.id }
    }

    override suspend fun setLayout(layout: KeyboardLayout?) {
        context.dataStore.edit { it[Keys.LAYOUT] = layout?.id ?: AUTO_LAYOUT_ID }
    }

    override suspend fun setShowKeyboard(visible: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_KEYBOARD] = visible }
    }

    override suspend fun setRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REMINDERS] = enabled }
    }

    override suspend fun addXp(amount: Int) {
        context.dataStore.edit { it[Keys.XP] = (it[Keys.XP] ?: 0) + amount }
    }

    /**
     * Streak rules: same day → unchanged; consecutive day → +1; gap → reset to 1.
     * The returned pair is read from the preferences [edit] actually persisted,
     * so it can never disagree with storage.
     */
    override suspend fun recordDailyCompleted(todayEpochDay: Long): Pair<Int, Int> {
        val prefs = context.dataStore.edit { p ->
            val current = when (p[Keys.DAILY_DONE]) {
                todayEpochDay -> (p[Keys.STREAK] ?: 1)          // already done today
                todayEpochDay - 1 -> (p[Keys.STREAK] ?: 0) + 1  // consecutive day
                else -> 1
            }
            p[Keys.STREAK] = current
            p[Keys.BEST_STREAK] = maxOf(p[Keys.BEST_STREAK] ?: 0, current)
            p[Keys.DAILY_DONE] = todayEpochDay
        }
        return (prefs[Keys.STREAK] ?: 1) to (prefs[Keys.BEST_STREAK] ?: 1)
    }

    override suspend fun unlockAchievements(ids: Set<String>) {
        if (ids.isEmpty()) return
        context.dataStore.edit {
            it[Keys.ACHIEVEMENTS] = (it[Keys.ACHIEVEMENTS] ?: emptySet()) + ids
        }
    }
}
