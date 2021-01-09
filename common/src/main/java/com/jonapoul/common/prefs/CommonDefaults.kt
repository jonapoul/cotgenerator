package com.jonapoul.common.prefs

import com.jonapoul.common.cot.CotRole
import com.jonapoul.common.ui.location.CoordinateFormat
import com.jonapoul.common.utils.DataFormat
import com.jonapoul.common.utils.Protocol

object CommonDefaults {
    /* CoT settings */
    const val CALLSIGN = "GENERATED"
    const val TEAM_COLOUR = 0xFF00FFFF // cyan
    val ICON_ROLE = CotRole.TEAM_MEMBER.string
    const val STALE_TIMER = 5
    const val TRANSMISSION_PERIOD = 3
    val TRANSMISSION_PROTOCOL = Protocol.UDP.toString()

    /* Transmission settings */
    val DATA_FORMAT = DataFormat.PROTOBUF.toString()
    const val SSL_PRESET = "SSL¶Discord TAK Server¶discordtakserver.mooo.com¶58088"
    const val TCP_PRESET = "TCP¶Public FreeTakServer¶204.48.30.216¶8087"
    const val UDP_PRESET = "UDP¶Default Multicast SA¶239.2.3.1¶6969"

    val PRESET_PROTOCOL = Protocol.UDP.toString()
    const val PRESET_ALIAS = ""
    const val PRESET_DESTINATION_ADDRESS = ""
    const val PRESET_DESTINATION_PORT = ""
    val PRESET_SSL_CLIENT_CERT_BYTES = byteArrayOf()
    const val PRESET_SSL_CLIENT_CERT_PASSWORD = ""
    val PRESET_SSL_TRUST_STORE_BYTES = byteArrayOf()
    const val PRESET_SSL_TRUST_STORE_PASSWORD = ""

    const val LOG_TO_FILE = false

    val LOCATION_COORDINATE_FORMAT = CoordinateFormat.DD.name
}
