package com.jon.common.cot;

import android.os.Build;

import com.jon.common.AppSpecific;
import com.jon.common.cot.proto.ContactOuterClass.Contact;
import com.jon.common.cot.proto.Cotevent.CotEvent;
import com.jon.common.cot.proto.DetailOuterClass.Detail;
import com.jon.common.cot.proto.GroupOuterClass.Group;
import com.jon.common.cot.proto.StatusOuterClass.Status;
import com.jon.common.cot.proto.Takmessage.TakMessage;
import com.jon.common.cot.proto.TakvOuterClass.Takv;
import com.jon.common.cot.proto.TrackOuterClass.Track;
import com.jon.common.ui.ArrayUtils;
import com.jon.common.utils.DataFormat;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class CursorOnTarget {
    // Prepended to every protobuf packet
    private static final byte[] TAK_HEADER = new byte[] { (byte)0xbf, (byte)0x01, (byte)0xbf };

    public CotHow how = CotHow.MG;
    public CotType type = CotType.GROUND_COMBAT;

    // User info
    public String uid = "UUID";    // unique ID of the device. Stays constant when changing callsign

    // Time info
    public UtcTimestamp time;    // time when the icon was created
    public UtcTimestamp start;   // time when the icon is considered valid
    public UtcTimestamp stale;   // time when the icon is considered invalid

    // Contact info
    public String callsign = "CALLSIGN"; // ATAK callsign

    // Position and movement info
    public double hae = 0.0;    // height above ellipsoid in metres
    public double lat = 0.0;    // latitude in decimal degrees
    public double lon = 0.0;    // longitude in decimal degrees
    public double ce = 0.0;     // circular (radial) error in metres. applies to 2D position only
    public double le = 0.0;     // linear error in metres. applies to altitude only
    public double course = 0.0; // ground bearing in decimal degrees
    public double speed = 0.0;  // ground velocity in m/s. Doesn't include altitude climb rate

    // Group
    public CotTeam team = CotTeam.CYAN;  // cyan, green, purple, etc
    public CotRole role = CotRole.TEAM_MEMBER;  // HQ, sniper, K9, etc

    // Location source
    public String altsrc = "GENERATED";
    public String geosrc = "GENERATED";

    // System info
    public Integer battery = 100; // internal battery charge percentage, scale of 1-100
    public final String device = getDeviceName(); // Android device model
    public final String platform = AppSpecific.getPlatform(); // application name
    public final String os = String.valueOf(Build.VERSION.SDK_INT); // Android SDK version number
    public final String version = AppSpecific.getVersionName(); // application version number

    public CursorOnTarget() {
        time = start = UtcTimestamp.now();
        stale = start.add(10, TimeUnit.MINUTES);
    }

    /* Sent to a TAK Server to request protobuf communication. See protocol.txt in the ATAK-CIV repo for more details */
    public static byte[] takRequest(String uid) {
        final UtcTimestamp time = UtcTimestamp.now();
        return String.format(Locale.ENGLISH,
                "<event version='2.0' uid=\"protouid\" type=\"t-x-takp-q\" time=\"%s\" start=\"%s\" stale=\"%s\" how=\"%s\">" +
                        "<point lat=\"0.0\" lon=\"0.0\" hae=\"0.0\" ce=\"999999\" le=\"999999\"/><detail>" +
                        "<TakControl><TakRequest version=\"1\"/></TakControl></detail></event>",
                time.toString(), time.toString(), time.add(1, TimeUnit.MINUTES).toString(), CotHow.MG.get())
                .getBytes();
    }

    public byte[] toBytes(DataFormat dataFormat) {
        switch (dataFormat) {
            case XML:      return toXml();
            case PROTOBUF: return toProtobuf();
            default:       throw new IllegalArgumentException("Unknown data format: " + dataFormat);
        }
    }

    public void setStaleDiff(final long dt, final TimeUnit timeUnit) {
        stale = start.add(dt, timeUnit);
    }

    private byte[] toXml() {
        return String.format(Locale.ENGLISH,
                "<event version=\"2.0\" uid=\"%s\" type=\"%s\" time=\"%s\" start=\"%s\" stale=\"%s\" how=\"%s\"><point lat=\"%.7f\" " +
                        "lon=\"%.7f\" hae=\"%f\" ce=\"%f\" le=\"%f\"/><detail><track speed=\"%.7f\" course=\"%.7f\"/><contact callsign=\"%s\"/>" +
                        "<__group name=\"%s\" role=\"%s\"/><takv device=\"%s\" platform=\"%s\" os=\"%s\" version=\"%s\"/><status battery=\"%d\"/>" +
                        "<precisionlocation altsrc=\"%s\" geopointsrc=\"%s\" /></detail></event>",
                uid, type.get(), time.toString(), start.toString(), stale.toString(), how.get(), lat, lon, hae, ce, le, speed,
                course, callsign, team.get(), role.get(), device, platform, os, version, battery, altsrc, geosrc)
                .getBytes();
    }

    private byte[] toProtobuf() {
        byte[] cotBytes = TakMessage.newBuilder()
                .setCotEvent(CotEvent.newBuilder()
                        .setType(type.get())
                        .setUid(uid)
                        .setHow(how.get())
                        .setSendTime(time.toLong())
                        .setStartTime(start.toLong())
                        .setStaleTime(stale.toLong())
                        .setLat(lat)
                        .setLon(lon)
                        .setHae(hae)
                        .setCe(ce)
                        .setLe(le)
                        .setDetail(Detail.newBuilder()
                                .setGroup(Group.newBuilder()
                                        .setName(team.get())
                                        .setRole(role.get())
                                        .build())
                                .setTakv(Takv.newBuilder()
                                        .setDevice(device)
                                        .setPlatform(platform)
                                        .setOs(os)
                                        .setVersion(version)
                                        .build())
                                .setStatus(Status.newBuilder()
                                        .setBattery(battery)
                                        .build())
                                .setTrack(Track.newBuilder()
                                        .setCourse(course)
                                        .setSpeed(speed)
                                        .build())
                                .setContact(Contact.newBuilder()
                                        .setCallsign(callsign)
                                        .build())
                                .build())
                        .build())
                .build()
                .toByteArray();
        return ArrayUtils.concatBytes(TAK_HEADER, cotBytes);
    }

    private String getDeviceName() {
        try {
            return String.format("%s %s", Build.MANUFACTURER.toUpperCase(), Build.MODEL.toUpperCase());
        } catch (NullPointerException e) {
            /* Thrown in unit tests */
            return "DEVICE";
        }
    }
}
