package com.wkuxr.eclipsetotality.database

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "metadata")
class Metadata(
    @NonNull
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,

    @NonNull
    var latitude: Double = 0.0,

    @NonNull
    var longitude: Double = 0.0,

    @NonNull
    var altitude: Double = 0.0,

    @NonNull
    var captureTime: Long = 0
) {}