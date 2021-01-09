package com.jonapoul.common.ui

interface IServiceCommunicator {
    fun startService()
    fun stopService()
    fun isServiceNull(): Boolean
    fun isServiceRunning(): Boolean
}