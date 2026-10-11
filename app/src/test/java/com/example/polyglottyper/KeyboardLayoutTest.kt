package com.example.polyglottyper

import com.example.polyglottyper.data.words.WordLists
import com.example.polyglottyper.domain.model.KeyboardLayout
import com.example.polyglottyper.domain.model.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutTest {

    @Test
    fun `every layout has non-empty rows and no duplicate keys`() {
        KeyboardLayout.entries.forEach { layout ->
            assertTrue(
                "${layout.name}: rows must be non-empty",
                layout.rows.isNotEmpty() && layout.rows.all { it.isNotEmpty() },
            )
            val all = layout.rows.flatten()
            assertEquals("${layout.name}: duplicate key found", all.size, all.toSet().size)
        }
    }

    @Test
    fun `ids round-trip through fromId`() {
        KeyboardLayout.entries.forEach { layout ->
            assertEquals(layout, KeyboardLayout.fromId(layout.id))
        }
    }

    @Test
    fun `unknown id falls back to qwerty`() =
        assertEquals(KeyboardLayout.QWERTY, KeyboardLayout.fromId("nope"))

    @Test
    fun `automatic recommendation follows language`() {
        assertEquals(KeyboardLayout.AZERTY, KeyboardLayout.recommendedFor(Language.FRENCH))
        assertEquals(KeyboardLayout.QWERTZ, KeyboardLayout.recommendedFor(Language.GERMAN))
        assertEquals(KeyboardLayout.TURKISH_F, KeyboardLayout.recommendedFor(Language.TURKISH))
        assertEquals(KeyboardLayout.QWERTY, KeyboardLayout.recommendedFor(Language.SPANISH))
    }

    @Test
    fun `turkish words are fully typeable on turkish f layout`() {
        val typeable = KeyboardLayout.TURKISH_F.rows.flatten().toSet() +
            Language.TURKISH.specialChars.toSet()
        WordLists.forLanguage(Language.TURKISH)
            .flatMap { it.toList() } // Kotlin stdlib: String → List<Char>
            .forEach { char -> assertTrue("missing key: $char", char in typeable) }
    }
}
