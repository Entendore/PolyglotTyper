package com.example.polyglottyper.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.polyglottyper.R
import com.example.polyglottyper.domain.model.Achievement
import com.example.polyglottyper.domain.model.KeyboardLayout
import com.example.polyglottyper.domain.model.Language
import com.example.polyglottyper.domain.model.LanguageStats
import com.example.polyglottyper.domain.model.LayoutStats
import com.example.polyglottyper.domain.model.TypingResult
import com.example.polyglottyper.ui.common.Sparkline
import com.example.polyglottyper.ui.common.achievementInfo
import com.example.polyglottyper.ui.common.layoutDescription
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit, viewModel: StatsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isEmpty) {
            Column(
                modifier = Modifier.padding(padding).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    stringResource(R.string.no_results),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (uiState.wpmHistory.size >= 2) {
                    item(key = "header_history") {
                        SectionTitle(stringResource(R.string.wpm_history), topPadding = 0.dp)
                    }
                    item(key = "sparkline") {
                        WpmHistoryCard(points = uiState.wpmHistory.map { it.wpm })
                    }
                }

                if (uiState.layoutStats.isNotEmpty()) {
                    item(key = "header_layout") { SectionTitle(stringResource(R.string.by_layout)) }
                    items(uiState.layoutStats, key = { "layout_${it.layoutId}" }) { stat ->
                        LayoutStatCard(stat)
                    }
                }

                if (uiState.languageStats.isNotEmpty()) {
                    item(key = "header_language") { SectionTitle(stringResource(R.string.per_language)) }
                    items(uiState.languageStats, key = { "lang_${it.languageCode}" }) { stat ->
                        LanguageStatCard(stat)
                    }
                }

                item(key = "header_ach") { SectionTitle(stringResource(R.string.achievements)) }
                item(key = "ach_grid") { AchievementGrid(uiState.unlocked) }

                if (uiState.recentResults.isNotEmpty()) {
                    item(key = "header_recent") { SectionTitle(stringResource(R.string.recent_games)) }
                    itemsIndexed(
                        uiState.recentResults,
                        key = { _, result -> "result_${result.id}" }, // stable DB id — no time-collision crashes
                    ) { _, result ->
                        RecentResultRow(result)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, topPadding: androidx.compose.ui.unit.Dp = 8.dp) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = topPadding, bottom = 2.dp),
    )
}

@Composable
private fun WpmHistoryCard(points: List<Int>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Sparkline(
                values = points.map { it.toFloat() },
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().height(72.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${points.first()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.wpm_history_peak, points.max()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "${points.last()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LayoutStatCard(stat: LayoutStats) {
    val layout = KeyboardLayout.fromId(stat.layoutId)
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Keyboard, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(layout.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    layoutDescription(layout),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${stat.bestWpm}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.games_played, stat.gamesPlayed),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LanguageStatCard(stat: LanguageStats) {
    val language = Language.fromCode(stat.languageCode)
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(language.flagEmoji, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(language.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.avg_accuracy, stat.avgAccuracy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${stat.bestWpm}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.games_played, stat.gamesPlayed),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RecentResultRow(result: TypingResult) {
    val date = remember(result.timestamp) {
        DateTimeFormatter.ofPattern("MMM d · HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(result.timestamp))
    }
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(result.language.flagEmoji, fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(result.mode.label, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${result.layout.displayName} · $date",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${result.wpm} WPM",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Text("${result.accuracy}%")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AchievementGrid(unlocked: Set<Achievement>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Achievement.entries.forEach { achievement ->
            val info = achievementInfo(achievement)
            val isUnlocked = achievement in unlocked
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isUnlocked) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.width(150.dp).padding(10.dp)) {
                    Text(info.emoji, fontSize = 22.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(info.titleRes),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (isUnlocked) stringResource(info.descRes)
                        else stringResource(R.string.ach_locked),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
