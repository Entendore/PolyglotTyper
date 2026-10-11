package com.example.polyglottyper.ui.rain

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.polyglottyper.domain.model.GameMode
import com.example.polyglottyper.domain.model.KeyboardLayout
import com.example.polyglottyper.domain.model.Language
import com.example.polyglottyper.domain.model.RewardInfo
import com.example.polyglottyper.domain.model.TypingResult
import com.example.polyglottyper.domain.repository.WordRepository
import com.example.polyglottyper.domain.usecase.RecordGameResult
import com.example.polyglottyper.util.TypingMath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RainPhase { READY, RUNNING, FINISHED }

data class FallingWord(
    val id: Long,
    val text: String,
    val x: Float,      // 0..1 of width
    val y: Float,      // 0..1 of height
    val speed: Float,  // screen heights per second at level 1
)

data class RainUiState(
    val phase: RainPhase = RainPhase.READY,
    val isPaused: Boolean = false,
    val language: Language = Language.ENGLISH,
    val layout: KeyboardLayout = KeyboardLayout.QWERTY,
    val falling: List<FallingWord> = emptyList(),
    val typedText: String = "",
    val lives: Int = STARTING_LIVES,
    val score: Int = 0,
    val level: Int = 1,
    val elapsedMs: Double = 0.0,
    val liveWpm: Int = 0,
    val liveAccuracy: Int = 100,
    val destroyedWords: Int = 0,
    val destroyedChars: Int = 0,      // public — copy() from the VM needs it
    val landedWords: Int = 0,
    val reward: RewardInfo? = null,
    val finishedResult: TypingResult? = null,
) {
    fun canType(): Boolean = phase == RainPhase.RUNNING && !isPaused
}

@HiltViewModel
class RainViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wordRepository: WordRepository,
    private val recordGameResult: RecordGameResult,
) : ViewModel() {

    private val language = Language.fromCode(checkNotNull(savedStateHandle["languageCode"]))
    private val layout = savedStateHandle.get<String>("layoutId")
        ?.let(KeyboardLayout::fromId)
        ?: KeyboardLayout.recommendedFor(language)

    private val _uiState = MutableStateFlow(RainUiState(language = language, layout = layout))
    val uiState: StateFlow<RainUiState> = _uiState.asStateFlow()

    private var nextId = 0L
    private var spawnCountdownMs = 0.0
    private var random = kotlin.random.Random.Default

    fun startGame() {
        random = kotlin.random.Random(System.currentTimeMillis())
        spawnCountdownMs = 500.0
        _uiState.value = RainUiState(language = language, layout = layout, phase = RainPhase.RUNNING)
    }

    fun pause() {
        if (_uiState.value.phase == RainPhase.RUNNING && !_uiState.value.isPaused) {
            _uiState.update { it.copy(isPaused = true) }
        }
    }

    fun resume() {
        if (_uiState.value.phase == RainPhase.RUNNING && _uiState.value.isPaused) {
            _uiState.update { it.copy(isPaused = false) }
        }
    }

    fun playAgain() = startGame()

    /** Called every animation frame with delta time in ms. */
    fun tick(dtMs: Double) {
        val state = _uiState.value
        if (state.phase != RainPhase.RUNNING || state.isPaused) return

        val dt = dtMs.coerceAtMost(MAX_DT_MS) // clamp long frame gaps (debug pauses)
        val elapsed = state.elapsedMs + dt
        val level = (elapsed / LEVEL_PERIOD_MS).toInt() + 1
        val fallScale = 1.0 + (level - 1) * LEVEL_SPEED_FACTOR
        val dtSeconds = dt / 1_000.0

        var lives = state.lives
        var landed = state.landedWords
        val survivors = ArrayList<FallingWord>(state.falling.size + 1)
        for (word in state.falling) {
            val y = word.y + word.speed * fallScale * dtSeconds
            if (y >= 1f) {
                lives--
                landed++
            } else {
                survivors.add(word.copy(y = y.toFloat()))
            }
        }

        spawnCountdownMs -= dt
        if (spawnCountdownMs <= 0.0) {
            if (survivors.size < MAX_FALLING) survivors.add(spawnWord())
            spawnCountdownMs = spawnIntervalMs(level).toDouble()
        }

        _uiState.update {
            it.copy(
                elapsedMs = elapsed,
                level = level,
                falling = survivors,
                lives = lives,
                landedWords = landed,
            )
        }
        refreshLiveStats()
        if (lives <= 0) finishGame()
    }

    /** Avoids spawning a word that is already falling (typing would be ambiguous). */
    private fun spawnWord(): FallingWord {
        val onScreen = _uiState.value.falling.mapTo(HashSet()) { it.text }
        var text: String
        var attempts = 0
        do {
            text = wordRepository.generateWords(language, 1).first()
            attempts++
        } while (text in onScreen && attempts < 5)
        return FallingWord(
            id = nextId++,
            text = text,
            x = 0.04f + random.nextFloat() * 0.72f,
            y = -0.05f,
            speed = BASE_SPEED + random.nextFloat() * SPEED_VARIANCE,
        )
    }

    fun onTypedTextChange(text: String) {
        if (!_uiState.value.canType()) return
        if (text.endsWith(' ')) {
            val candidate = text.trim()
            if (candidate.isNotEmpty()) tryDestroy(candidate)
            _uiState.update { it.copy(typedText = "") }
        } else {
            _uiState.update { it.copy(typedText = text) }
        }
    }

    fun onCharTyped(char: Char) = onTypedTextChange(_uiState.value.typedText + char)

    fun onBackspace() {
        if (!_uiState.value.canType()) return
        _uiState.update { it.copy(typedText = it.typedText.dropLast(1)) }
    }

    fun submitTyped() {
        if (!_uiState.value.canType()) return
        val candidate = _uiState.value.typedText.trim()
        if (candidate.isNotEmpty()) {
            tryDestroy(candidate)
            _uiState.update { it.copy(typedText = "") }
        }
    }

    /** Destroys the LOWEST matching word — the most urgent one. One atomic update. */
    private fun tryDestroy(candidate: String) {
        val target = _uiState.value.falling
            .filter { it.text == candidate }
            .maxByOrNull { it.y } ?: return
        _uiState.update { st ->
            st.copy(
                falling = st.falling - target,
                score = st.score + SCORE_PER_WORD * st.level,
                destroyedWords = st.destroyedWords + 1,
                destroyedChars = st.destroyedChars + target.text.length + 1,
            )
        }
        refreshLiveStats()
    }

    private fun refreshLiveStats() {
        _uiState.update { state ->
            state.copy(
                liveWpm = TypingMath.wpm(
                    state.destroyedChars,
                    state.elapsedMs.toLong().coerceAtLeast(1_000L),
                ),
                liveAccuracy = TypingMath.accuracy(state.destroyedWords, state.landedWords),
            )
        }
    }

    private fun finishGame() {
        if (_uiState.value.phase == RainPhase.FINISHED) return
        val state = _uiState.value
        val result = TypingResult(
            language = language,
            layout = layout,
            mode = GameMode.WordRain,
            wpm = state.liveWpm,
            accuracy = TypingMath.accuracy(state.destroyedWords, state.landedWords),
            correctWords = state.destroyedWords,
            incorrectWords = state.landedWords,
            durationSeconds = (state.elapsedMs / 1_000).toInt().coerceAtLeast(1),
            timestamp = System.currentTimeMillis(),
        )
        _uiState.update { it.copy(phase = RainPhase.FINISHED, finishedResult = result) }
        // Same pipeline as every other mode → XP, streak, achievements.
        viewModelScope.launch {
            _uiState.update { s -> s.copy(reward = recordGameResult(result)) }
        }
    }

    private fun spawnIntervalMs(level: Int): Long =
        (SPAWN_BASE_MS - level * SPAWN_STEP_MS).coerceAtLeast(SPAWN_MIN_MS)

    private companion object {
        const val STARTING_LIVES = 3
        const val MAX_DT_MS = 100.0
        const val LEVEL_PERIOD_MS = 15_000.0
        const val LEVEL_SPEED_FACTOR = 0.15
        const val BASE_SPEED = 0.10f          // ≈10s to cross at level 1
        const val SPEED_VARIANCE = 0.05f
        const val SPAWN_BASE_MS = 2_400L
        const val SPAWN_STEP_MS = 140L
        const val SPAWN_MIN_MS = 1_000L
        const val SCORE_PER_WORD = 10
        const val MAX_FALLING = 8
    }
}
