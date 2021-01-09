package com.jonapoul.common.repositories.impl

import android.content.Context
import androidx.lifecycle.LiveData
import com.jonapoul.common.R
import com.jonapoul.common.presets.OutputPreset
import com.jonapoul.common.presets.IPresetDao
import com.jonapoul.common.repositories.IPresetRepository
import com.jonapoul.common.utils.FileUtils
import com.jonapoul.common.utils.Protocol
import com.jonapoul.common.utils.exhaustive
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Inject


class PresetRepository @Inject constructor(
        context: Context,
        private val presetDao: IPresetDao
): IPresetRepository {
    private val executor: Executor = Executors.newSingleThreadExecutor()

    private val udpDefaults = listOf(
            OutputPreset(
                    Protocol.UDP,
                    context.getString(R.string.udp_default_sa),
                    context.getString(R.string.udp_default_sa_ip),
                    context.getString(R.string.udp_default_sa_port).toInt()
            )
    )
    private val tcpDefaults = listOf(
            OutputPreset(
                    Protocol.TCP,
                    context.getString(R.string.tcp_freetakserver_name),
                    context.getString(R.string.tcp_freetakserver_ip),
                    context.getString(R.string.tcp_freetakserver_port).toInt()
            )
    )
    private val sslDefaults = listOf(
            OutputPreset(
                    Protocol.SSL,
                    context.getString(R.string.ssl_takserver_name),
                    context.getString(R.string.ssl_takserver_ip),
                    context.getString(R.string.ssl_takserver_port).toInt(),
                    FileUtils.rawResourceToByteArray(context, R.raw.discord_client),
                    context.getString(R.string.default_ssl_password),
                    FileUtils.rawResourceToByteArray(context, R.raw.discord_truststore),
                    context.getString(R.string.default_ssl_password)
            )
    )

    override fun insertPreset(preset: OutputPreset) {
        executor.execute { presetDao.insert(preset) }
    }

    override fun deletePreset(preset: OutputPreset) {
        executor.execute { presetDao.delete(preset) }
    }

    override fun getPreset(protocol: Protocol, address: String, port: Int): OutputPreset? {
        return presetDao.getPreset(protocol.toString(), address, port)
    }

    /* Returns a list of only those presets entered by the user */
    override fun getCustomByProtocol(protocol: Protocol): LiveData<List<OutputPreset>> {
        return presetDao.getByProtocol(protocol.toString())
    }

    override fun deleteDatabase() {
        executor.execute { presetDao.deleteAll() }
    }

    override fun defaultsByProtocol(protocol: Protocol): List<OutputPreset> {
        return when (protocol) {
            Protocol.SSL -> sslDefaults
            Protocol.TCP -> tcpDefaults
            Protocol.UDP -> udpDefaults
        }.exhaustive
    }
}
