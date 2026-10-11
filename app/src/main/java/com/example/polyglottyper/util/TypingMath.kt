package com.example.polyglottyper.util

import kotlin.math.roundToInt

object TypingMath {

    /** Standard WPM: correct characters ÷ 5, per minute. */
    fun wpm(correctChars: Int, elapsedMs: Long): Int {
        if (elapsedMs <= 0) return 0
        val minutes = elapsedMs / 60_000.0
        return (correctChars / 5.0 / minutes).roundToInt()
    }

    fun accuracy(correct: Int, incorrect: Int): Int {
        val total = correct + incorrect
        if (total == 0) return 100
        return (correct * 100.0 / total).roundToInt()
    }

    /** +1 char per correctly typed word counts as its separator. */
    fun correctChars(words: List<String>, results: List<Boolean>): Int =
        words.asSequence()
            .take(results.size)
            .mapIndexed { index, word -> if (results[index]) word.length + 1 else 0 }
            .sum()

    /**
     * Correct characters for the live WPM display: completed words plus the
     * correctly typed prefix of the in-progress word (how real typing tests score).
     * Final results still use [correctChars] for predictability.
     */
    fun liveCorrectChars(words: List<String>, results: List<Boolean>, typed: String): Int {
        var total = results.foldIndexed(0) { index, acc, ok ->
            acc + if (ok) (words.getOrNull(index)?.length ?: 0) + 1 else 0
        }
        val target = words.getOrNull(results.size) ?: return total
        val n = minOf(typed.length, target.length)
        for (i in 0 until n) if (typed[i] == target[i]) total += 1
        return total
    }

    /**
     * The next character the player should type: the next letter of the current
     * word, or a space once the word is complete. Null when there is no word.
     */
    fun nextExpectedChar(words: List<String>, wordIndex: Int, typed: String): Char? {
        val target = words.getOrNull(wordIndex) ?: return null
        return if (typed.length < target.length) target[typed.length] else ' '
    }
}
