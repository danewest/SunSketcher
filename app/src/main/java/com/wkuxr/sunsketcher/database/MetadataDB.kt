package com.wkuxr.sunsketcher.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

//defines the SQLite database for the internal metadata storage
@Database(entities = [Metadata::class], version = 1)
abstract class MetadataDB : RoomDatabase() {

    //companion objects in kotlin are basically just public static variables and methods in java
    companion object {
        lateinit var db: MetadataDB

        //gets a reference to the database and initializes it if it isn't already
        fun createDB(context: Context): MetadataDB {
            db = Room.databaseBuilder(context, MetadataDB::class.java, "metadata").allowMainThreadQueries().build()
            db.initialize()

            return db
        }
    }

    //abstract method to initialize the Metadata table's database access object that gets auto-generated during compilation
    abstract fun metadataDao(): MetadataDAO

    //initializes the database access objects for the database
    fun initialize(){
        metadataDao()
    }

    //insert the given Metadata object into the Metadata table
    fun addMetadata(metadata: Metadata): Long {
        return metadataDao().addImageMeta(metadata)
    }

    //gets a list of all Metadata objects in the Metadata table
    fun getMetadata(): List<Metadata> {
        return metadataDao().getAllImageMetas()
    }

    //update the given metadata id with the given values
    fun updateRowFilepath(id: Int, filepath: String, fstop: Double, iso: Int, whiteBalance: Int, exposure: Double, focalDistance: String, isCropped: Boolean): Int {
        return metadataDao().updateRow(id, filepath, fstop, iso, whiteBalance, exposure, focalDistance, isCropped)
    }

}
