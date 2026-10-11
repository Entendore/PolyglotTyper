package com.example.polyglottyper.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

object ReminderScheduler {

    private const val WORK_NAME = "daily_reminder"
    private val REMINDER_TIME: LocalTime = LocalTime.of(18, 0) // evening nudge

    /**
     * Idempotent — the KEEP policy means calling this on every app launch
     * never duplicates or reschedules the existing periodic work.
     */
    fun schedule(context: Context) {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(REMINDER_TIME)
        if (!next.isAfter(now)) next = next.plusDays(1)

        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(Duration.ofDays(1))
            .setInitialDelay(Duration.between(now, next))
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
