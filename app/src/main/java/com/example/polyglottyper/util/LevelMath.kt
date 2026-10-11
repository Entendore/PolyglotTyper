package com.example.polyglottyper.util

import com.example.polyglottyper.domain.model.TypingResult
import kotlin.math.floor
import kotlin.math.sqrt

object LevelMath {

    private const val XP_PER_LEVEL_STEP = 50

    /** Accuracy-weighted XP, with a bonus for flawless runs. */
    fun xpFor(result: TypingResult): Int {
        val base = result.correctWords * result.accuracy / 100
        val perfectBonus = if (result.accuracy == 100 && result.correctWords >= 10) 15 else 0
        return base + perfectBonus
    }

    /** Level curve: L2 at 50 XP, L3 at 200, L4 at 450, L5 at 800… */
    fun levelFor(xp: Int): Int =
        floor(sqrt(xp / XP_PER_LEVEL_STEP.toDouble())).toInt() + 1

    fun xpForLevel(level: Int): Int = (level - 1) * (level - 1) * XP_PER_LEVEL_STEP

    fun xpToNextLevel(xp: Int): Int = xpForLevel(levelFor(xp) + 1) - xp

    fun progressToNextLevel(xp: Int): Float {
        val current = xpForLevel(levelFor(xp))
        val next = xpForLevel(levelFor(xp) + 1)
        return ((xp - current).toFloat() / (next - current)).coerceIn(0f, 1f)
    }
}
