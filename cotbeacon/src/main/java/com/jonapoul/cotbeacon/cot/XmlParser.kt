package com.jonapoul.cotbeacon.cot

import com.jonapoul.common.cot.UtcTimestamp

internal object XmlParser {
    fun parseChat(bytes: ByteArray): ChatCursorOnTarget {
        val cot = ChatCursorOnTarget(isIncoming = true)
        val xml = setQuotes(String(bytes))
        cot.start = UtcTimestamp(
                getAttribute(
                        xml = xml,
                        element = "event",
                        attribute = "start"
                )
        )
        parseXmlDetail(xml, cot)
        return cot
    }

    fun parseXmlDetail(xml: String, cot: ChatCursorOnTarget) {
        cot.callsign = getAttribute(
                xml = xml,
                element = "__chat",
                attribute = "senderCallsign"
        )
        cot.uid = getAttribute(
                xml = xml,
                element = "chatgrp",
                attribute = "uid0"
        )
        cot.message = getElement(
                xml = xml,
                element = "remarks"
        )
    }

    private fun setQuotes(xml: String): String {
        return xml.replace("'", "\"")
    }

    private fun getAttribute(xml: String, element: String, attribute: String): String {
        val regexPattern = "<$element\\s+.*?$attribute=\"(.*?)\".*?/?>"
        return regexMatch(xml, regexPattern)
    }

    private fun getElement(xml: String, element: String): String {
        val regexPattern = "<$element.*?>(.*?)</$element>"
        return regexMatch(xml, regexPattern)
    }

    private fun regexMatch(xml: String, regexPattern: String): String {
        val matchResult = regexPattern.toRegex().find(xml)
        return if (matchResult == null) {
            throw IllegalArgumentException("No matches for regex pattern '$regexPattern'")
        } else {
            matchResult.groupValues[1]
        }
    }
}
