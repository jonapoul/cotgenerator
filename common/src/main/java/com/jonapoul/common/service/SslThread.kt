package com.jonapoul.common.service

import android.content.SharedPreferences
import android.os.AsyncTask
import com.jonapoul.common.cot.CursorOnTarget
import timber.log.Timber
import java.io.Closeable
import java.io.IOException
import java.net.SocketException
import java.util.concurrent.ExecutionException

internal class SslThread(
        prefs: SharedPreferences,
) : TcpThread(prefs) {

    override fun shutdown() {
        super.shutdown()
        Timber.i("shutdown")
        try {
            closeFromMainThread(socket)
            closeFromMainThread(outputStream)
        } catch (ignored : UninitializedPropertyAccessException) { }
        socketRepository.clearSockets()
    }

    @Throws(IOException::class)
    override fun sendToDestination(cot: CursorOnTarget) {
        Timber.i("sendToDestination")
        try {
            outputStream.write(cot.toBytes(dataFormat))
            Timber.i("Sent cot: %s to %d from %d", cot.callsign, socket.port, socket.localPort)
        } catch (e: NullPointerException) {
            /* Thrown when the thread is cancelled from another thread and we try to access the sockets */
            shutdown()
        } catch (e: SocketException) {
            shutdown()
        }
    }

    @Throws(Exception::class)
    override fun openSockets() {
        socket = socketRepository.getSslSocket()
        outputStream = socketRepository.getOutputStream(socket)
    }


    /* This workaround is only necessary because I was getting NetworkOnMainThreadExceptions when
     * closing the SSLSocket via shutdown() from the UI thread. This wasn't an issue with TCP/UDP
     * so this is just a patch to deal with it for now. */
    private fun closeFromMainThread(closeable: Closeable?) {
        Timber.i("closeFromMainThread %s", closeable)
        try {
            CloseSocketTask().execute(closeable).get()
        } catch (e: ExecutionException) {
            /* Ignore */
        } catch (e: InterruptedException) {
            /* Ignore */
        }
    }

    private class CloseSocketTask : AsyncTask<Closeable?, Void, Void?>() {
        override fun doInBackground(vararg closeables: Closeable?): Void? {
            for (closeable in closeables) {
                try {
                    closeable?.close()
                } catch (e: IOException) {
                    Timber.e(e)
                }
            }
            return null
        }
    }
}