package com.terry.duey.data

import androidx.room.TypeConverter
import com.terry.duey.model.AppDate

class DateConverters {
    @TypeConverter
    fun fromAppDate(date: AppDate): String = date.toStorageString()

    @TypeConverter
    fun toAppDate(value: String): AppDate = AppDate.fromStorageString(value)
}
