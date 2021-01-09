package com.jonapoul.cotbeacon.cot

enum class EmergencyType(val description: String?, val type: String) {
    ALERT_911(
            description = "911 Alert",
            type = "b-a-o-tbl"
    ),

    RING_THE_BELL(
            description = "Ring The Bell",
            type = "b-a-o-pan"
    ),

    GEO_FENCE_BREACHED(
            description = "Geo-fence Breached",
            type = "b-a-g"
    ),

    TROOPS_IN_CONTACT(
            description = "Troops In Contact",
            type = "b-a-o-opn"
    ),

    CANCEL(
            description = "Cancel",
            type = "b-a-o-can"
    );

    companion object {
        fun fromString(string: String): EmergencyType {
            return values()
                    .firstOrNull { it.description == string }
                    ?: throw IllegalArgumentException("Unknown emergency type '$string'")
        }
    }
}
