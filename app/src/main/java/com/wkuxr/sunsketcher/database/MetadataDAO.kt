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

    @Query("UPDATE Metadata SET filepath = :value1 WHERE id = :id")
    fun updateRow(id: Int, value1: String): Int
}