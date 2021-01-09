package com.jonapoul.common.presets

import androidx.room.TypeConverter
import com.jonapoul.common.utils.Protocol

class ProtocolConverter {
    @TypeConverter
    fun stringToProtocol(string: String): Protocol {
        return Protocol.fromString(string)
    }

    @TypeConverter
    fun protocolToString(protocol: Protocol): String {
        return protocol.toString()
    }
}
