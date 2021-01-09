package com.jonapoul.common.service

import java.io.OutputStream

class SynchronisedOutputStream(private val outputStream: OutputStream) : OutputStream() {
    private val lock = Any()

    override fun write(b: Int) {
        synchronized(lock) {
            outputStream.write(b)
        }
    }

    override fun write(bytes: ByteArray?) {
        synchronized(lock) {
            outputStream.write(bytes)
        }
    }

    override fun flush() {
        synchronized(lock) {
            outputStream.flush()
        }
    }

    override fun write(b: ByteArray?, off: Int, len: Int) {
        synchronized(lock) {
            outputStream.write(b, off, len)
        }
    }

    override fun close() {
        synchronized(lock) {
            outputStream.close()
        }
    }
}
