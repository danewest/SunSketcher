package com.wkuxr.sunsketcher.database

import androidx.room.Entity
import androidx.room.PrimaryKey

//compiles as an SQLite database table, these are the metadata entries per-image. The data found within should be self-evident
@Entity(tableName = "metadata")
class Metadata(
    var filepath: String = "",

    var latitude: Double = 0.0,

    var longitude: Double = 0.0,

    var altitude: Double = 0.0,

    var captureTime: Long = 0,

    var fstop: Double = 0.0,

    var iso: Int = 0,

    var whiteBalance: Int = 0,

    var exposure: Double = 0.0,

    var focalDistance: String = "",

    var isCropped: Boolean = false
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}