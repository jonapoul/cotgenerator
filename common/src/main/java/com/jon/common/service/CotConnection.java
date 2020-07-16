package com.jon.common.service;

import com.jon.common.cot.CursorOnTarget;
import com.jon.common.utils.DataFormat;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/* Simple container class for a CoT icon and some relevant objects */
class CotConnection {
    private static final long TIMEOUT_MS = TimeUnit.MINUTES.toMillis(1);
    private BufferedReader bufferedReader;

    List<CursorOnTarget> cots = new ArrayList<>();
    OutputStream outStream;
    InputStream inStream;

    /* Data format is XML by default because we don't know whether the server supports protobuf yet.
       So if the user has requested protobuf output, we:
          a) wait for a packet containing <TakControl><TakProtocolSupport version="1"/></TakControl>
          b) send a packet containing <TakControl><TakRequest version="1"/></TakControl>
          c) block any XML transmissions until a response is received from the server
          d) wait for a response containing <TakControl><TakResponse status="true"/></TakControl>
          e) Set dataFormat to DataFormat.PROTOBUF
     */
    DataFormat dataFormat = DataFormat.XML;
    boolean canTransmit = true;


    /* Task passed to an ExecutorService to be started when opening our sockets. This listens for the server's announcement of
     * protobuf support, then engages in the TAK Protocol conversation until we get a 'TakResponse status="true"'. At this point
     * we flick all future transmissions from this socket to be in protobuf format. */
    Runnable negotiateTakProtocol() {
        return () -> {
            /* Listen for initial contact from the server */
            bufferedReader = new BufferedReader(new InputStreamReader(inStream));
            String conversationUid = null;
            long start = System.currentTimeMillis();
            while (conversationUid == null && !timedOut(start)) {
                conversationUid = waitForTakProtocolSupport();
                if (Thread.currentThread().isInterrupted()) {
                    Timber.w("Interrupted, backing out");
                    return;
                }
            }
            if (conversationUid == null) {
                Timber.w("Failed initiating negotiation with server");
                return;
            }

            /* Received a packet, so block regular transmissions and send a response requesting protobuf usage */
            canTransmit = false;
            if (!sendTakRequest(conversationUid)) {
                Timber.w("Failed to send TakRequest");
                canTransmit = true;
                return;
            }

            if (Thread.currentThread().isInterrupted()) {
                Timber.w("Interrupted, backing out");
                canTransmit = true;
                return;
            }

            start = System.currentTimeMillis();
            while (!Thread.currentThread().isInterrupted()) {
                Boolean takResponse = waitForTakResponse(conversationUid);
                if (takResponse == null) {
                    if (timedOut(start)) {
                        Timber.w("Timed out waiting for TakResponse");
                        break;
                    }
                } else {
                    Timber.w("TakResponse = %s", takResponse);
                    dataFormat = takResponse ? DataFormat.PROTOBUF : DataFormat.XML;
                    break;
                }
            }
            canTransmit = true;
            Timber.i("Finishing requestTakProtocol, dataFormat = %s", dataFormat.get());
        };
    }

    private boolean timedOut(long start) {
        return System.currentTimeMillis() - start > TIMEOUT_MS;
    }

    private @Nullable String waitForTakProtocolSupport() {
        try {
            Timber.i("Waiting for TakProtocolSupport");
            final String xml = bufferedReader.readLine();
            if (xml.contains("<TakControl><TakProtocolSupport version=\"1\"/></TakControl>")) {
                Matcher matcher = Pattern.compile("uid=\"(.*?)\"").matcher(xml);
                if (matcher.find()) {
                    Timber.i("Found TakProtocolSupport message");
                    return matcher.group(1);
                } else {
                    Timber.w("Couldn't parse TakProtocolSupport message");
                }
            } else {
                Timber.w("Not a TakProtocolSupport message");
            }
        } catch (SocketException e) {
            Timber.w("Timed out when waiting for TakProtocolSupport");
        } catch (IOException e) {
            Timber.w(e);
        }
        return null;
    }

    private boolean sendTakRequest(String conversationUid) {
        try {
            Timber.i("Sending TakRequest");
            outStream.write(CursorOnTarget.takRequest(conversationUid));
            Timber.i("Successfully sent TakRequest");
            return true;
        } catch (IOException e) {
            Timber.w(e);
            return false;
        }
    }

    private Boolean waitForTakResponse(String conversationUid) {
        try {
            Timber.i("Waiting for TakResponse");
            final String xml = bufferedReader.readLine();
            if (xml.contains("<TakControl><TakResponse status=")) {
                Matcher matcher = Pattern.compile("<TakResponse status=\"(.*?)\"").matcher(xml);
                if (matcher.find()) {
                    Timber.i("Found %s", matcher.group(1));
                    return Boolean.parseBoolean(matcher.group(1));
                } else {
                    Timber.w("Couldn't parse TakResponse");
                }
            } else {
                Timber.w("Not a TakResponse message");
            }
        } catch (SocketTimeoutException e) {
            Timber.w("Timed out waiting for TakResponse");
        } catch (Exception e) {
            Timber.w(e);
        }
        return null;
    }
}
