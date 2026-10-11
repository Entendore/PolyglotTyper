package com.example.polyglottyper

import com.example.polyglottyper.data.repository.WordRepositoryImpl
import com.example.polyglottyper.domain.model.Achievement
import com.example.polyglottyper.domain.model.GameMode
import com.example.polyglottyper.domain.model.KeyboardLayout
import com.example.polyglottyper.domain.model.Language
import com.example.polyglottyper.domain.model.PlayerSnapshot
import com.example.polyglottyper.domain.model.TypingResult
import com.example.polyglottyper.util.LevelMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GamificationTest {

    private fun result(wpm: Int = 50, accuracy: Int = 95, correct: Int = 20) = TypingResult(
        language = Language.ENGLISH,
        layout = KeyboardLayout.QWERTY,
        mode = GameMode.Timed(60),
        wpm = wpm,
        accuracy = accuracy,
        correctWords = correct,
        incorrectWords = 0,
        durationSeconds = 60,
    )

    // ── XP ────────────────────────────────────────────────────

    @Test
    fun `xp scales with accuracy`() {
        assertEquals(16, LevelMath.xpFor(result(accuracy = 80, correct = 20)))
        assertEquals(19, LevelMath.xpFor(result(accuracy = 95, correct = 20)))
    }

    @Test
    fun `small perfect runs get no bonus`() =
        assertEquals(5, LevelMath.xpFor(result(accuracy = 100, correct = 5)))

    @Test
    fun `perfect run earns bonus`() =
        assertEquals(35, LevelMath.xpFor(result(accuracy = 100, correct = 20)))

    @Test
    fun `level curve matches thresholds`() {
        assertEquals(1, LevelMath.levelFor(0))
        assertEquals(2, LevelMath.levelFor(50))
        assertEquals(3, LevelMath.levelFor(200))
        assertEquals(5, LevelMath.levelFor(800))
        assertTrue(LevelMath.progressToNextLevel(0) in 0f..1f)
        assertTrue(LevelMath.progressToNextLevel(25) > 0f)
    }

    // ── Achievements ─────────────────────────────────────────

    @Test
    fun `new player only unlocks first game`() {
        val snapshot = PlayerSnapshot(1, 0, 0, 1, 1, 0, 0, 0)
        val earned = Achievement.entries.filter { it.isUnlockedBy(snapshot) }.map { it.id }.toSet()
        assertEquals(setOf("first_game"), earned)
    }

    @Test
    fun `streak achievements use best streak`() {
        val snapshot = PlayerSnapshot(20, 30, 0, 1, 1, 7, 0, 0)
        assertTrue(Achievement.STREAK_7.isUnlockedBy(snapshot))
        assertTrue(Achievement.GAMES_10.isUnlockedBy(snapshot))
    }

    @Test
    fun `rain achievements read rain aggregates`() {
        val snapshot = PlayerSnapshot(5, 30, 0, 1, 1, 0, 130, 55)
        assertTrue(Achievement.RAIN_50.isUnlockedBy(snapshot))
        assertTrue(Achievement.RAIN_SURVIVOR.isUnlockedBy(snapshot))
    }

    // ── Seeded generation ────────────────────────────────────

    @Test
    fun `same seed produces identical daily words`() {
        val repo = WordRepositoryImpl()
        assertEquals(
            repo.generateWords(Language.FRENCH, 25, seed = 20_000),
            repo.generateWords(Language.FRENCH, 25, seed = 20_000),
        )
    }

    @Test
    fun `different seeds produce different words`() {
        val repo = WordRepositoryImpl()
        assertNotEquals(
            repo.generateWords(Language.FRENCH, 25, seed = 1),
            repo.generateWords(Language.FRENCH, 25, seed = 2),
        )
    }
}
