package com.wkuxr.sunsketcher.database

import android.content.Context
import androidx.room.Database
import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper

@Database(entities = [Metadata::class], version = 1)
abstract class MetadataDB : RoomDatabase() {
    companion object {
        lateinit var db: MetadataDB

        fun createDB(context: Context): MetadataDB {
            db = Room.databaseBuilder(context, MetadataDB::class.java, "metadata").allowMainThreadQueries().build()
            db.initialize()

            return db
        }
    }

    abstract fun metadataDao(): MetadataDAO

    fun initialize(){
        metadataDao()
    }

    fun addMetadata(metadata: Metadata): Long {
        return metadataDao().addImageMeta(metadata)
    }

    fun getMetadata(): List<Metadata> {
        return metadataDao().getAllImageMetas()
    }

    override fun clearAllTables() {
        TODO("Not yet implemented")
    }

    override fun createInvalidationTracker(): InvalidationTracker {
        TODO("Not yet implemented")
    }

    override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper {
        TODO("Not yet implemented")
    }

}
