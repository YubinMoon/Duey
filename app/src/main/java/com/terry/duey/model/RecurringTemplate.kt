package com.terry.duey.model

import androidx.room.Entity
import androidx.room.PrimaryKey

object RecurrenceTypes {
    const val DAILY = "DAILY"
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"
}

@Entity(tableName = "recurring_templates")
data class RecurringTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val categoryId: Long? = null,
    val repeatStartDate: AppDate,
    val repeatEndDate: AppDate,
    val repeatType: String,
    val weeklyDays: String = "",
    val monthlyDay: Int = 1,
    val periodLengthDays: Int = 1,
    val lastGeneratedUntil: AppDate? = null,
)
