package com.jonapoul.common.prefs

object CommonKeys {
    /* CoT settings */
    const val CALLSIGN = "callsign"
    const val TEAM_COLOUR = "team_colour"
    const val ICON_ROLE = "icon_role"
    const val STALE_TIMER = "stale_timer"
    const val TRANSMISSION_PERIOD = "transmission_period"
    const val TRANSMISSION_PROTOCOL = "transmission_protocol"

    /* Transmission settings */
    const val DATA_FORMAT = "data_format"
    const val SSL_PRESETS = "ssl_presets"
    const val TCP_PRESETS = "tcp_presets"
    const val UDP_PRESETS = "udp_presets"
    const val DEST_ADDRESS = "dest_address"
    const val DEST_PORT = "dest_port"
    const val EDIT_PRESETS = "edit_presets"

    /* Other settings */
    const val LOG_TO_FILE = "log_to_file"

    /* Preset settings */
    const val PRESET_PROTOCOL = "preset_protocol"
    const val PRESET_ALIAS = "preset_alias"
    const val PRESET_DESTINATION_ADDRESS = "preset_destination_address"
    const val PRESET_DESTINATION_PORT = "preset_destination_port"
    const val SSL_OPTIONS_CATEGORY = "ssl_options_category"
    const val PRESET_SSL_CLIENT_CERT_BYTES = "preset_ssl_client_cert_bytes"
    const val PRESET_SSL_CLIENT_CERT_PASSWORD = "preset_ssl_client_cert_password"
    const val PRESET_SSL_TRUST_STORE_BYTES = "preset_ssl_trust_store_bytes"
    const val PRESET_SSL_TRUST_STORE_PASSWORD = "preset_ssl_trust_store_password"

    /* List of versions that we've ignored for update reminders */
    const val IGNORED_UPDATE_VERSIONS = "ignored_update_versions"

    /* Coordinate display format in LocationFragment */
    const val LOCATION_COORDINATE_FORMAT = "location_coordinate_display"
}
