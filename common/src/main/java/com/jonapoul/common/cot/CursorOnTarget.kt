package com.jonapoul.common.cot

import android.annotation.SuppressLint
import android.os.Build
import com.jonapoul.common.cot.proto.ContactOuterClass.Contact
import com.jonapoul.common.cot.proto.Cotevent.CotEvent
import com.jonapoul.common.cot.proto.DetailOuterClass
import com.jonapoul.common.cot.proto.GroupOuterClass
import com.jonapoul.common.cot.proto.StatusOuterClass
import com.jonapoul.common.cot.proto.Takmessage.TakMessage
import com.jonapoul.common.cot.proto.TakvOuterClass.Takv
import com.jonapoul.common.cot.proto.TrackOuterClass
import com.jonapoul.common.di.IBuildResources
import com.jonapoul.common.utils.DataFormat
import com.jonapoul.common.utils.TimeUtils
import com.jonapoul.common.utils.exhaustive
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

open class CursorOnTarget(buildResources: IBuildResources?) {
    var how = "m-g"
    var type = "a-f-G-U-C"

    // User info
    var uid: String? = null // unique ID of the device. Stays constant when changing callsign

    // Time info
    var time: UtcTimestamp // time when the icon was created
    var start: UtcTimestamp // time when the icon is considered valid
    var stale: UtcTimestamp // time when the icon is considered invalid

    // Contact info
    var callsign = "CALLSIGN" // ATAK callsign
    private var callsignAddendum: String = ""     // Used to include information about device doze mode, if it's active

    // Position and movement info
    var hae = 0.0 // height above ellipsoid in metres
    var lat = 0.0 // latitude in decimal degrees
    var lon = 0.0 // longitude in decimal degrees
    var ce = 0.0 // circular (radial) error in metres. applies to 2D position only
    var le = 0.0 // linear error in metres. applies to altitude only
    var course = 0.0 // ground bearing in decimal degrees
    var speed = 0.0 // ground velocity in m/s. Doesn't include altitude climb rate

    // Group
    var team = CotTeam.CYAN // cyan, green, purple, etc
    var role = CotRole.TEAM_MEMBER // HQ, sniper, K9, etc

    // Location source
    var altsrc = "GENERATED"
    var geosrc = "GENERATED"

    // System info
    var battery = 100                                   // internal battery charge percentage, scale of 1-100
    val device = getDeviceName()                 // Android device model
    val platform = buildResources?.platform      // application name
    val os = Build.VERSION.SDK_INT.toString()    // Android SDK version number
    val version = buildResources?.versionName    // application version number

    fun toBytes(dataFormat: DataFormat): ByteArray {
        return when (dataFormat) {
            DataFormat.XML -> toXml()
            DataFormat.PROTOBUF -> toProtobuf()
        }.exhaustive.also {
            Timber.d(String(it))
        }
    }

    fun setStaleDiff(dt: Long, timeUnit: TimeUnit) {
        stale = start.add(dt, timeUnit)
    }

    fun setDozeModeTags(isDozeMode: Boolean, lastGpsUpdateMs: Long) {
        val diffMs = (System.currentTimeMillis() - lastGpsUpdateMs)
        /* If we're in doze mode, append a tag to the callsign so their status is visible on the map */
        callsignAddendum = if (isDozeMode) {
            Timber.i("Setting doze mode tag")
            " DOZING FOR ${TimeUtils.msToString(diffMs)}"
        } else ""
    }

    protected open fun toXml(): ByteArray {
        return String.format(Locale.ENGLISH,
                "<event version=\"2.0\" uid=\"%s\" type=\"%s\" time=\"%s\" start=\"%s\" stale=\"%s\" how=\"%s\"><point lat=\"%.7f\" " +
                        "lon=\"%.7f\" hae=\"%f\" ce=\"%f\" le=\"%f\"/><detail><track speed=\"%.7f\" course=\"%.7f\"/><contact callsign=\"%s%s\"/>" +
                        "<__group name=\"%s\" role=\"%s\"/><takv device=\"%s\" platform=\"%s\" os=\"%s\" version=\"%s\"/><status battery=\"%d\"/>" +
                        "<precisionlocation altsrc=\"%s\" geopointsrc=\"%s\" /></detail></event>",
                uid, type, time.isoTimestamp(), start.isoTimestamp(), stale.isoTimestamp(), how, lat, lon, hae, ce, le, speed,
                course, callsign, callsignAddendum, team.toString(), role.toString(), device, platform, os, version, battery, altsrc, geosrc)
                .toByteArray()
    }

    protected open fun toProtobuf(): ByteArray {
        val cotBytes = TakMessage.newBuilder()
                .setCotEvent(CotEvent.newBuilder()
                        .setType(type)
                        .setUid(uid)
                        .setHow(how)
                        .setSendTime(time.milliseconds())
                        .setStartTime(start.milliseconds())
                        .setStaleTime(stale.milliseconds())
                        .setLat(lat)
                        .setLon(lon)
                        .setHae(hae)
                        .setCe(ce)
                        .setLe(le)
                        .setDetail(DetailOuterClass.Detail.newBuilder()
                                .setGroup(GroupOuterClass.Group.newBuilder()
                                        .setName(team.toString())
                                        .setRole(role.toString())
                                        .build())
                                .setTakv(Takv.newBuilder()
                                        .setDevice(device)
                                        .setPlatform(platform)
                                        .setOs(os)
                                        .setVersion(version)
                                        .build())
                                .setStatus(StatusOuterClass.Status.newBuilder()
                                        .setBattery(battery)
                                        .build())
                                .setTrack(TrackOuterClass.Track.newBuilder()
                                        .setCourse(course)
                                        .setSpeed(speed)
                                        .build())
                                .setContact(Contact.newBuilder()
                                        .setCallsign(callsign)
                                        .build())
                                .build())
                        .build())
                .build()
                .toByteArray()
        return TAK_HEADER + cotBytes
    }

    @SuppressLint("DefaultLocale")
    private fun getDeviceName(): String {
        return try {
            "${Build.MANUFACTURER.toUpperCase()} ${Build.MODEL.toUpperCase()}"
        } catch (e: NullPointerException) {
            /* Thrown in unit tests */
            "DEVICE"
        }
    }

    companion object {
        const val MAGIC_BYTE = 0xbf.toByte()

        // Prepended to every protobuf packet
        val TAK_HEADER = byteArrayOf(
                MAGIC_BYTE,
                0x01.toByte(), // protocol version
                MAGIC_BYTE
        )
    }

    init {
        val now = UtcTimestamp.now()
        start = now
        time = now
        stale = start.add(10, TimeUnit.MINUTES)
    }
}
