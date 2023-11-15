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
}