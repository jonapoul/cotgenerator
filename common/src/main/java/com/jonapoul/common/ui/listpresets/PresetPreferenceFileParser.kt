package com.jonapoul.common.ui.listpresets

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jonapoul.common.presets.OutputPreset
import com.jonapoul.common.utils.Protocol
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

internal class PresetPreferenceFileParser(
        private val context: Context,
        private val path: String?
) {
    fun parse(callback: (OutputPreset?) -> Unit) {
        Timber.d("parse %s", path)
        if (path == null) {
            callback(null)
            return
        }
        val file = File(path)
        when (file.extension) {
            "zip" -> parseZipFile(file, callback)
            "pref" -> parsePrefFile(file, callback)
            else -> callback(null)
        }
    }

    private fun parseZipFile(file: File, callback: (OutputPreset?) -> Unit) {
        Timber.d("parseZipFile")
        try {
            ZipFile(file).use { zipFile ->
                val filenames = zipFile.entries()
                        .asSequence()
                        .map { it.name }
                        .filter { it.endsWith(".pref") }
                        .toList()

                Timber.d("filenames = %s", filenames)

                val presets = arrayListOf<OutputPreset>()
                filenames.forEach {
                    val inputStream = zipFile.getInputStream(zipFile.getEntry(it))
                    val xmlString = inputStreamToString(inputStream)
                    presets.addAll(getPresetsFromXml(xmlString))
                }
                callbackDependingOnPresetCount(presets, callback)
            }
        } catch (e: Exception) {
            Timber.e(e)
            callback(null)
        }
    }

    private fun parsePrefFile(file: File, callback: (OutputPreset?) -> Unit) {
        Timber.d("parsePrefFile")
        try {
            val xmlString = concatXmlLines(file)
            val presets = getPresetsFromXml(xmlString)
            callbackDependingOnPresetCount(presets, callback)
        } catch (e: Exception) {
            Timber.e(e)
            callback(null)
        }
    }

    private fun getPresetsFromXml(xmlString: String): List<OutputPreset> {
        Timber.d("getPresetsFromXml")
        val presets = arrayListOf<OutputPreset>()
        val cotStreams = COT_STREAMS_PATTERN.find(xmlString)!!.groupValues[1]
        val count = COUNT_PATTEN.find(cotStreams)!!.groupValues[1].toInt()
        for (i in 0 until count) {
            var alias: String? = null
            var address: String? = null
            var port: Int? = null
            var protocol: Protocol? = null
            aliasPattern(i).find(cotStreams)?.let {
                alias = it.groupValues[1]
            }
            connectStringPattern(i).find(cotStreams)?.let {
                val split = it.groupValues[1].split(':')
                address = split[0]
                port = split[1].toInt()
                protocol = Protocol.fromString(split[2])
            }
            if (protocol != null && alias != null && address != null && port != null) {
                presets.add(OutputPreset(protocol!!, alias!!, address!!, port!!))
            }
        }
        return presets
    }

    private fun inputStreamToString(inputStream: InputStream): String {
        Timber.d("inputStreamToString")
        BufferedReader(inputStream.reader()).use {
            val content = it.readText()
            return content.replace("\n", "").replace("\t", "")
        }
    }

    private fun concatXmlLines(file: File): String {
        Timber.d("concatXmlLines")
        val builder = StringBuilder()
        file.forEachLine { builder.append(it) }
        return builder.toString()
    }

    @Suppress("CascadeIf")
    private fun callbackDependingOnPresetCount(presets: List<OutputPreset>, callback: (OutputPreset?) -> Unit) {
        Timber.d("callbackDependingOnPresetCount %d", presets.size)
        if (presets.isEmpty()) {
            /* None found, so pass back a null value */
            callback(null)
        } else if (presets.size == 1) {
            /* Just the one, so pass this back to the activity to do its work */
            callback(presets[0])
        } else {
            /* There are more than one, so ask the user which one they want to import */
            val presetStrings = presets.map { it.alias }.toTypedArray()
            MaterialAlertDialogBuilder(context)
                    .setTitle("Import Preset")
                    .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                        callback(null)
                    }.setSingleChoiceItems(presetStrings, NO_ITEMS_CHECKED) { dialog, index ->
                        dialog.dismiss()
                        callback(presets[index])
                    }
                    .show()
        }
    }

    private fun aliasPattern(index: Int): Regex {
        return Regex("<entry key=\"description${index}\" class=\"class java.lang.String\">(.*?)</entry>")
    }

    private fun connectStringPattern(index: Int): Regex {
        return Regex("<entry key=\"connectString${index}\" class=\"class java.lang.String\">(.*?)</entry>")
    }

    private companion object {
        const val NO_ITEMS_CHECKED = -1
        val COT_STREAMS_PATTERN = Regex("<preference version=\"1\" name=\"cot_streams\">(.*?)</preference>")
        val COUNT_PATTEN = Regex("<entry key=\"count\" class=\"class java.lang.Integer\">(\\d+)</entry>")
    }
}
