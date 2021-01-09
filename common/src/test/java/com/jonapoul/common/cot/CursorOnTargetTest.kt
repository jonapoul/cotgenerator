package com.jonapoul.common.cot

import com.jonapoul.common.utils.DataFormat
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.lessThan
import org.hamcrest.core.StringRegularExpression.matchesRegex
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class CursorOnTargetTest {
    private lateinit var cot: CursorOnTarget

    @Before
    fun initialise() {
//        cot = CursorOnTarget()
//        cot.start = UtcTimestamp.now()
//        cot.time = cot.start
//        cot.stale = cot.start.add(10, TimeUnit.MINUTES)
    }

    @Test
    fun toBytes_Xml() {
        val xml = String(cot.toBytes(DataFormat.XML))
        assertThat(xml, containsXml("event", "uid", cot.uid))
        assertThat(xml, containsXml("event", "type", cot.type))
        assertThat(xml, containsXml("event", "time", cot.time.isoTimestamp()))
        assertThat(xml, containsXml("event", "start", cot.start.isoTimestamp()))
        assertThat(xml, containsXml("event", "stale", cot.stale.isoTimestamp()))
        assertThat(xml, containsXml("event", "how", cot.how))
        assertThat(xml, containsXml("point", "lat", "%.7f".format(cot.lat)))
        assertThat(xml, containsXml("point", "lon", "%.7f".format(cot.lon)))
        assertThat(xml, containsXml("point", "hae", "%f".format(cot.hae)))
        assertThat(xml, containsXml("point", "ce", "%f".format(cot.ce)))
        assertThat(xml, containsXml("point", "le", "%f".format(cot.le)))
        assertThat(xml, containsXml("track", "speed", "%.7f".format(cot.speed)))
        assertThat(xml, containsXml("track", "course", "%.7f".format(cot.course)))
        assertThat(xml, containsXml("contact", "callsign", cot.callsign))
        assertThat(xml, containsXml("__group", "name", cot.team.toString()))
        assertThat(xml, containsXml("__group", "role", cot.role.toString()))
        assertThat(xml, containsXml("takv", "device", cot.device))
        assertThat(xml, containsXml("takv", "platform", cot.platform))
        assertThat(xml, containsXml("takv", "os", cot.os))
        assertThat(xml, containsXml("takv", "version", cot.version))
        assertThat(xml, containsXml("status", "battery", cot.battery.toString()))
        assertThat(xml, containsXml("precisionlocation", "altsrc", cot.altsrc))
        assertThat(xml, containsXml("precisionlocation", "geopointsrc", cot.geosrc))
    }

    @Test
    fun toBytes_Protobuf_CheckHeader() {
        val protobuf = cot.toBytes(DataFormat.PROTOBUF)
        assertThat(protobuf[0], equalTo(0xbf.toByte()))
        assertThat(protobuf[1], equalTo(0x01.toByte()))
        assertThat(protobuf[2], equalTo(0xbf.toByte()))
        assertThat(protobuf.size, lessThan(cot.toBytes(DataFormat.XML).size))
    }

    @Test
    fun setStaleDiff_OneHour() {
        cot.setStaleDiff(1, TimeUnit.HOURS)
        assertThat(cot.stale.milliseconds() - cot.start.milliseconds(), equalTo(TimeUnit.HOURS.toMillis(1L)))
    }

    @Test
    fun setStaleDiff_1MillionDays() {
        cot.setStaleDiff(1e6.toLong(), TimeUnit.DAYS)
        assertThat(cot.stale.milliseconds() - cot.start.milliseconds(), equalTo(TimeUnit.DAYS.toMillis(1e6.toLong())))
    }

    @Test
    fun setStaleDiff_Zero() {
        cot.setStaleDiff(0, TimeUnit.MILLISECONDS)
        assertThat(cot.stale.milliseconds() - cot.start.milliseconds(), equalTo(0L))
    }

    @Test
    fun setStaleDiff_Negative() {
        cot.setStaleDiff(-1, TimeUnit.HOURS)
        assertThat(cot.stale.milliseconds() - cot.start.milliseconds(), equalTo(TimeUnit.HOURS.toMillis(-1L)))
    }

    private fun containsXml(tag: String, attribute: String, value: String?): Matcher<String> {
        return matchesRegex(".*<%s.*?%s=\"%s\".*?>.*".format(tag, attribute, value))
    }
}
