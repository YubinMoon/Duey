package com.terry.duey.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

data class AppDate(
    val year: Int,
    val month: Int,
    val day: Int,
) : Comparable<AppDate> {
    override fun compareTo(other: AppDate): Int {
        if (year != other.year) return year - other.year
        if (month != other.month) return month - other.month
        return day - other.day
    }

    fun addDays(days: Int): AppDate {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, day)
        cal.add(Calendar.DAY_OF_MONTH, days)
        return AppDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    fun daysUntil(other: AppDate): Int {
        val cal1 =
            Calendar.getInstance().apply {
                set(year, month - 1, day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val cal2 =
            Calendar.getInstance().apply {
                set(other.year, other.month - 1, other.day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        return ((cal2.timeInMillis - cal1.timeInMillis) / (1000L * 60 * 60 * 24)).toInt()
    }

    fun dayOfWeekLabel(): String {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, day)
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "일"
            Calendar.MONDAY -> "월"
            Calendar.TUESDAY -> "화"
            Calendar.WEDNESDAY -> "수"
            Calendar.THURSDAY -> "목"
            Calendar.FRIDAY -> "금"
            Calendar.SATURDAY -> "토"
            else -> ""
        }
    }

    override fun toString(): String = "%04d.%02d.%02d".format(year, month, day)

    fun toStorageString(): String = "%04d-%02d-%02d".format(year, month, day)

    companion object {
        fun today(): AppDate {
            val cal = Calendar.getInstance()
            return AppDate(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH),
            )
        }

        fun fromStorageString(value: String): AppDate {
            val parts = value.split("-")
            return AppDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        }
    }
}

@Entity(tableName = "todos")
data class TodoItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val startDate: AppDate,
    val endDate: AppDate,
    val isCompleted: Boolean = false,
    val category: String = "기본",
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val name: String,
)
