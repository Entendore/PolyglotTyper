package com.example.polyglottyper

import com.example.polyglottyper.ui.game.SavedGameState
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class SavedGameStateTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `game state round-trips through json`() {
        val saved = SavedGameState(
            words = listOf("café", "naïve", "straße"),
            wordResults = listOf(true, false),
            currentWordIndex = 2,
            elapsedMs = 45_678L,
            wasPaused = false,
        )
        val restored = json.decodeFromString<SavedGameState>(json.encodeToString(saved))
        assertEquals(saved, restored)
    }

    @Test
    fun `empty game state round-trips`() {
        val saved = SavedGameState(emptyList(), emptyList(), 0, 0, wasPaused = true)
        val restored = json.decodeFromString<SavedGameState>(json.encodeToString(saved))
        assertEquals(saved, restored)
    }
}
