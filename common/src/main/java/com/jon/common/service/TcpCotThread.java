package com.jon.common.service;

import android.content.SharedPreferences;

import com.jon.common.cot.CursorOnTarget;
import com.jon.common.utils.Constants;
import com.jon.common.utils.DataFormat;
import com.jon.common.utils.Key;
import com.jon.common.utils.PrefUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

class TcpCotThread extends CotThread {
    private List<CotConnection> cotConnections = new ArrayList<>();
    private ExecutorService takProtocolNegotiationThreads;
    protected List<Socket> sockets = new ArrayList<>();
    protected boolean emulateMultipleUsers;

    TcpCotThread(SharedPreferences prefs) {
        super(prefs);
        emulateMultipleUsers = PrefUtils.getBoolean(prefs, Key.EMULATE_MULTIPLE_USERS);
    }

    @Override
    void shutdown() {
        super.shutdown();
        if (dataFormat == DataFormat.PROTOBUF) {
            Timber.i("Shutting down listening executor service");
            stopListeningForTakProtocol();
        }
        Timber.d("Shutting down output streams");
        for (CotConnection connection : cotConnections) {
            safeClose(connection.outStream);
            safeClose(connection.inStream);
        }
        Timber.d("Shutting down sockets");
        for (Socket socket : sockets)
            safeClose(socket);
        sockets.clear();
        cotConnections.clear();
    }

    @Override
    public void run() {
        try {
            super.run();
            initialiseDestAddress();
            openSockets();
            initialiseCotConnections();
            int bufferTimeMs = periodMilliseconds() / cotIcons.size();

            if (dataFormat == DataFormat.PROTOBUF) {
                startListeningForTakProtocol();
            }

            while (isRunning) {
                for (CotConnection cotStream : cotConnections) {
                    if (!isRunning) break;
                    sendToDestination(cotStream);
                    bufferSleep(bufferTimeMs);
                }
                updateCotConnections();
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

    protected void sendToDestination(CotConnection connection) throws IOException {
        if (!connection.canTransmit) {
            Timber.w("Skipping transmission, waiting for TAK protocol negotiation");
            return;
        }
        try {
            for (CursorOnTarget cot : connection.cots) {
                final byte[] cotBytes = cot.toBytes(connection.dataFormat);
                connection.outStream.write(cotBytes);
                Timber.i("Sent %s as %s", cot.callsign, connection.dataFormat.get());
            }
        } catch (SocketException | NullPointerException e) {
            /* Thrown when the thread is cancelled from another thread and we try to access the sockets */
            shutdown();
        }
    }

    @Override
    protected void initialiseDestAddress() throws UnknownHostException {
        Timber.d("Initialising destination address/port");
        destIp = InetAddress.getByName(PrefUtils.getString(prefs, Key.DEST_ADDRESS));
        destPort = PrefUtils.parseInt(prefs, Key.DEST_PORT);
    }

    @Override
    protected void openSockets() throws Exception {
        sockets.clear();
        Timber.d("Opening sockets");
        final int numSockets = emulateMultipleUsers ? cotIcons.size() : 1;

        /* Thread-safe list */
        final List<Socket> synchronisedSockets = Collections.synchronizedList(new ArrayList<>());
        final ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i = 0; i < numSockets; i++) {
            /* Execute the socket-building code on a separate thread per socket, since it takes a while with lots of sockets */
            executorService.execute(() -> {
                try {
                    buildSocket(synchronisedSockets);
                } catch (IOException e) {
                    Timber.e(e);
                }
            });
        }
        collectSynchronisedSockets(executorService, synchronisedSockets);
    }

    protected void collectSynchronisedSockets(ExecutorService executorService, List<Socket> synchronisedSockets)
            throws InterruptedException, IOException {
        try {
            /* Block the thread until all sockets are built */
            executorService.shutdown();
            while (!executorService.awaitTermination(1, TimeUnit.MINUTES));
            sockets = synchronisedSockets;
            Timber.d("All sockets open!");
        } catch (InterruptedException e) {
            /* Thrown if we stop the service whilst sockets are being built */
            executorService.shutdownNow();
            Timber.w(e);
            while (!executorService.awaitTermination(10, TimeUnit.SECONDS));
            for (Socket socket : synchronisedSockets) socket.close();
            shutdown();
        }
    }

    private void buildSocket(List<Socket> synchronisedSockets) throws IOException {
        Timber.d("Opening socket");
        Socket socket = new Socket();
        socket.connect(
                new InetSocketAddress(destIp, destPort),
                Constants.TCP_SOCKET_TIMEOUT_MILLISECONDS
        );
        Timber.d("Opened socket on port %d", socket.getLocalPort());
        synchronisedSockets.add(socket);
    }

    private void initialiseCotConnections() throws IOException {
        Timber.d("Initialising CoT connections");
        cotConnections.clear();
        try {
            if (emulateMultipleUsers) {
                /* One CotConnection per icon */
                for (int i = 0; i < cotIcons.size(); i++) {
                    final CotConnection cotConnection = new CotConnection();
                    cotConnection.cots.add(cotIcons.get(i));
                    final Socket socket = sockets.get(i);
                    cotConnection.outStream = socket.getOutputStream();
                    cotConnection.inStream = socket.getInputStream();
                    cotConnections.add(cotConnection);
                }
            } else {
                /* One CotConnection to hold all icons */
                final CotConnection cotConnection = new CotConnection();
                cotConnection.cots.addAll(cotIcons);
                final Socket socket = sockets.get(0);
                cotConnection.outStream = socket.getOutputStream();
                cotConnection.inStream = socket.getInputStream();
                cotConnections.add(cotConnection);
            }
        } catch (SocketException | IndexOutOfBoundsException e) {
            /* Thrown if the "sockets" instance has been cleared/shutdown before we get to this point */
            shutdown();
        }
    }

    private void updateCotConnections() {
        Timber.d("Updating %d CoT connections", cotConnections.size());
        cotIcons = cotFactory.generate();
        for (int i = 0; i < cotConnections.size(); i++) {
            CotConnection connection = cotConnections.get(i);
            connection.cots.clear();
            if (emulateMultipleUsers) {
                connection.cots.add(cotIcons.get(i));
            } else {
                connection.cots.addAll(cotIcons);
            }
        }
    }

    private void startListeningForTakProtocol() {
        takProtocolNegotiationThreads = Executors.newCachedThreadPool();
        for (final CotConnection connection : cotConnections) {
            takProtocolNegotiationThreads.execute(connection.negotiateTakProtocol());
        }
    }

    private void stopListeningForTakProtocol() {
        takProtocolNegotiationThreads.shutdownNow();
    }
}
