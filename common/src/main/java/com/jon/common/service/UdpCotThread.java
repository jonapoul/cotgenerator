package com.jon.common.service;

import android.content.SharedPreferences;

import com.jon.common.cot.CursorOnTarget;
import com.jon.common.utils.Key;
import com.jon.common.utils.NetworkHelper;
import com.jon.common.utils.PrefUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

class UdpCotThread extends CotThread {
    private List<DatagramSocket> sockets = new ArrayList<>();
    private List<String> interfaceNames = new ArrayList<>();

    UdpCotThread(SharedPreferences prefs) {
        super(prefs);
    }

    @Override
    void shutdown() {
        super.shutdown();
        for (DatagramSocket socket : sockets) {
            safeClose(socket);
        }
        sockets.clear();
        interfaceNames.clear();
    }

    @Override
    public void run() {
        try {
            super.run();
            initialiseDestAddress();
            openSockets();
            int bufferTimeMs = periodMilliseconds() / cotIcons.size();

            while (isRunning) {
                for (CursorOnTarget cot : cotIcons) {
                    if (!isRunning) break;
                    sendToDestination(cot);
                    bufferSleep(bufferTimeMs);
                }
                cotIcons = cotFactory.generate();
            }
        } catch (Exception e) {
            /* We've encountered an unexpected exception, so close all sockets and pass the message back to our
             * thread exception handler */
            Timber.e(e);
            throw new RuntimeException(e.getMessage());
        } finally {
            shutdown();
        }
    }

    @Override
    protected void initialiseDestAddress() throws UnknownHostException {
        destIp = InetAddress.getByName(PrefUtils.getString(prefs, Key.DEST_ADDRESS));
        destPort = PrefUtils.parseInt(prefs, Key.DEST_PORT);
    }

    @Override
    protected void openSockets() throws IOException {
        if (destIp.isMulticastAddress()) {
            final List<NetworkInterface> interfaces = NetworkHelper.getValidInterfaces();
            for (NetworkInterface ni : interfaces) {
                InetAddress address = NetworkHelper.getAddressFromInterface(ni);
                if (address != null) {
                    Timber.i("Interface %s is valid with address %s", ni.getName(), address.getHostAddress());
                    MulticastSocket socket = new MulticastSocket();
                    socket.setNetworkInterface(ni);
                    socket.setLoopbackMode(false);
                    sockets.add(socket);
                    interfaceNames.add(ni.getName());
                }
            }
        } else {
            sockets.add(new DatagramSocket());
            interfaceNames.add("all interfaces");
        }
    }

    protected void sendToDestination(CursorOnTarget cot) throws IOException {
        try {
            final byte[] buf = cot.toBytes(dataFormat);
            for (int i = 0; i < sockets.size(); i++) {
                sockets.get(i).send(new DatagramPacket(buf, buf.length, destIp, destPort));
                Timber.i("Sent %s over %s", cot.callsign, interfaceNames.get(i));
            }
        } catch (NullPointerException e) {
            /* Thrown when the thread is cancelled from another thread and we try to access the sockets */
            shutdown();
        }
    }
}
