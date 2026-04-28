package com.example.mytodo.data

import androidx.room.TypeConverter
import com.example.mytodo.model.AppDate

class DateConverters {
    @TypeConverter
    fun fromAppDate(date: AppDate): String = date.toStorageString()

    @TypeConverter
    fun toAppDate(value: String): AppDate = AppDate.fromStorageString(value)
}
