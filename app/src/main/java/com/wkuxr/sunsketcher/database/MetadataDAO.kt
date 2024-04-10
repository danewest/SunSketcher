package com.wkuxr.sunsketcher.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

//these compile to predefined SQLite database queries.
@Dao
interface MetadataDAO {
    //get a list of all Metadata objects (rows) in the Metadata table
    @Query("SELECT * FROM Metadata")
    fun getAllImageMetas(): List<Metadata>

    //insert a Metadata object into the Metadata table
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addImageMeta(metadata: Metadata): Long

    //update a Metadata object in the Metadata table
    @Query("UPDATE Metadata SET filepath = :filepath, fstop = :fstop, iso = :iso, whiteBalance = :whiteBalance, exposure = :exposure, focalDistance = :focalDistance, isCropped = :isCropped WHERE id = :id")
    fun updateRow(id: Int, filepath: String, fstop: Double, iso: Int, whiteBalance: Int, exposure: Double, focalDistance: String, isCropped: Boolean): Int
}