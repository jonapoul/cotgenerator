package com.jonapoul.common.utils

import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

object NetworkUtils {
    fun getValidInterfaces(): List<NetworkInterface> {
        return NetworkInterface.getNetworkInterfaces()
                .asSequence()
                /* Remove any interfaces that are either inactive, have no addresses or are localhost.
                 * Localhost is ignored because multicast loopback is enabled. */
                .filter { it.isUp && interfaceHasIpv4Address(it) && it.name != "lo" }
                .toList()
    }

    fun getAddressFromInterface(ni: NetworkInterface): InetAddress? {
        return ni.interfaceAddresses.map { it.address }.firstOrNull { it is Inet4Address }
    }

    private fun interfaceHasIpv4Address(ni: NetworkInterface): Boolean {
        return getAddressFromInterface(ni) != null
    }

    fun getLocalAddress(): InetAddress {
        return getValidInterfaces()
                .mapNotNull { getAddressFromInterface(it) }
                .first()
    }
}
