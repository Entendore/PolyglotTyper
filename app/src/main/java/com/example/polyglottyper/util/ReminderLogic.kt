package com.example.polyglottyper.util

object ReminderLogic {

    /**
     * Notify only when the player has played a Daily before (so new users are
     * never nagged) and hasn't completed today's yet.
     */
    fun shouldNotify(dailyCompletedOn: Long?, todayEpochDay: Long): Boolean =
        dailyCompletedOn != null && dailyCompletedOn < todayEpochDay
}
