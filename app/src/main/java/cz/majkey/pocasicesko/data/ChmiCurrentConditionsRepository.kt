package cz.majkey.pocasicesko.data

import android.content.Context
import cz.majkey.pocasicesko.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

internal class ChmiCurrentConditionsRepository(
    private val stations: List<CurrentStation>,
    private val fetchText: (CurrentStation, String) -> String = ::request,
) {
    constructor(context: Context) : this(
        context.assets.open(STATION_CATALOG_ASSET).bufferedReader().use { source ->
            decodeCurrentStationCatalog(source.readText())
        },
    )

    fun fetch(location: CzechLocation, now: Instant): List<CurrentStationObservation> {
        ensureForecastThreadActive()
        val date = now.atZone(ZoneOffset.UTC).toLocalDate().format(DATE_FORMAT)
        val worker = Executors.newFixedThreadPool(REQUIRED_STATION_COUNT)
        return try {
            worker.invokeAll(nearestCurrentStations(location, stations, REQUIRED_STATION_COUNT).map { station ->
                Callable {
                    ensureForecastThreadActive()
                    val observation = runCatching {
                        parseCurrentStationObservation(fetchText(station, date), station)
                    }.onFailure { error ->
                        if (error is CancellationException) throw error
                    }.getOrNull()
                    ensureForecastThreadActive()
                    observation
                }
            }).mapNotNull { it.get() }
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            throw CancellationException("Weather refresh interrupted.").apply { initCause(error) }
        } catch (error: ExecutionException) {
            throw error.cause ?: error
        } finally {
            worker.shutdownNow()
        }
    }

    companion object {
        private fun request(station: CurrentStation, date: String): String {
            val url = "$CHMI_CURRENT_ROOT/10m-${station.stationId}-$date.json"
            val connection = URL(url).openConnection() as HttpURLConnection
            return try {
                connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
                connection.readTimeout = READ_TIMEOUT_MILLIS
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("User-Agent", USER_AGENT)
                if (connection.responseCode !in 200..299) {
                    throw IOException("ČHMÚ returned HTTP ${connection.responseCode}.")
                }
                connection.inputStream.use { readLimited(it, MAX_RESPONSE_BYTES).toString(Charsets.UTF_8) }
            } finally {
                connection.disconnect()
            }
        }

        private const val STATION_CATALOG_ASSET = "chmi_current_stations.json"
        private const val CHMI_CURRENT_ROOT = "https://opendata.chmi.cz/meteorology/climate/now/data"
        private const val REQUIRED_STATION_COUNT = 3
        private const val CONNECT_TIMEOUT_MILLIS = 4_000
        private const val READ_TIMEOUT_MILLIS = 6_000
        private const val MAX_RESPONSE_BYTES = 2_000_000
        private val DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE
        private val USER_AGENT =
            "Selia-Weather/${BuildConfig.VERSION_NAME} (Android; https://github.com/Majkey25/Selia-Weather)"
    }
}
