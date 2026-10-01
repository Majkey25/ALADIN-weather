package cz.majkey.pocasicesko.notification

import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in cold-process probe using real forecast data, never the production package. */
class BackgroundDeliveryProbe {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun prepare() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("backgroundProbe") == "true")
        check(context.packageName.endsWith(".debug"))
        val scheduler = context.getSystemService(JobScheduler::class.java)
        check(scheduler.getPendingJob(7003) == null) { "Existing refresh work must finish first" }
        val backup = context.getSharedPreferences("notification_probe", Context.MODE_PRIVATE)
        check(!backup.contains("enabled")) { "Restore the preceding probe first" }
        check(backup.edit().putBoolean("enabled", DailyBriefingScheduler.isEnabled(context))
            .putString("pending_day", context.getSharedPreferences("weather_refresh", Context.MODE_PRIVATE)
                .getString("briefing_day", null)).commit())
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            DailyBriefingScheduler.setEnabled(context, true)
            WeatherRefreshScheduler.request(context, briefing = true)
            check(scheduler.schedule(JobInfo.Builder(7003, ComponentName(context, WeatherRefreshJob::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(120_000)
                .setPersisted(true).build()) == JobScheduler.RESULT_SUCCESS)
            context.getSystemService(NotificationManager::class.java).cancel(DailyBriefingScheduler.NOTIFICATION_ID)
        }
    }

    @Test
    fun restore() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("backgroundProbe") == "true")
        check(context.packageName.endsWith(".debug"))
        val backup = context.getSharedPreferences("notification_probe", Context.MODE_PRIVATE)
        check(backup.contains("enabled"))
        context.getSystemService(JobScheduler::class.java).cancel(7003)
        DailyBriefingScheduler.setEnabled(context, backup.getBoolean("enabled", false))
        check(context.getSharedPreferences("weather_refresh", Context.MODE_PRIVATE).edit()
            .putString("briefing_day", backup.getString("pending_day", null)).commit())
        context.getSystemService(NotificationManager::class.java).cancel(DailyBriefingScheduler.NOTIFICATION_ID)
        check(backup.edit().clear().commit())
    }
}
