package com.jonapoul.common.service

enum class ServiceState {
    RUNNING, STOPPED, ERROR;

    companion object {
        var errorMessage: String? = null
    }
}
