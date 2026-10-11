package com.example.polyglottyper.ui.game

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.polyglottyper.domain.model.GameMode
import com.example.polyglottyper.domain.model.KeyboardLayout
import com.example.polyglottyper.domain.model.Language
import com.example.polyglottyper.domain.model.RewardInfo
import com.example.polyglottyper.domain.model.TypingResult
import com.example.polyglottyper.domain.repository.UserPreferencesRepository
import com.example.polyglottyper.domain.repository.WordRepository
import com.example.polyglottyper.domain.usecase.RecordGameResult
import com.example.polyglottyper.util.TypingMath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject

enum class GamePhase { READY, RUNNING, FINISHED }

/** Minimal snapshot that fully reconstructs an in-progress game after process death. */
@Serializable
data class SavedGameState(
    val words: List<String>,
    val wordResults: List<Boolean>,
    val currentWordIndex: Int,
    val elapsedMs: Long,
    val wasPaused: Boolean,
)

data class GameUiState(
    val phase: GamePhase = GamePhase.READY,
    val isPaused: Boolean = false,
    val language: Language = Language.ENGLISH,
    val mode: GameMode = GameMode.Timed(60),
    val layout: KeyboardLayout = KeyboardLayout.QWERTY,
    val keyboardVisible: Boolean = true,
    val words: List<String> = emptyList(),
    val wordResults: List<Boolean> = emptyList(),
    val currentWordIndex: Int = 0,
    val typedText: String = "",
    val remainingSeconds: Int = 60,
    val elapsedSeconds: Int = 0,
    val liveWpm: Int = 0,
    val liveAccuracy: Int = 100,
    val correctWords: Int = 0,
    val incorrectWords: Int = 0,
    val finishedResult: TypingResult? = null,
    val reward: RewardInfo? = null,
) {
    /** 0..1 for the progress bar, or null for indeterminate (Zen). */
    val progress: Float?
        get() = when (val m = mode) {
            is GameMode.Timed -> remainingSeconds / m.seconds.toFloat()
            is GameMode.SuddenDeath -> remainingSeconds / m.seconds.toFloat()
            is GameMode.WordCount -> currentWordIndex / m.words.toFloat()
            is GameMode.DailyChallenge ->
                currentWordIndex / GameMode.DailyChallenge.WORD_COUNT.toFloat()
            GameMode.Zen -> null
            GameMode.WordRain -> null
        }
}

@HiltViewModel
class GameViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val wordRepository: WordRepository,
    private val recordGameResult: RecordGameResult,
    private val userPreferences: UserPreferencesRepository,
) : ViewModel() {

    private val language = Language.fromCode(checkNotNull(savedStateHandle["languageCode"]))
    private val mode = GameMode.fromId(savedStateHandle["modeId"] ?: GameMode.Timed(60).id)
    private val layout = savedStateHandle.get<String>("layoutId")
        ?.let(KeyboardLayout::fromId)
        ?: KeyboardLayout.recommendedFor(language)

    /** Captured once so a game crossing midnight stays self-consistent. */
    private val dailyEpochDay: Long = LocalDate.now().toEpochDay()

    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow(
        GameUiState(language = language, mode = mode, layout = layout),
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    // Split clock: elapsed = accumulated + (now - segmentStart). Pausing banks
    // the current segment — background time never counts and there is no drift.
    private var accumulatedMs = 0L
    private var segmentStartMs = 0L

    init {
        viewModelScope.launch {
            userPreferences.showKeyboard.collect { visible ->
                _uiState.update { it.copy(keyboardVisible = visible) }
            }
        }
        // Read the snapshot BEFORE prepareNewGame clears the handle.
        val saved = restoreSavedState()
        prepareNewGame()
        if (saved != null) applySavedState(saved)
    }

    // ── Process-death restoration ────────────────────────────

    private fun restoreSavedState(): SavedGameState? =
        savedStateHandle.get<String>(SAVED_STATE_KEY)
            ?.let { raw -> runCatching { json.decodeFromString<SavedGameState>(raw) }.getOrNull() }

    private fun applySavedState(saved: SavedGameState) {
        accumulatedMs = saved.elapsedMs
        segmentStartMs = SystemClock.elapsedRealtime()
        _uiState.update {
            it.copy(
                phase = GamePhase.RUNNING,
                isPaused = saved.wasPaused,
                words = saved.words,
                wordResults = saved.wordResults,
                currentWordIndex = saved.currentWordIndex,
                elapsedSeconds = (saved.elapsedMs / 1_000L).toInt(),
                remainingSeconds = remainingAfter(saved.elapsedMs),
                typedText = "",
            )
        }
        if (!saved.wasPaused) startTicker()
        // If it was paused, the PausedOverlay shows and Resume restarts the ticker.
    }

    private fun remainingAfter(elapsedMs: Long): Int {
        val limit = durationSeconds()
        if (limit <= 0) return 0
        return (limit - elapsedMs / 1_000L).coerceIn(0, limit)
    }

    private fun persistState() {
        val state = _uiState.value
        if (state.phase != GamePhase.RUNNING) return
        val saved = SavedGameState(
            words = state.words,
            wordResults = state.wordResults,
            currentWordIndex = state.currentWordIndex,
            elapsedMs = elapsedMs(),
            wasPaused = state.isPaused,
        )
        savedStateHandle[SAVED_STATE_KEY] = json.encodeToString(saved)
    }

    private fun clearSavedState() {
        savedStateHandle.remove<String>(SAVED_STATE_KEY)
    }

    // ── Game lifecycle ───────────────────────────────────────

    fun prepareNewGame() {
        tickerJob?.cancel()
        clearSavedState()
        _uiState.value = GameUiState(
            language = language,
            mode = mode,
            layout = layout,
            keyboardVisible = _uiState.value.keyboardVisible, // survives "play again"
            words = newWords(),
            remainingSeconds = durationSeconds(),
        )
    }

    /** Daily uses a fixed seed → identical words for everyone on the same day. */
    private fun newWords(): List<String> = when (mode) {
        is GameMode.DailyChallenge -> wordRepository.generateWords(
            language,
            GameMode.DailyChallenge.WORD_COUNT,
            seed = dailyEpochDay,
        )
        else -> wordRepository.generateWords(language, MAX_TIMED_WORDS)
    }

    private fun durationSeconds(): Int = when (val m = mode) {
        is GameMode.Timed -> m.seconds
        is GameMode.SuddenDeath -> m.seconds
        else -> 0
    }

    fun startGame() {
        if (_uiState.value.phase != GamePhase.READY) return
        resetClock()
        _uiState.update { it.copy(phase = GamePhase.RUNNING) }
        persistState()
        startTicker()
    }

    /** Called when the app leaves the foreground. The clock freezes. */
    fun pause() {
        if (_uiState.value.phase != GamePhase.RUNNING || _uiState.value.isPaused) return
        tickerJob?.cancel()
        accumulatedMs += SystemClock.elapsedRealtime() - segmentStartMs
        _uiState.update { it.copy(isPaused = true) }
        persistState()
    }

    fun resume() {
        if (_uiState.value.phase != GamePhase.RUNNING || !_uiState.value.isPaused) return
        segmentStartMs = SystemClock.elapsedRealtime()
        _uiState.update { it.copy(isPaused = false) }
        persistState()
        startTicker()
    }

    /** Zen only: end the session and bank the result. */
    fun finishEarly() {
        if (mode == GameMode.Zen && _uiState.value.phase == GamePhase.RUNNING) finishGame()
    }

    fun playAgain() {
        prepareNewGame()
        startGame()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(TICK_MS)
                tick()
            }
        }
    }

    private fun resetClock() {
        accumulatedMs = 0L
        segmentStartMs = SystemClock.elapsedRealtime()
    }

    private fun elapsedMs(): Long = when {
        segmentStartMs == 0L || _uiState.value.isPaused -> accumulatedMs
        else -> accumulatedMs + (SystemClock.elapsedRealtime() - segmentStartMs)
    }

    private fun canType(): Boolean =
        _uiState.value.phase == GamePhase.RUNNING && !_uiState.value.isPaused

    // ── Input ────────────────────────────────────────────────

    fun onTypedTextChange(text: String) {
        if (!canType()) return
        if (text.endsWith(' ')) {
            val submitted = text.trim()
            if (submitted.isNotEmpty()) submitWord(submitted)
            _uiState.update { it.copy(typedText = "") }
        } else {
            _uiState.update { it.copy(typedText = text) }
        }
    }

    /** IME "Done" action: submit the current word even without a trailing space. */
    fun submitCurrentWord() {
        if (!canType()) return
        val current = _uiState.value.typedText.trim()
        if (current.isNotEmpty()) {
            submitWord(current)
            _uiState.update { it.copy(typedText = "") }
        }
    }

    fun onCharTyped(char: Char) = onTypedTextChange(_uiState.value.typedText + char)

    fun onBackspace() {
        if (!canType()) return
        _uiState.update { it.copy(typedText = it.typedText.dropLast(1)) }
    }

    fun onToggleKeyboard() {
        viewModelScope.launch { userPreferences.setShowKeyboard(!_uiState.value.keyboardVisible) }
    }

    // ── Scoring ──────────────────────────────────────────────

    private fun submitWord(typed: String) {
        var suddenDeathFailure = false
        _uiState.update { state ->
            val target = state.words.getOrNull(state.currentWordIndex) ?: return@update state
            val correct = typed == target
            if (mode is GameMode.SuddenDeath && !correct) suddenDeathFailure = true
            state.copy(
                wordResults = state.wordResults + correct,
                currentWordIndex = state.currentWordIndex + 1,
                correctWords = state.correctWords + if (correct) 1 else 0,
                incorrectWords = state.incorrectWords + if (correct) 0 else 1,
            )
        }
        if (suddenDeathFailure) {
            finishGame()
            return
        }
        maybeExtendZenWords()
        persistState()
        refreshLiveStats()
        if (hasReachedTarget()) finishGame()
    }

    /** Zen is endless: top up the word list before it runs dry. */
    private fun maybeExtendZenWords() {
        if (mode != GameMode.Zen) return
        val state = _uiState.value
        if (state.words.size - state.currentWordIndex < ZEN_EXTEND_THRESHOLD) {
            _uiState.update {
                it.copy(words = it.words + wordRepository.generateWords(language, ZEN_BATCH))
            }
        }
    }

    private fun hasReachedTarget(): Boolean {
        val index = _uiState.value.currentWordIndex
        return when (mode) {
            is GameMode.WordCount -> index >= mode.words
            is GameMode.DailyChallenge -> index >= GameMode.DailyChallenge.WORD_COUNT
            GameMode.Zen -> false
            else -> index >= _uiState.value.words.size   // timed-mode exhaustion guard
        }
    }

    private fun tick() {
        val elapsed = elapsedMs()
        val newSecond = (elapsed / 1_000L).toInt()
        val secondChanged = newSecond != _uiState.value.elapsedSeconds
        _uiState.update { it.copy(elapsedSeconds = newSecond) }

        val limit = durationSeconds()
        if (limit > 0) {
            val remaining = limit - newSecond
            if (remaining <= 0) {
                finishGame()
                return
            }
            _uiState.update { it.copy(remainingSeconds = remaining) }
        }
        refreshLiveStats()
        if (secondChanged) persistState() // once per second, not 10×/s
    }

    private fun refreshLiveStats() {
        _uiState.update { state ->
            state.copy(
                liveWpm = TypingMath.wpm(
                    correctChars = TypingMath.liveCorrectChars(
                        state.words, state.wordResults, state.typedText,
                    ),
                    elapsedMs = elapsedMs().coerceAtLeast(MIN_ELAPSED_MS),
                ),
                liveAccuracy = TypingMath.accuracy(state.correctWords, state.incorrectWords),
            )
        }
    }

    private fun finishGame() {
        if (_uiState.value.phase == GamePhase.FINISHED) return
        tickerJob?.cancel()
        clearSavedState() // finished games must not resurrect
        refreshLiveStats()

        val state = _uiState.value
        val result = TypingResult(
            language = language,
            layout = layout,
            mode = mode,
            wpm = state.liveWpm,
            accuracy = state.liveAccuracy,
            correctWords = state.correctWords,
            incorrectWords = state.incorrectWords,
            durationSeconds = (elapsedMs() / 1_000L).toInt().coerceAtLeast(1),
            timestamp = System.currentTimeMillis(),
        )
        _uiState.update { it.copy(phase = GamePhase.FINISHED, finishedResult = result) }
        // Rewards arrive async — the overlay shows instantly, the celebration pops in.
        viewModelScope.launch {
            _uiState.update { s -> s.copy(reward = recordGameResult(result, dailyEpochDay)) }
        }
    }

    override fun onCleared() {
        tickerJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val SAVED_STATE_KEY = "saved_game"
        const val TICK_MS = 100L
        const val MAX_TIMED_WORDS = 300
        const val MIN_ELAPSED_MS = 1_000L
        const val ZEN_EXTEND_THRESHOLD = 15
        const val ZEN_BATCH = 50
    }
}
