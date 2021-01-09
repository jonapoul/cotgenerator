package com.jonapoul.cotbeacon.cot

import com.jonapoul.common.cot.CursorOnTarget
import com.jonapoul.common.cot.UtcTimestamp
import com.jonapoul.common.cot.proto.Cotevent
import com.jonapoul.common.cot.proto.DetailOuterClass
import com.jonapoul.common.cot.proto.Precisionlocation
import com.jonapoul.common.cot.proto.Takmessage
import com.jonapoul.common.repositories.IGpsRepository
import java.util.*
import java.util.concurrent.TimeUnit

class EmergencyCursorOnTarget(
        private val emergencyType: EmergencyType,
        gpsRepository: IGpsRepository,
        uid: String,
        callsign: String
) : CursorOnTarget(null) {

    init {
        type = emergencyType.type
        how = "m-g"

        this.callsign = callsign
        this.uid = uid

        val now = UtcTimestamp.now()
        start = now
        time = now
        stale = start.add(15, TimeUnit.SECONDS)

        lat = gpsRepository.latitude()
        lon = gpsRepository.longitude()
        hae = gpsRepository.altitude()
        ce = gpsRepository.circularError90()
        le = gpsRepository.linearError90()
    }

    override fun toXml(): ByteArray {
        return String.format(Locale.ENGLISH,
                "<event version=\"2.0\" uid=\"%s\" type=\"%s\" time=\"%s\" start=\"%s\" stale=\"%s\" how=\"%s\">" +
                        "<point lat=\"%.7f\" lon=\"%.7f\" hae=\"%f\" ce=\"%f\" le=\"%f\"/><detail>%s</detail></event>",
                messageUid(), type, time.isoTimestamp(), start.isoTimestamp(), stale.isoTimestamp(), how, lat, lon, hae, ce, le, buildXmlDetail()
        ).toByteArray()
    }

    override fun toProtobuf(): ByteArray {
        val cotBytes = Takmessage.TakMessage.newBuilder()
                .setCotEvent(Cotevent.CotEvent.newBuilder()
                        .setType(type)
                        .setUid(messageUid())
                        .setHow(how)
                        .setSendTime(time.milliseconds())
                        .setStartTime(start.milliseconds())
                        .setStaleTime(stale.milliseconds())
                        .setLat(lat)
                        .setLon(lon)
                        .setHae(hae)
                        .setCe(ce)
                        .setLe(le)
                        .setDetail(buildProtobufDetail())
                        .build())
                .build()
                .toByteArray()
        return TAK_HEADER + cotBytes
    }

    private fun messageUid() = "$uid-9-1-1"

    private fun buildProtobufDetail(): DetailOuterClass.Detail {
        val builder = DetailOuterClass.Detail.newBuilder().setPrecisionLocation(
                Precisionlocation.PrecisionLocation.newBuilder()
                        .setAltsrc("???")
                        .setGeopointsrc("???")
                        .build()
        )
        builder.xmlDetail = if (emergencyType == EmergencyType.CANCEL) {
            "<emergency cancel=\"true\">$callsign</emergency>"
        } else {
            "<link relation=\"p-p\" type=\"a-f-G-U-C\" uid=\"$uid\"/><contact callsign=\"$callsign-Alert\"/>" +
                    "<emergency type=\"${emergencyType.description}\">$callsign</emergency>"
        }
        return builder.build()
    }

    private fun buildXmlDetail(): String {
        return if (emergencyType == EmergencyType.CANCEL) {
            "<emergency cancel=\"true\">$callsign</emergency><precisionlocation geopointsrc=\"???\" altsrc=\"???\"/>"
        } else {
            "<link relation=\"p-p\" type=\"a-f-G-U-C\" uid=\"$uid\"/><contact callsign=\"$callsign-Alert\"/>" +
                    "<emergency type=\"${emergencyType.description}\">$callsign</emergency><precisionlocation geopointsrc=\"???\" altsrc=\"???\"/>"
        }
    }
}