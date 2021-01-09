package com.jonapoul.common.utils

import android.content.Context
import androidx.annotation.RawRes
import java.io.File

object FileUtils {
    fun toByteArray(filepath: String): ByteArray {
        return File(filepath).readBytes()
    }

    fun rawResourceToByteArray(context: Context, @RawRes resId: Int): ByteArray {
        val inputStream = context.resources.openRawResource(resId)
        return inputStream.readBytes()
    }
}
