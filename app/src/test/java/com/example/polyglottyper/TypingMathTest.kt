package com.example.polyglottyper

import com.example.polyglottyper.util.TypingMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TypingMathTest {

    @Test
    fun `wpm is zero before any time elapses`() =
        assertEquals(0, TypingMath.wpm(correctChars = 100, elapsedMs = 0))

    @Test
    fun `300 correct chars in one minute equals 60 wpm`() =
        assertEquals(60, TypingMath.wpm(correctChars = 300, elapsedMs = 60_000))

    @Test
    fun `accuracy is 100 with no attempts`() =
        assertEquals(100, TypingMath.accuracy(correct = 0, incorrect = 0))

    @Test
    fun `accuracy counts wrong words`() =
        assertEquals(80, TypingMath.accuracy(correct = 8, incorrect = 2))

    @Test
    fun `correct chars include word separators`() {
        // cat(3)+1 + 0 + bird(4)+1 = 9
        assertEquals(9, TypingMath.correctChars(listOf("cat", "dog", "bird"), listOf(true, false, true)))
    }

    @Test
    fun `next expected char walks the current word then space`() {
        val words = listOf("héllo", "world")
        assertEquals('h', TypingMath.nextExpectedChar(words, 0, ""))
        assertEquals('é', TypingMath.nextExpectedChar(words, 0, "h"))
        assertEquals(' ', TypingMath.nextExpectedChar(words, 0, "héllo"))
        assertEquals('w', TypingMath.nextExpectedChar(words, 1, ""))
    }

    @Test
    fun `next expected char is null with no words`() =
        assertNull(TypingMath.nextExpectedChar(emptyList(), 0, ""))

    @Test
    fun `live correct chars counts completed words plus matched prefix`() {
        val words = listOf("cat", "dog")
        val results = listOf(true) // "cat" submitted correctly → 3 chars + separator
        assertEquals(4, TypingMath.liveCorrectChars(words, results, ""))
        assertEquals(6, TypingMath.liveCorrectChars(words, results, "do"))   // 4 + 2 matched
        assertEquals(7, TypingMath.liveCorrectChars(words, results, "dogx")) // 4 + 3 matched
        assertEquals(4, TypingMath.liveCorrectChars(words, results, "xx"))   // 4 + 0 matched
    }

    @Test
    fun `live correct chars with nothing submitted uses typed prefix only`() =
        assertEquals(2, TypingMath.liveCorrectChars(listOf("cat"), emptyList(), "ca"))
}
