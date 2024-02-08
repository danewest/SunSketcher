package com.wkuxr.sunsketcher.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MetadataDAO {
    @Query("SELECT * FROM Metadata")
    fun getAllImageMetas(): List<Metadata>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addImageMeta(metadata: Metadata): Long

    @Query("UPDATE Metadata SET filepath = :filepath, fstop = :fstop, iso = :iso, whiteBalance = :whiteBalance, exposure = :exposure, focalDistance = :focalDistance WHERE id = :id")
    fun updateRow(id: Int, filepath: String, fstop: Double, iso: Int, whiteBalance: Int, exposure: Double, focalDistance: String): Int
}