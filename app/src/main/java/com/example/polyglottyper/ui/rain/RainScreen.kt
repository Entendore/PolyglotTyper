package com.example.polyglottyper.ui.rain

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import com.example.polyglottyper.domain.model.RewardInfo
import com.example.polyglottyper.domain.model.TypingResult
import com.example.polyglottyper.ui.game.LayoutKeyboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RainScreen(
    onExit: () -> Unit,
    viewModel: RainViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val systemKeyboard = LocalSoftwareKeyboardController.current
    val haptics = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Freeze physics while the app is in the background.
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

    LaunchedEffect(uiState.phase) {
        if (uiState.phase == RainPhase.RUNNING) {
            focusRequester.requestFocus()
            systemKeyboard?.hide()
        }
    }

    // Game loop: one physics tick per animation frame.
    LaunchedEffect(uiState.phase, uiState.isPaused) {
        if (uiState.phase == RainPhase.RUNNING && !uiState.isPaused) {
            var lastFrame = withFrameNanos { it }
            while (true) {
                withFrameNanos { now ->
                    val dt = ((now - lastFrame) / 1e6).coerceAtMost(100.0)
                    lastFrame = now
                    viewModel.tick(dt)
                }
            }
        }
    }

    // Buzz when a word hits the ground (life lost). Baseline starts from the
    // current count so re-entering the screen doesn't fire a spurious buzz.
    var lastLanded by remember { mutableIntStateOf(uiState.landedWords) }
    LaunchedEffect(uiState.landedWords) {
        if (uiState.landedWords > lastLanded) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        lastLanded = uiState.landedWords
    }

    Scaffold(
        topBar = {
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(uiState.language.flagEmoji)
                        Text(
                            stringResource(R.string.rain_level, uiState.level),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                },
                actions = {
                    LiveStat("❤️".repeat(uiState.lives) + "🖤".repeat(3 - uiState.lives))
                    LiveStat("⭐ ${uiState.score}")
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize().imePadding()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds(),
                ) {
                    uiState.falling.forEach { word ->
                        val isTarget = uiState.typedText.isNotEmpty() &&
                            word.text.startsWith(uiState.typedText)
                        Text(
                            text = word.text,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isTarget) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = word.x * maxWidth, y = word.y * maxHeight),
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.typedText,
                    onValueChange = viewModel::onTypedTextChange,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    enabled = uiState.canType(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 20.sp),
                    placeholder = { Text(stringResource(R.string.type_here)) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrect = false,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = viewModel::submitTyped),
                )

                // Any visible word may be targeted → no single "next key" highlight.
                LayoutKeyboard(
                    layout = uiState.layout,
                    language = uiState.language,
                    nextChar = null,
                    onChar = viewModel::onCharTyped,
                    onBackspace = viewModel::onBackspace,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
            }

            if (uiState.phase == RainPhase.READY) {
                RainOverlay {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("☔", fontSize = 44.sp)
                        Text(
                            stringResource(R.string.arcade_title),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            stringResource(R.string.arcade_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(viewModel::startGame) { Text(stringResource(R.string.tap_to_start)) }
                    }
                }
            }
            if (uiState.phase == RainPhase.FINISHED) {
                uiState.finishedResult?.let { result ->
                    RainOverOverlay(result, uiState.reward, viewModel::playAgain, onExit)
                }
            }
        }
    }
}

@Composable
private fun LiveStat(value: String) {
    Text(
        value,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 6.dp),
    )
}

@Composable
private fun RainOverOverlay(
    result: TypingResult,
    reward: RewardInfo?,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit,
) {
    RainOverlay {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.rain_game_over),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                "${result.correctWords * 10}",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(stringResource(R.string.rain_score), style = MaterialTheme.typography.labelMedium)
            reward?.let {
                Text(
                    stringResource(R.string.reward_xp, it.xpEarned),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ResultStat(stringResource(R.string.wpm_label), "${result.wpm}")
                ResultStat(stringResource(R.string.accuracy_label), "${result.accuracy}%")
                ResultStat(stringResource(R.string.rain_words), "${result.correctWords}")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onHome) { Text(stringResource(R.string.go_home)) }
                Button(onClick = onPlayAgain) { Text(stringResource(R.string.play_again)) }
            }
        }
    }
}

@Composable
private fun RainOverlay(content: @Composable () -> Unit) {
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
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
