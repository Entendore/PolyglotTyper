package com.example.polyglottyper.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.polyglottyper.domain.model.Achievement
import com.example.polyglottyper.domain.model.LanguageStats
import com.example.polyglottyper.domain.model.LayoutStats
import com.example.polyglottyper.domain.model.TypingResult
import com.example.polyglottyper.domain.model.WpmPoint
import com.example.polyglottyper.domain.repository.AchievementsRepository
import com.example.polyglottyper.domain.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatsUiState(
    val layoutStats: List<LayoutStats> = emptyList(),
    val languageStats: List<LanguageStats> = emptyList(),
    val recentResults: List<TypingResult> = emptyList(),
    val unlocked: Set<Achievement> = emptySet(),
    val wpmHistory: List<WpmPoint> = emptyList(),   // chronological
) {
    val isEmpty: Boolean
        get() = layoutStats.isEmpty() && languageStats.isEmpty() &&
            recentResults.isEmpty() && wpmHistory.size < 2
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    statsRepository: StatsRepository,
    achievementsRepository: AchievementsRepository,
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        statsRepository.observeRecentResults(),
        statsRepository.observeLanguageStats(),
        statsRepository.observeLayoutStats(),
        achievementsRepository.unlocked,
        statsRepository.observeWpmHistory(),
    ) { recent, perLanguage, perLayout, unlocked, history ->
        StatsUiState(
            layoutStats = perLayout.sortedByDescending { it.bestWpm },
            languageStats = perLanguage.sortedByDescending { it.bestWpm },
            recentResults = recent,
            unlocked = unlocked,
            wpmHistory = history.reversed(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState(),
    )
}
