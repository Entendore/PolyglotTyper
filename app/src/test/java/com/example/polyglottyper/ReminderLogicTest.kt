package com.example.polyglottyper

import com.example.polyglottyper.util.ReminderLogic
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderLogicTest {

    @Test
    fun `notifies when the last daily was before today`() {
        assertTrue(ReminderLogic.shouldNotify(dailyCompletedOn = 20_000, todayEpochDay = 20_001))
    }

    @Test
    fun `silent when today is already done`() {
        assertFalse(ReminderLogic.shouldNotify(dailyCompletedOn = 20_001, todayEpochDay = 20_001))
    }

    @Test
    fun `silent for players who never played a daily`() {
        assertFalse(ReminderLogic.shouldNotify(dailyCompletedOn = null, todayEpochDay = 20_001))
    }

    @Test
    fun `silent for future timestamps (clock skew safety)`() {
        assertFalse(ReminderLogic.shouldNotify(dailyCompletedOn = 20_002, todayEpochDay = 20_001))
    }
}
