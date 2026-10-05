package cz.majkey.pocasicesko.data

import java.io.IOException
import java.time.Instant
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ChmiCurrentConditionsRepositoryTest {
    @Test
    fun threeNearestRequestsOverlapAndResultsKeepStationOrder() {
        val started = CountDownLatch(3)
        val requests = AtomicInteger()
        val repository = ChmiCurrentConditionsRepository(stations) { station, date ->
            assertEquals("20261005", date)
            requests.incrementAndGet()
            started.countDown()
            assertTrue(started.await(2, TimeUnit.SECONDS))
            if (station.stationId == "0-1") Thread.sleep(20)
            payload(station)
        }

        val observations = repository.fetch(location, now)
        assertEquals(listOf("0-1", "0-2", "0-3"), observations.map { it.stationId })
        assertEquals(stations.take(3).map { parseCurrentStationObservation(payload(it), it) }, observations)
        assertEquals(3, requests.get())
    }

    @Test
    fun unavailableAndMalformedStationsRemainOptionalButCancellationEscapes() {
        val repository = ChmiCurrentConditionsRepository(stations) { station, _ ->
            when (station.stationId) {
                "0-1" -> throw IOException("Unavailable fixture")
                "0-2" -> "invalid json"
                else -> payload(station)
            }
        }
        assertEquals(listOf("0-3"), repository.fetch(location, now).map { it.stationId })
        val cancelled = ChmiCurrentConditionsRepository(stations) { _, _ ->
            throw CancellationException("Cancelled fixture")
        }
        assertThrows(CancellationException::class.java) { cancelled.fetch(location, now) }
    }

    @Test
    fun callerInterruptionStopsStationWorkAndPreservesInterruptStatus() {
        val started = CountDownLatch(3)
        val finished = CountDownLatch(3)
        val blocked = CountDownLatch(1)
        val failure = AtomicReference<Throwable>()
        val interrupted = AtomicBoolean()
        val repository = ChmiCurrentConditionsRepository(stations) { station, _ ->
            started.countDown()
            try {
                blocked.await()
                payload(station)
            } finally {
                finished.countDown()
            }
        }
        val caller = Thread {
            try {
                repository.fetch(location, now)
            } catch (error: Throwable) {
                failure.set(error)
                interrupted.set(Thread.currentThread().isInterrupted)
            }
        }
        try {
            caller.start()
            assertTrue(started.await(2, TimeUnit.SECONDS))
            caller.interrupt()
            caller.join(2_000)
            assertFalse(caller.isAlive)
            assertTrue(finished.await(2, TimeUnit.SECONDS))
            assertTrue(failure.get() is CancellationException)
            assertTrue(interrupted.get())
        } finally {
            blocked.countDown()
            caller.interrupt()
            caller.join(2_000)
        }
    }

    private val location = CzechLocation("Fixture", REGION_PRAGUE, 50.0, 14.0)
    private val now = Instant.parse("2026-10-05T12:00:00Z")
    private val stations = (1..4).map { index ->
        CurrentStation("0-$index", "Fixture $index", 50.0, 14.0 + index * 0.01, 200.0, false)
    }

    private fun payload(station: CurrentStation): String = """
        {"data":{"data":{"header":"STATION,ELEMENT,DT,VAL,FLAG,QUALITY","values":[
        ["${station.stationId}","T","2026-10-05T11:50:00Z",20.0,"",5],
        ["${station.stationId}","SRA10M","2026-10-05T11:50:00Z",0.0,"",5]
        ]}}}
    """.trimIndent()
}
