package com.example.polyglottyper.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.polyglottyper.R
import com.example.polyglottyper.domain.model.GameMode
import com.example.polyglottyper.domain.model.KeyboardLayout
import com.example.polyglottyper.domain.model.Language

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartGame: (Language, GameMode, KeyboardLayout) -> Unit,
    onPlayDaily: (Language, KeyboardLayout) -> Unit,
    onStartRain: (Language, KeyboardLayout) -> Unit,
    onOpenStats: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenStats) {
                        Icon(
                            Icons.Filled.BarChart,
                            contentDescription = stringResource(R.string.stats_title),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PlayerHeader(
                level = uiState.level,
                levelProgress = uiState.levelProgress,
                xpToNextLevel = uiState.xpToNextLevel,
                currentStreak = uiState.currentStreak,
            )

            if (uiState.streakAtRisk) {
                StreakAtRiskBanner()
            }

            DailyChallengeCard(
                completedToday = uiState.dailyCompletedToday,
                onClick = { onPlayDaily(uiState.selectedLanguage, uiState.resolvedLayout) },
            )

            WordRainCard(
                onClick = { onStartRain(uiState.selectedLanguage, uiState.resolvedLayout) },
            )

            Text(stringResource(R.string.select_language), style = MaterialTheme.typography.titleMedium)
            LanguageGrid(
                selected = uiState.selectedLanguage,
                bestWpmByLanguage = uiState.bestWpmByLanguage,
                onSelected = viewModel::onLanguageSelected,
            )

            Text(stringResource(R.string.select_mode), style = MaterialTheme.typography.titleMedium)
            // GameMode.ALL automatically includes Sudden Death and Zen.
            ModeSelector(selected = uiState.selectedMode, onSelected = viewModel::onModeSelected)

            Text(stringResource(R.string.select_layout), style = MaterialTheme.typography.titleMedium)
            LayoutPickerCard(
                selected = uiState.selectedLayout,
                resolved = uiState.resolvedLayout,
                onSelected = viewModel::onLayoutSelected,
            )

            Button(
                onClick = {
                    onStartGame(uiState.selectedLanguage, uiState.selectedMode, uiState.resolvedLayout)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(stringResource(R.string.start_typing), style = MaterialTheme.typography.titleMedium)
            }

            RemindersToggle(
                enabled = uiState.remindersEnabled,
                onToggled = viewModel::onRemindersToggled,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Keyboard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.keyboard_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PlayerHeader(
    level: Int,
    levelProgress: Float,
    xpToNextLevel: Int,
    currentStreak: Int,
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.level_label, level),
                    style = MaterialTheme.typography.titleMedium,
                )
                LinearProgressIndicator(
                    progress = { levelProgress },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
                Text(
                    stringResource(R.string.xp_to_next, xpToNextLevel, level + 1),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔥 $currentStreak", style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.streak_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StreakAtRiskBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Text(
            stringResource(R.string.streak_at_risk),
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyChallengeCard(completedToday: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("📅", fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.daily_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.daily_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (completedToday) {
                Text(
                    stringResource(R.string.daily_done),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordRainCard(onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("☔", fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.arcade_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.arcade_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LanguageGrid(
    selected: Language,
    bestWpmByLanguage: Map<String, Int>,
    onSelected: (Language) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Language.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { language ->
                    LanguageCard(
                        language = language,
                        selected = language == selected,
                        bestWpm = bestWpmByLanguage[language.code],
                        onClick = { onSelected(language) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageCard(
    language: Language,
    selected: Boolean,
    bestWpm: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(language.flagEmoji, fontSize = 26.sp)
                Spacer(Modifier.width(8.dp))
                Text(language.displayName, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = bestWpm?.let { stringResource(R.string.best_wpm, it) }
                    ?: stringResource(R.string.no_games_yet),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModeSelector(selected: GameMode, onSelected: (GameMode) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GameMode.ALL.forEach { mode ->
            FilterChip(
                selected = mode == selected,
                onClick = { onSelected(mode) },
                label = { Text(mode.label) },
            )
        }
    }
}

@Composable
private fun RemindersToggle(enabled: Boolean, onToggled: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.daily_reminders), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.daily_reminders_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onToggled)
    }
}
