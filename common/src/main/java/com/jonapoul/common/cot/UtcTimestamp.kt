package com.jonapoul.common.cot

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/* For easier translating between milliseconds and ISO timestamp strings. */
class UtcTimestamp(private var milliseconds: Long) {

    constructor(timestampString: String) : this(stringToLong(timestampString))

    fun milliseconds(): Long = milliseconds

    fun isoTimestamp(): String = longToString(milliseconds)

    fun shortLocalTimestamp(): String {
        /* For an ISO string of "1970-01-02Z03:04:56.111", returns "03:04:56" */
        return LOCAL_TIME_FORMATTER.format(Instant.ofEpochMilli(milliseconds))
    }

    fun add(differenceMilliseconds: Long): UtcTimestamp = UtcTimestamp(milliseconds + differenceMilliseconds)

    fun add(dt: Long, timeUnit: TimeUnit): UtcTimestamp = add(timeUnit.toMillis(dt))

    companion object {
        private val LOCAL_TIME_FORMATTER = DateTimeFormatter
                .ofPattern("HH:mm:ss")
                .withZone(ZoneOffset.systemDefault())

        fun now(): UtcTimestamp = UtcTimestamp(System.currentTimeMillis())

        private fun stringToLong(timestamp: String): Long = Instant.parse(timestamp).toEpochMilli()

        private fun longToString(ms: Long): String = Instant.ofEpochMilli(ms).toString()
    }
}
