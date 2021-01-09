package com.jonapoul.common.utils

object TimeUtils {
    fun msToString(ms: Long): String {
        return when {
            ms < MINUTE -> "%.0fs".format(ms / SECOND)
            ms < HOUR -> "%.0fm".format(ms / MINUTE)
            else -> "%.0fh".format(ms / HOUR)
        }
    }

    private const val SECOND = 1e3
    private const val MINUTE = SECOND * 60
    private const val HOUR = MINUTE * 60
}
