package com.jonapoul.cotbeacon.cot

import android.annotation.SuppressLint
import com.jonapoul.common.cot.CursorOnTarget
import com.jonapoul.common.cot.UtcTimestamp
import com.jonapoul.common.cot.proto.Cotevent
import com.jonapoul.common.cot.proto.DetailOuterClass
import com.jonapoul.common.cot.proto.Takmessage
import com.jonapoul.common.di.IBuildResources
import com.jonapoul.common.presets.OutputPreset
import com.jonapoul.common.utils.NetworkUtils
import com.jonapoul.common.utils.Protocol
import java.util.*
import java.util.concurrent.TimeUnit

class ChatCursorOnTarget(
        val isIncoming: Boolean,
        private val buildResources: IBuildResources? = null
) : CursorOnTarget(buildResources) {

    var message: String = ""
    var messageUid: String = ""
    var outputPreset: OutputPreset? = null

    init {
        type = "b-t-f"
        how = "h-g-i-g-o"
        ce = 0.0
        le = 9999999.0
        hae = 0.0

        val now = UtcTimestamp.now()
        start = now
        time = now
        stale = start.add(1, TimeUnit.DAYS)
    }

    override fun toXml(): ByteArray {
        return String.format(Locale.ENGLISH,
                "<event version=\"2.0\" uid=\"%s\" type=\"%s\" time=\"%s\" start=\"%s\" stale=\"%s\" how=\"%s\">" +
                        "<point lat=\"%.7f\" lon=\"%.7f\" hae=\"%f\" ce=\"%f\" le=\"%f\"/><detail>%s</detail></event>",
                buildMessageUid(), type, time.isoTimestamp(), start.isoTimestamp(),
                stale.isoTimestamp(), how, lat, lon, hae, ce, le, buildXmlDetail()
        ).toByteArray()
    }

    override fun toProtobuf(): ByteArray {
        val cotBytes = Takmessage.TakMessage.newBuilder()
                .setCotEvent(Cotevent.CotEvent.newBuilder()
                        .setType(type)
                        .setUid(buildMessageUid())
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
                                .setXmlDetail(buildXmlDetail())
                                .build())
                        .build())
                .build()
                .toByteArray()
        return TAK_HEADER + cotBytes
    }

    private fun buildXmlDetail(): String {
        val serverDestination = getServerDestinationIfRequired()
        return ("<__chat%s id=\"All Chat Rooms\" chatroom=\"All Chat Rooms\" senderCallsign=\"%s\" groupOwner=\"false\"><chatgrp " +
                "id=\"All Chat Rooms\" uid0=\"%s\" uid1=\"All Chat Rooms\"/></__chat><link uid=\"%s\" type=\"a-f-G-U-C\" relation=\"p-p\"/>" +
                "<remarks source=\"BAO.F.%s.%s\"%s to=\"All Chat Rooms\" time=\"%s\">%s</remarks>%s")
                .format(parent(), callsign, uid, uid, sanitisePlatform(), uid, sourceId(), time.isoTimestamp(), message, serverDestination)
    }

    private fun parent(): String {
        return if (outputPreset?.protocol == Protocol.SSL) {
            " parent=\"RootContactGroup\""
        } else ""
    }

    private fun sourceId(): String {
        return if (outputPreset?.protocol == Protocol.SSL) {
            ""
        } else {
            " sourceID=\"$uid\""
        }
    }

    private fun sanitisePlatform(): String {
        /* Remove any non-alphabetic (is that a word?) characters */
        return buildResources?.platform?.replace("[^a-zA-Z]".toRegex(), "")
                ?: "PLATFORM"
    }

    private fun buildMessageUid(): String {
        return "GeoChat.$uid.All Chat Rooms.$messageUid"
    }

    @SuppressLint("DefaultLocale")
    private fun getServerDestinationIfRequired(): String {
        return if (outputPreset?.protocol != Protocol.UDP) {
            val address = NetworkUtils.getLocalAddress().hostAddress
            "<__serverdestination destinations=\"$address:4242:tcp:$uid\"/>"
        } else {
            ""
        }
    }

    companion object {
        fun fromBytes(bytes: ByteArray): ChatCursorOnTarget? {
            return try {
                if (isProtobuf(bytes)) {
                    fromProtobuf(bytes)
                } else {
                    fromXml(bytes)
                }
            } catch (e: Exception) {
                null
            }
        }

        private fun fromProtobuf(bytes: ByteArray): ChatCursorOnTarget {
            val trimmedBytes = bytes.drop(3).toByteArray()
            val takMessage = Takmessage.TakMessage.parseFrom(trimmedBytes)
            val cot = ChatCursorOnTarget(isIncoming = true)
            val event = takMessage.cotEvent
            cot.type = event.type
            cot.uid = event.uid
            cot.how = event.how
            cot.time = UtcTimestamp(event.sendTime)
            cot.start = UtcTimestamp(event.startTime)
            cot.stale = UtcTimestamp(event.staleTime)
            cot.lat = event.lat
            cot.lon = event.lon
            cot.hae = event.hae
            cot.ce = event.ce
            cot.le = event.le
            XmlParser.parseXmlDetail(event.detail.xmlDetail, cot)
            return cot
        }

        private fun fromXml(bytes: ByteArray): ChatCursorOnTarget {
            return XmlParser.parseChat(bytes)
        }

        private fun isProtobuf(bytes: ByteArray): Boolean {
            if (bytes.size < 3) return false
            return bytes[0] == MAGIC_BYTE && bytes[2] == MAGIC_BYTE
        }
    }
}