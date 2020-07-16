package com.jon.common.service;

import android.content.SharedPreferences;

import com.jon.common.AppSpecific;
import com.jon.common.cot.CursorOnTarget;
import com.jon.common.utils.DataFormat;
import com.jon.common.utils.Key;
import com.jon.common.utils.PrefUtils;
import com.jon.common.utils.Protocol;

import java.io.Closeable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import timber.log.Timber;

abstract class CotThread extends Thread {
    protected final SharedPreferences prefs;
    protected volatile boolean isRunning = false;
    protected CotFactory cotFactory;
    protected DataFormat dataFormat;
    protected InetAddress destIp;
    protected int destPort;
    protected List<CursorOnTarget> cotIcons;

    abstract protected void initialiseDestAddress() throws UnknownHostException;
    abstract protected void openSockets() throws Exception;

    static CotThread fromPrefs(SharedPreferences prefs) {
        Protocol protocol = Protocol.fromPrefs(prefs);
        switch (protocol) {
            case UDP: return new UdpCotThread(prefs);
            case TCP: return new TcpCotThread(prefs);
            case SSL: return new SslCotThread(prefs);
            default: throw new IllegalArgumentException("Unexpected protocol: " + protocol);
        }
    }

    protected CotThread(SharedPreferences sharedPrefs) {
        prefs = sharedPrefs;
        dataFormat = DataFormat.fromPrefs(prefs);
        cotFactory = AppSpecific.getCotFactory(prefs);
        cotIcons = cotFactory.generate();
    }

    boolean isRunning() {
        return isRunning;
    }

    @Override
    public void run() {
        isRunning = true;
    }

    void shutdown() {
        isRunning = false;
        if (cotFactory != null) {
            cotFactory.clear();
        }
        interrupt();
    }

    protected void bufferSleep(int bufferTimeMs) {
        try {
            Thread.sleep(bufferTimeMs);
        } catch (InterruptedException e) {
            /* do nothing */
        }
    }

    protected int periodMilliseconds() {
        return PrefUtils.getInt(prefs, Key.TRANSMISSION_PERIOD) * 1000;
    }

    protected void safeClose(Closeable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            Timber.w(e);
        }
    }
}
