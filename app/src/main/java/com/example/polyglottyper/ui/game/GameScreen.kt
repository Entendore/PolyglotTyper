package com.example.polyglottyper.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.polyglottyper.R
import com.example.polyglottyper.domain.model.GameMode
import com.example.polyglottyper.domain.model.KeyboardLayout
import com.example.polyglottyper.domain.model.Language
import com.example.polyglottyper.domain.model.RewardInfo
import com.example.polyglottyper.domain.model.TypingResult
import com.example.polyglottyper.ui.common.achievementInfo
import com.example.polyglottyper.ui.theme.CorrectGreen
import com.example.polyglottyper.util.TypingMath

private const val WORDS_BEFORE = 4
private const val WORDS_AFTER = 24

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun GameScreen(
    onExit: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val systemKeyboard = LocalSoftwareKeyboardController.current
    val haptics = LocalHapticFeedback.current

    // Freeze the clock while the app is in the background.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.pause()
                Lifecycle.Event.ON_RESUME -> viewModel.resume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.phase, uiState.isPaused, uiState.keyboardVisible) {
        when (uiState.phase) {
            GamePhase.RUNNING -> if (!uiState.isPaused) {
                focusRequester.requestFocus()
                // The on-screen practice keyboard replaces the system IME.
                if (uiState.keyboardVisible) systemKeyboard?.hide()
            }
            GamePhase.FINISHED -> focusManager.clearFocus()
            else -> Unit
        }
    }

    // Buzz when a wrong word is submitted. Baseline starts from the current
    // count so a process-death restoration doesn't fire a spurious buzz.
    var lastIncorrectCount by remember { mutableIntStateOf(uiState.incorrectWords) }
    LaunchedEffect(uiState.incorrectWords) {
        if (uiState.incorrectWords > lastIncorrectCount) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        lastIncorrectCount = uiState.incorrectWords
    }

    // Next character the player should type; drives the key highlight.
    val nextChar = TypingMath.nextExpectedChar(
        words = uiState.words,
        wordIndex = uiState.currentWordIndex,
        typed = uiState.typedText,
    )

    Scaffold(
        topBar = {
            GameTopBar(
                uiState = uiState,
                onFinish = viewModel::finishEarly,
                onToggleKeyboard = viewModel::onToggleKeyboard,
                onExit = onExit,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding(),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val progress = uiState.progress
                if (progress == null) {
                    LinearProgressIndicator(Modifier.fillMaxWidth()) // Zen: indeterminate
                } else {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                WordsDisplay(
                    words = uiState.words,
                    results = uiState.wordResults,
                    currentIndex = uiState.currentWordIndex,
                    typedText = uiState.typedText,
                    modifier = Modifier.weight(1f),
                )

                OutlinedTextField(
                    value = uiState.typedText,
                    onValueChange = viewModel::onTypedTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { state ->
                            if (state.isFocused && uiState.keyboardVisible) systemKeyboard?.hide()
                        },
                    enabled = uiState.phase == GamePhase.RUNNING && !uiState.isPaused,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 20.sp),
                    placeholder = { Text(stringResource(R.string.type_here)) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrect = false,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = viewModel::submitCurrentWord),
                )

                AnimatedVisibility(visible = uiState.keyboardVisible) {
                    LayoutKeyboard(
                        layout = uiState.layout,
                        language = uiState.language,
                        nextChar = nextChar,
                        onChar = viewModel::onCharTyped,
                        onBackspace = viewModel::onBackspace,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(4.dp))
            }

            if (uiState.phase == GamePhase.READY) {
                StartOverlay(
                    language = uiState.language,
                    mode = uiState.mode,
                    layout = uiState.layout,
                    onStart = viewModel::startGame,
                )
            }
            if (uiState.phase == GamePhase.FINISHED) {
                uiState.finishedResult?.let { result ->
                    ResultsOverlay(
                        result = result,
                        reward = uiState.reward,
                        onPlayAgain = viewModel::playAgain,
                        onHome = onExit,
                    )
                }
            }
            if (uiState.phase == GamePhase.RUNNING && uiState.isPaused) {
                PausedOverlay(onResume = viewModel::resume)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameTopBar(
    uiState: GameUiState,
    onFinish: () -> Unit,
    onToggleKeyboard: () -> Unit,
    onExit: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onExit) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.exit_game),
                )
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(uiState.language.flagEmoji)
                if (uiState.mode is GameMode.SuddenDeath) Text("⚡")
                when (val mode = uiState.mode) {
                    is GameMode.Timed, is GameMode.SuddenDeath ->
                        Text("${uiState.remainingSeconds}s", style = MaterialTheme.typography.titleLarge)
                    is GameMode.WordCount ->
                        Text(
                            stringResource(
                                R.string.words_progress,
                                uiState.currentWordIndex.coerceAtMost(mode.words),
                                mode.words,
                            ),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    is GameMode.DailyChallenge ->
                        Text(
                            stringResource(
                                R.string.words_progress,
                                uiState.currentWordIndex.coerceAtMost(GameMode.DailyChallenge.WORD_COUNT),
                                GameMode.DailyChallenge.WORD_COUNT,
                            ),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    GameMode.Zen ->
                        Text("${uiState.elapsedSeconds}s", style = MaterialTheme.typography.titleLarge)
                    GameMode.WordRain -> Unit
                }
            }
        },
        actions = {
            LiveStat(value = "${uiState.liveWpm}", label = stringResource(R.string.wpm_label))
            LiveStat(value = "${uiState.liveAccuracy}%", label = stringResource(R.string.accuracy_label))
            if (uiState.mode == GameMode.Zen && uiState.phase == GamePhase.RUNNING) {
                TextButton(onClick = onFinish) { Text(stringResource(R.string.finish)) }
            }
            IconButton(onClick = onToggleKeyboard) {
                Icon(
                    imageVector = if (uiState.keyboardVisible) Icons.Filled.KeyboardHide
                    else Icons.Filled.Keyboard,
                    contentDescription = stringResource(R.string.toggle_keyboard),
                )
            }
        },
    )
}

@Composable
private fun LiveStat(value: String, label: String) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordsDisplay(
    words: List<String>,
    results: List<Boolean>,
    currentIndex: Int,
    typedText: String,
    modifier: Modifier = Modifier,
) {
    val start = (currentIndex - WORDS_BEFORE).coerceAtLeast(0)
    val end = (currentIndex + WORDS_AFTER).coerceAtMost(words.size)

    Box(modifier = modifier.fillMaxWidth().clipToBounds()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            words.subList(start, end).forEachIndexed { i, word ->
                val index = start + i
                when {
                    index < currentIndex -> {
                        val correct = results.getOrNull(index) == true
                        Text(
                            word,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (correct) CorrectGreen else MaterialTheme.colorScheme.error,
                        )
                    }
                    index == currentIndex -> CurrentWord(target = word, typed = typedText)
                    else -> Text(
                        word,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

/** Highlights the current word with per-character feedback while typing. */
@Composable
private fun CurrentWord(target: String, typed: String) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        target.forEachIndexed { index, char ->
            val color = when {
                index < typed.length && typed[index] == char -> CorrectGreen
                index < typed.length -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
            Text(char.toString(), style = MaterialTheme.typography.titleLarge, color = color)
        }
        if (typed.length > target.length) {
            typed.substring(target.length).forEach { char ->
                Text(
                    char.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun StartOverlay(
    language: Language,
    mode: GameMode,
    layout: KeyboardLayout,
    onStart: () -> Unit,
) {
    OverlayContainer {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(language.flagEmoji, fontSize = 44.sp)
            Text(language.displayName, style = MaterialTheme.typography.headlineSmall)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Outlined.Keyboard,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    layout.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                mode.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onStart) { Text(stringResource(R.string.tap_to_start)) }
        }
    }
}

@Composable
private fun PausedOverlay(onResume: () -> Unit) {
    OverlayContainer {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.paused), style = MaterialTheme.typography.headlineSmall)
            Button(onClick = onResume) { Text(stringResource(R.string.resume)) }
        }
    }
}

@Composable
private fun ResultsOverlay(
    result: TypingResult,
    reward: RewardInfo?,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit,
) {
    OverlayContainer {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "${result.language.flagEmoji} ${result.language.displayName}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(stringResource(R.string.results_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                "${result.wpm}",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(stringResource(R.string.wpm_label), style = MaterialTheme.typography.labelMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ResultStat(stringResource(R.string.accuracy_label), "${result.accuracy}%")
                ResultStat(stringResource(R.string.correct_words), "${result.correctWords}")
                ResultStat(stringResource(R.string.incorrect_words), "${result.incorrectWords}")
                ResultStat(
                    stringResource(R.string.duration),
                    stringResource(R.string.seconds_format, result.durationSeconds),
                )
            }

            // Reward celebration — appears once the use case finishes computing.
            reward?.let { reward ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(R.string.reward_xp, reward.xpEarned),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    reward.leveledUpTo?.let { level ->
                        Text(
                            stringResource(R.string.level_up, level),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                if (reward.newAchievements.isNotEmpty()) {
                    Text(
                        stringResource(R.string.new_achievements),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        reward.newAchievements.forEach { achievement ->
                            Text(achievementInfo(achievement).emoji, fontSize = 24.sp)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onHome) { Text(stringResource(R.string.go_home)) }
                Button(onClick = onPlayAgain) { Text(stringResource(R.string.play_again)) }
            }
        }
    }
}

@Composable
private fun OverlayContainer(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center,
    ) {
        ElevatedCard(Modifier.padding(24.dp)) {
            Column(Modifier.padding(24.dp)) { content() }
        }
    }
}

@Composable
private fun ResultStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
