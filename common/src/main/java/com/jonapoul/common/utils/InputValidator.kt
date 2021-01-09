package com.jonapoul.common.utils

import android.os.AsyncTask
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.regex.Pattern

class InputValidator {
    fun validateInt(str: String, min: Int? = null, max: Int? = null): Boolean {
        return try {
            isInRange(str.toInt(), min, max)
        } catch (e: NumberFormatException) {
            false
        }
    }

    fun validateDouble(str: String, min: Double? = null, max: Double? = null): Boolean {
        return try {
            isInRange(str.toDouble(), min, max)
        } catch (e: NumberFormatException) {
            false
        }
    }

    fun validateString(str: String?): Boolean {
        return str != null && str.isNotEmpty()
    }

    fun validateString(str: String, regexPattern: String): Boolean {
        return if (!validateString(str)) false else Pattern.compile(regexPattern).matcher(str).find()
    }

    fun validateCallsign(callsign: String): Boolean {
        /* These characters break TAK parsing when using XML, but not protobuf. I'll block them from both just to be safe */
        val disallowedCharacters = arrayOf(
                '<', '>', '&', '\"'
        )
        return disallowedCharacters.none { callsign.contains(it) }
    }

    fun validateHostname(host: String): Boolean {
        return try {
            ValidateHostnameTask().execute(host).get()
        } catch (e: Exception) {
            false
        }
    }

    /* We don't care about the call to InetAddress.getByName() returning anything, all we want is to catch any exceptions from calling it */
    private class ValidateHostnameTask : AsyncTask<String, Void, Boolean>() {
        override fun doInBackground(vararg params: String): Boolean {
            return try {
                InetAddress.getByName(params[0])
                true
            } catch (e: UnknownHostException) {
                false
            }
        }
    }

    private fun <T : Comparable<T>> isInRange(value: T, min: T?, max: T?): Boolean {
        if (min != null && value < min) return false
        if (max != null && value > max) return false
        return true
    }
}
