package com.jonapoul.common.presets

import android.content.SharedPreferences
import android.os.Parcel
import android.os.Parcelable
import androidx.room.*
import com.jonapoul.common.prefs.getStringFromPair
import com.jonapoul.common.utils.Protocol

@Entity(tableName = "Presets", indices = [Index(value = ["id"], unique = true)])
class OutputPreset() : Parcelable {
    /* Basic output fields */
    @PrimaryKey(autoGenerate = true)
    var id = 0

    @ColumnInfo(name = "Protocol")
    var protocol: Protocol = Protocol.UDP

    @ColumnInfo(name = "Alias")
    var alias: String = ""

    @ColumnInfo(name = "Address")
    var address: String = ""

    @ColumnInfo(name = "Port")
    var port = 0

    /* SSL cert data. This are all null for UDP/TCP presets */
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB, name = "ClientCert")
    var clientCert: ByteArray? = null

    @ColumnInfo(name = "ClientCertPassword")
    var clientCertPassword: String? = null

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB, name = "TrustStore")
    var trustStore: ByteArray? = null

    @ColumnInfo(name = "TrustStorePassword")
    var trustStorePassword: String? = null

    @Ignore
    constructor(parcel: Parcel) : this() {
        id = parcel.readInt()
        alias = parcel.readString() ?: ""
        address = parcel.readString() ?: ""
        port = parcel.readInt()
        clientCert = parcel.createByteArray()
        clientCertPassword = parcel.readString()
        trustStore = parcel.createByteArray()
        trustStorePassword = parcel.readString()
    }

    @Ignore // Don't use this constructor for database initialisation
    constructor(protocol: Protocol, alias: String, address: String, port: Int)
            : this(protocol, alias, address, port, null, null, null, null)

    constructor(
            protocol: Protocol,
            alias: String,
            address: String,
            port: Int,
            clientCert: ByteArray? = null,
            clientCertPassword: String? = null,
            trustStore: ByteArray? = null,
            trustStorePassword: String? = null) : this() {
        this.protocol = protocol
        this.alias = alias
        this.address = address
        this.port = port
        this.clientCert = clientCert
        this.clientCertPassword = clientCertPassword
        this.trustStore = trustStore
        this.trustStorePassword = trustStorePassword
    }

    override fun toString(): String {
        return protocol.toString() + SEPARATOR + alias + SEPARATOR + address + SEPARATOR + port
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(alias)
        parcel.writeString(address)
        parcel.writeInt(port)
        parcel.writeByteArray(clientCert)
        parcel.writeString(clientCertPassword)
        parcel.writeByteArray(trustStore)
        parcel.writeString(trustStorePassword)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OutputPreset> {
        override fun createFromParcel(parcel: Parcel): OutputPreset {
            return OutputPreset(parcel)
        }

        override fun newArray(size: Int): Array<OutputPreset?> {
            return arrayOfNulls(size)
        }

        const val SEPARATOR = "¶" // pilcrow

        fun fromString(str: String?): OutputPreset? {
            if (str == null) return null
            val split = str.split(SEPARATOR).toTypedArray()
            if (split.size != 4) {
                throw RuntimeException("There should only be 4 elements in this string: " + str + ". Found " + split.size)
            }
            return OutputPreset(
                    Protocol.fromString(split[0]),
                    split[1],
                    split[2],
                    split[3].toInt()
            )
        }

        fun fromPrefs(prefs: SharedPreferences): OutputPreset? {
            val key = Protocol.fromPrefs(prefs).presetPref
            return fromString(prefs.getStringFromPair(key))
        }
    }
}