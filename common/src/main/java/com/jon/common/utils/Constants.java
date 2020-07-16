package com.jon.common.utils;

public final class Constants {
    private Constants() { }

    public static final double EARTH_RADIUS = 6371000; // metres

    public static final double DEG_TO_RAD = Math.PI / 180.0;
    public static final double RAD_TO_DEG = 1.0 / DEG_TO_RAD;
    public static final double MPH_TO_METRES_PER_SECOND = 0.44704;
    public static final double METRES_PER_SECOND_TO_MPH = 1.0 / MPH_TO_METRES_PER_SECOND;

    public static final int TCP_SOCKET_TIMEOUT_MILLISECONDS = 10000;
}
