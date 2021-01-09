package com.jonapoul.common.utils

import android.os.Environment
import java.io.File

@Suppress("DEPRECATION")
object Paths {
    val EXTERNAL_DIRECTORY: File = Environment.getExternalStorageDirectory()
    val ATAK_DIRECTORY = File(EXTERNAL_DIRECTORY, "atak")
}
