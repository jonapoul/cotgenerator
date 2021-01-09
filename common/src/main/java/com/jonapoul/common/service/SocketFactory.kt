package com.jonapoul.common.service

import android.content.SharedPreferences
import com.jonapoul.common.prefs.CommonPrefs
import com.jonapoul.common.presets.OutputPreset
import com.jonapoul.common.repositories.IPresetRepository
import com.jonapoul.common.utils.Constants
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.OutputStream
import java.net.*
import java.security.*
import java.security.cert.CertificateException
import javax.net.ssl.*

class SocketFactory(
        private val prefs: SharedPreferences,
        private val presetRepository: IPresetRepository
) {
    private val lock = Any()

    fun getUdpInputSocket(group: String, port: Int): MulticastSocket {
        Timber.i("getUdpInputSocket %s %d", group, port)
        return MulticastSocket(port).also {
            it.joinGroup(InetAddress.getByName(group))
            it.loopbackMode = true // don't subscribe to loopback traffic
        }
    }

    fun getTcpSocket(): Socket {
        synchronized(lock) {
            return Socket().also {
                val address = getDestinationIp()
                val port = getDestinationPort()
                Timber.i("getTcpSocket %s %d", address, port)
                it.connect(
                        InetSocketAddress(address, port),
                        Constants.TCP_SOCKET_TIMEOUT_MS
                )
                Timber.i("TCP socket connected!")
            }
        }
    }

    fun getSslSocket(): SSLSocket {
        synchronized(lock) {
            val preset = buildPreset()
            val certStore = loadKeyStore(
                    preset.clientCert,
                    preset.clientCertPassword!!.toCharArray()
            )
            val trustStore = loadKeyStore(
                    preset.trustStore,
                    preset.trustStorePassword!!.toCharArray()
            )
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(
                    getKeyManagers(certStore, preset.clientCertPassword!!.toCharArray()),
                    getTrustManagers(trustStore),
                    SecureRandom()
            )
            val address = getDestinationIp()
            val port = getDestinationPort()
            Timber.i("getSslSocket %s %d", address, port)
            return (sslContext.socketFactory.createSocket() as SSLSocket).also {
                it.enabledProtocols = arrayOf("TLSv1.1", "TLSv1.2")
                it.connect(
                        InetSocketAddress(address, port),
                        Constants.TCP_SOCKET_TIMEOUT_MS
                )
                Timber.i("SSL socket connected!")
            }
        }
    }

    fun getOutputStream(socket: Socket): OutputStream {
        return SynchronisedOutputStream(socket.getOutputStream())
    }

    private fun buildPreset(): OutputPreset {
        /* This contains only the basic values: protocol, alias, address and port. We now need to query the
         * database to grab SSL cert information, then return this upgraded OutputPreset object if successful. */
        val basicPreset = OutputPreset.fromPrefs(prefs)
        val sslPreset = presetRepository.getPreset(basicPreset!!.protocol, basicPreset.address, basicPreset.port)
        return if (sslPreset == null) {
            /* If there is no database entry corresponding to the above protocol/address/port combo,
             * we treat this as a default. So grab the values from default certs */
            val sslDefaults = presetRepository.defaultsByProtocol(basicPreset.protocol)
            if (sslDefaults.isNotEmpty()) {
                sslDefaults[0] // Discord TAK Server
            } else {
                throw RuntimeException("Check the length of the SSL Presets list, YOU IDIOT")
            }
        } else {
            /* Valid cert info fetched from the database, so return it. */
            sslPreset
        }
    }

    @Throws(KeyStoreException::class, CertificateException::class, NoSuchAlgorithmException::class, IOException::class)
    private fun loadKeyStore(bytes: ByteArray?, password: CharArray): KeyStore {
        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(ByteArrayInputStream(bytes), password)
        return keyStore
    }

    @Throws(NoSuchAlgorithmException::class, KeyStoreException::class)
    private fun getTrustManagers(trustStore: KeyStore): Array<TrustManager?>? {
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(trustStore)
        return trustManagerFactory.trustManagers
    }

    @Throws(NoSuchAlgorithmException::class, KeyStoreException::class, UnrecoverableKeyException::class)
    private fun getKeyManagers(certStore: KeyStore, password: CharArray): Array<KeyManager?>? {
        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(certStore, password)
        return keyManagerFactory.keyManagers
    }

    private fun getDestinationIp() = InetAddress.getByName(prefs.getString(CommonPrefs.DEST_ADDRESS, ""))
    private fun getDestinationPort() = prefs.getString(CommonPrefs.DEST_PORT, "")!!.toInt()
}
