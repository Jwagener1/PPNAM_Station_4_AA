package com.ppnam.station4aa.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WasteOutboxEntity::class], version = 1, exportSchema = false)
abstract class WasteOutboxDatabase : RoomDatabase() {
    abstract fun wasteOutboxDao(): WasteOutboxDao

    companion object {
        fun create(context: Context): WasteOutboxDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                WasteOutboxDatabase::class.java,
                "ppnam_station4_outbox.db",
            ).build()
    }
}
