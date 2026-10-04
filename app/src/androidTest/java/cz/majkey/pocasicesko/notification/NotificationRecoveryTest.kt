package cz.majkey.pocasicesko.notification

import android.app.job.JobScheduler
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import java.time.LocalDate
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRecoveryTest {
    private val base get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val context get() = object : ContextWrapper(base) {
        override fun getSharedPreferences(name: String, mode: Int) =
            super.getSharedPreferences("notification_recovery_test_$name", mode)
    }

    @Test
    fun missedMorningBriefingExpiresInsteadOfArrivingWhenAppOpensLater() {
        val context = context
        context.getSharedPreferences("daily_briefing", Context.MODE_PRIVATE).edit().putBoolean("enabled", true).commit()
        val preferences = context.getSharedPreferences("weather_refresh", Context.MODE_PRIVATE)
        preferences.edit().putString("briefing_day", LocalDate.now().toString())
            .putLong("briefing_scheduled_at", System.currentTimeMillis() - 2 * 60 * 60 * 1000L).commit()
        assertNull(WeatherRefreshScheduler.pendingBriefing(context))
    }

    @Test
    fun bootAndUpdateRestoreAlertsWithMorningBriefingOff() {
        val context = context
        check(base.packageName.endsWith(".debug"))
        val scheduler = base.getSystemService(JobScheduler::class.java)
        val previous = scheduler.getPendingJob(WeatherAlertScheduler.JOB_ID)
        context.getSharedPreferences("daily_briefing", Context.MODE_PRIVATE).edit().putBoolean("enabled", false).commit()
        WeatherAlertSettings(rainEnabled = true, officialWarningsEnabled = true).save(context)
        try {
            for (action in listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) {
                scheduler.cancel(WeatherAlertScheduler.JOB_ID)
                DailyBriefingReceiver().onReceive(context, Intent(action))
                val job = scheduler.getPendingJob(WeatherAlertScheduler.JOB_ID)
                assertNotNull("$action must restore alerts without opening an activity", job)
                assertTrue(requireNotNull(job).isPersisted)
                assertTrue(job.isPeriodic)
                assertFalse(DailyBriefingScheduler.isEnabled(context))
            }
        } finally {
            scheduler.cancel(WeatherAlertScheduler.JOB_ID)
            previous?.let(scheduler::schedule)
        }
    }

    @Test
    fun recoveryDoesNotEnableNotificationsTheUserTurnedOff() {
        val context = context
        check(base.packageName.endsWith(".debug"))
        val scheduler = base.getSystemService(JobScheduler::class.java)
        val previous = scheduler.getPendingJob(WeatherAlertScheduler.JOB_ID)
        context.getSharedPreferences("daily_briefing", Context.MODE_PRIVATE).edit().putBoolean("enabled", false).commit()
        WeatherAlertSettings(rainEnabled = false, officialWarningsEnabled = false).save(context)
        try {
            scheduler.cancel(WeatherAlertScheduler.JOB_ID)
            DailyBriefingReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
            assertNull(scheduler.getPendingJob(WeatherAlertScheduler.JOB_ID))
            assertFalse(DailyBriefingScheduler.isEnabled(context))
        } finally {
            scheduler.cancel(WeatherAlertScheduler.JOB_ID)
            previous?.let(scheduler::schedule)
        }
    }
}
