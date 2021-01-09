package com.jonapoul.common.repositories.impl

import android.content.Context
import com.jonapoul.common.repositories.IDeviceUidRepository
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import javax.inject.Inject

class DeviceUidRepository @Inject constructor(context: Context) : IDeviceUidRepository {
    private val uid = generate(context)

    override fun getUid() = uid

    private fun generate(context: Context): String {
        var generatedUid: String
        try {
            val file = File(context.filesDir, FILENAME)
            if (file.exists()) {
                Timber.i("Reading UID from file...")
                generatedUid = readPreviousUid(context)
                Timber.i("Successfully read UID %s", generatedUid)
            } else {
                Timber.i("Writing new UID to file...")
                generatedUid = createAndWriteNewUid(context)
                Timber.i("Successfully written UID %s", generatedUid)
            }
        } catch (e: IOException) {
            generatedUid = UUID.randomUUID().toString()
            Timber.e(e)
            Timber.e("Failed to read/write UID from/to file. Using a temporary one instead: %s", generatedUid)
        }
        return generatedUid
    }

    @Throws(IOException::class)
    private fun readPreviousUid(context: Context): String {
        val inputStream = context.openFileInput(FILENAME)
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        val line = bufferedReader.readLine()
        inputStream.close()
        return line
    }

    @Throws(IOException::class)
    private fun createAndWriteNewUid(context: Context): String {
        val uuid = UUID.randomUUID().toString()
        val fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE)
        fos.write(uuid.toByteArray())
        fos.close()
        return uuid
    }

    companion object {
        private const val FILENAME = "uuid.txt"
    }
}
