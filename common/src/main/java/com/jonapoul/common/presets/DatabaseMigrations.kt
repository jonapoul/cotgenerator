package com.jonapoul.common.presets

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal object DatabaseMigrations {
    val allMigrations: Array<Migration>
        get() = arrayOf(
                V1_TO_V2,
                V2_TO_V3
        )

    /* 06 July 2020: Added SSL client certificate and truststore storage. */
    private val V1_TO_V2: Migration = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            /* Four new optional columns, only needed for SSL presets */
            database.execSQL("ALTER TABLE Presets ADD ClientCert BLOB DEFAULT NULL")
            database.execSQL("ALTER TABLE Presets ADD ClientCertPassword TEXT DEFAULT NULL")
            database.execSQL("ALTER TABLE Presets ADD TrustStore BLOB DEFAULT NULL")
            database.execSQL("ALTER TABLE Presets ADD TrustStorePassword TEXT DEFAULT NULL")
        }
    }

    /* 08 July 2020: Set PRIMARYKEY column to auto-generate (how the heck did I not notice this before?) */
    private val V2_TO_V3: Migration = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            /* Create a new table with the AUTOGENERATE flag on id */
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS PresetsTemp (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "Protocol TEXT, Alias TEXT, Address TEXT, Port INTEGER NOT NULL, " +
                            "ClientCert BLOB, ClientCertPassword TEXT, TrustStore BLOB, TrustStorePassword TEXT)"
            )
            /* Copy all data over from old table to new one */
            database.execSQL(
                    "INSERT INTO PresetsTemp(id, Protocol, Alias, Address, Port, ClientCert, ClientCertPassword, TrustStore, TrustStorePassword)\n" +
                            "SELECT id, Protocol, Alias, Address, Port, ClientCert, ClientCertPassword, TrustStore, TrustStorePassword\n" +
                            "FROM Presets;"
            )
            /* Delete the old table, then rename the temp table to match that expected */
            database.execSQL("DROP TABLE Presets")
            database.execSQL("ALTER TABLE PresetsTemp RENAME TO Presets;")
        }
    }
}
