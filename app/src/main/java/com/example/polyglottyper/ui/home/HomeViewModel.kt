package com.example.polyglottyper.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.polyglottyper.domain.model.GameMode
import com.example.polyglottyper.domain.model.KeyboardLayout
import com.example.polyglottyper.domain.model.Language
import com.example.polyglottyper.domain.repository.StatsRepository
import com.example.polyglottyper.domain.repository.UserPreferencesRepository
import com.example.polyglottyper.util.LevelMath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val selectedLanguage: Language = Language.ENGLISH,
    val selectedMode: GameMode = GameMode.Timed(60),
    /** Null = Automatic. */
    val selectedLayout: KeyboardLayout? = null,
    val resolvedLayout: KeyboardLayout = KeyboardLayout.QWERTY,
    val bestWpmByLanguage: Map<String, Int> = emptyMap(),
    val level: Int = 1,
    val totalXp: Int = 0,
    val xpToNextLevel: Int = LevelMath.xpToNextLevel(0),
    val levelProgress: Float = 0f,
    val currentStreak: Int = 0,
    val dailyCompletedToday: Boolean = false,
    val streakAtRisk: Boolean = false,
    val remindersEnabled: Boolean = true,
)

/** Bundles the player-related prefs so the outer combine stays within 5 flows. */
private data class PlayerInfo(
    val xp: Int,
    val streak: Int,
    val dailyDone: Long?,
    val reminders: Boolean,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userPreferences: UserPreferencesRepository,
    statsRepository: StatsRepository,
) : ViewModel() {

    private val playerFlow = combine(
        userPreferences.totalXp,
        userPreferences.currentStreak,
        userPreferences.dailyCompletedOn,
        userPreferences.remindersEnabled,
    ) { xp, streak, dailyDone, reminders -> PlayerInfo(xp, streak, dailyDone, reminders) }

    val uiState: StateFlow<HomeUiState> = combine(
        userPreferences.preferredLanguage,
        userPreferences.preferredMode,
        userPreferences.preferredLayout,
        statsRepository.observeLanguageStats(),
        playerFlow,
    ) { language, mode, layout, stats, info ->
        val today = LocalDate.now().toEpochDay()
        // A stored streak only advances on completion — display it only while
        // it is still alive (done today or yesterday), otherwise it has decayed.
        val streakAlive = info.dailyDone == today || info.dailyDone == today - 1
        HomeUiState(
            selectedLanguage = language,
            selectedMode = mode,
            selectedLayout = layout,
            resolvedLayout = layout ?: KeyboardLayout.recommendedFor(language),
            bestWpmByLanguage = stats.associate { it.languageCode to it.bestWpm },
            level = LevelMath.levelFor(info.xp),
            totalXp = info.xp,
            xpToNextLevel = LevelMath.xpToNextLevel(info.xp),
            levelProgress = LevelMath.progressToNextLevel(info.xp),
            currentStreak = if (streakAlive) info.streak else 0,
            dailyCompletedToday = info.dailyDone == today,
            streakAtRisk = streakAlive && info.dailyDone != today,
            remindersEnabled = info.reminders,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun onLanguageSelected(language: Language) =
        viewModelScope.launch { userPreferences.setLanguage(language) }

    fun onModeSelected(mode: GameMode) =
        viewModelScope.launch { userPreferences.setMode(mode) }

    fun onLayoutSelected(layout: KeyboardLayout?) =
        viewModelScope.launch { userPreferences.setLayout(layout) }

    fun onRemindersToggled(enabled: Boolean) =
        viewModelScope.launch { userPreferences.setRemindersEnabled(enabled) }
}
