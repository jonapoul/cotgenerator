package com.jonapoul.common.prefs

import com.jonapoul.sharedprefs.PrefPair

object CommonPrefs {
    /* CoT settings */
    val CALLSIGN = PrefPair(CommonKeys.CALLSIGN, CommonDefaults.CALLSIGN)
    val TEAM_COLOUR = PrefPair(CommonKeys.TEAM_COLOUR, CommonDefaults.TEAM_COLOUR)
    val ICON_ROLE = PrefPair(CommonKeys.ICON_ROLE, CommonDefaults.ICON_ROLE)
    val STALE_TIMER = PrefPair(CommonKeys.STALE_TIMER, CommonDefaults.STALE_TIMER)
    val TRANSMISSION_PERIOD = PrefPair(CommonKeys.TRANSMISSION_PERIOD, CommonDefaults.TRANSMISSION_PERIOD)
    val TRANSMISSION_PROTOCOL = PrefPair(CommonKeys.TRANSMISSION_PROTOCOL, CommonDefaults.TRANSMISSION_PROTOCOL)

    /* Transmission settings */
    val DATA_FORMAT = PrefPair(CommonKeys.DATA_FORMAT, CommonDefaults.DATA_FORMAT)
    val SSL_PRESETS = PrefPair(CommonKeys.SSL_PRESETS, CommonDefaults.SSL_PRESET)
    val TCP_PRESETS = PrefPair(CommonKeys.TCP_PRESETS, CommonDefaults.TCP_PRESET)
    val UDP_PRESETS = PrefPair(CommonKeys.UDP_PRESETS, CommonDefaults.UDP_PRESET)

    /* Other settings */
    val LOG_TO_FILE = PrefPair(CommonKeys.LOG_TO_FILE, CommonDefaults.LOG_TO_FILE)

    /* Preset settings */
    val PRESET_PROTOCOL = PrefPair(CommonKeys.PRESET_PROTOCOL, CommonDefaults.PRESET_PROTOCOL)
    val PRESET_ALIAS = PrefPair(CommonKeys.PRESET_ALIAS, CommonDefaults.PRESET_ALIAS)
    val PRESET_DESTINATION_ADDRESS = PrefPair(CommonKeys.PRESET_DESTINATION_ADDRESS, CommonDefaults.PRESET_DESTINATION_ADDRESS)
    val PRESET_DESTINATION_PORT = PrefPair(CommonKeys.PRESET_DESTINATION_PORT, CommonDefaults.PRESET_DESTINATION_PORT)
    val PRESET_SSL_CLIENTCERT_BYTES = PrefPair(CommonKeys.PRESET_SSL_CLIENT_CERT_BYTES, CommonDefaults.PRESET_SSL_CLIENT_CERT_BYTES)
    val PRESET_SSL_CLIENTCERT_PASSWORD = PrefPair(CommonKeys.PRESET_SSL_CLIENT_CERT_PASSWORD, CommonDefaults.PRESET_SSL_CLIENT_CERT_PASSWORD)
    val PRESET_SSL_TRUSTSTORE_BYTES = PrefPair(CommonKeys.PRESET_SSL_TRUST_STORE_BYTES, CommonDefaults.PRESET_SSL_TRUST_STORE_BYTES)
    val PRESET_SSL_TRUSTSTORE_PASSWORD = PrefPair(CommonKeys.PRESET_SSL_TRUST_STORE_PASSWORD, CommonDefaults.PRESET_SSL_TRUST_STORE_PASSWORD)

    /* List of versions that we've ignored for update reminders */
    val IGNORED_UPDATE_VERSIONS = PrefPair(CommonKeys.IGNORED_UPDATE_VERSIONS, CommonDefaults.IGNORED_UPDATE_VERSIONS)

    /* Coordinate display format in LocationFragment */
    val LOCATION_COORDINATE_FORMAT = PrefPair(CommonKeys.LOCATION_COORDINATE_FORMAT, CommonDefaults.LOCATION_COORDINATE_FORMAT)
}
