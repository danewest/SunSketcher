package com.wkuxr.sunsketcher.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "metadata")
class Metadata(
    var filepath: String = "",

    var latitude: Double = 0.0,

    var longitude: Double = 0.0,

    var altitude: Double = 0.0,

    var captureTime: Long = 0
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}