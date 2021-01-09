package com.jonapoul.common.cot

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class UtcTimestampTest {
    /* List of pre-calculated translations between epoch milliseconds and ISO 8601 string */
    private data class ExpectedResult(var ms: Long, var iso: String)
    private val expectedResults = listOf(
            ExpectedResult(1606836655789L, "2020-12-01T15:30:55.789Z"), // March 2020
            ExpectedResult(1624583396623L, "2021-06-25T01:09:56.623Z"), // June 2021
            ExpectedResult(1663110062235L, "2022-09-13T23:01:02.235Z"), // September 2022
            ExpectedResult(1701444655789L, "2023-12-01T15:30:55.789Z") // December 2023
    )

    @Test
    fun constructor_LongToString() {
        expectedResults.forEach {
            val timestamp = UtcTimestamp(it.ms)
            assertThat(timestamp.isoTimestamp(), equalTo(it.iso))
        }
    }

    @Test
    fun constructor_StringToLong() {
        expectedResults.forEach {
            val timestamp = UtcTimestamp(it.iso)
            assertThat(timestamp.milliseconds(), equalTo(it.ms))
        }
    }

    @Test
    fun now() {
        val nowMs = System.currentTimeMillis()
        val nowUtc = UtcTimestamp.now()
        val dt = (nowUtc.milliseconds() - nowMs).toInt()
        assertThat(dt, lessThan(10))
    }

    @Test
    fun add_OneHour() {
        val base = UtcTimestamp(BASELINE_ISO)
        val added = base.add(1, TimeUnit.HOURS)
        assertThat(added.isoTimestamp(), equalTo("2020-07-15T15:26:53.123Z"))
    }

    @Test
    fun add_NegativeOneDay() {
        val base = UtcTimestamp(BASELINE_ISO)
        val added = base.add(-1, TimeUnit.DAYS)
        assertThat(added.isoTimestamp(), equalTo("2020-07-14T14:26:53.123Z"))
    }

    @Test
    fun add_Nothing() {
        val base = UtcTimestamp(BASELINE_ISO)
        val added = base.add(0, TimeUnit.SECONDS)
        assertThat(added.isoTimestamp(), equalTo(BASELINE_ISO))
    }

    companion object {
        private const val BASELINE_ISO = "2020-07-15T14:26:53.123Z"
    }
}
