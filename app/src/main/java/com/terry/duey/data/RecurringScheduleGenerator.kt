package com.terry.duey.data

import com.terry.duey.model.AppDate
import com.terry.duey.model.RecurrenceTypes
import com.terry.duey.model.RecurringTemplate
import com.terry.duey.model.TodoItem
import java.util.Calendar

const val RECURRING_GENERATION_DAYS = 180

object RecurringScheduleGenerator {
    fun generatedTodos(
        template: RecurringTemplate,
        fromDate: AppDate,
        untilDate: AppDate,
    ): List<TodoItem> {
        val firstDate = maxOf(template.repeatStartDate, fromDate)
        val lastDate = minOf(template.repeatEndDate, untilDate)
        if (firstDate > lastDate) return emptyList()

        return occurrences(template, firstDate, lastDate).map { occurrenceDate ->
            TodoItem(
                title = template.title,
                description = template.description,
                categoryId = template.categoryId,
                startDate = occurrenceDate,
                endDate = occurrenceDate.addDays(template.periodLengthDays.coerceAtLeast(1) - 1),
                recurringTemplateId = template.id,
                recurringOccurrenceDate = occurrenceDate,
            )
        }
    }

    private fun occurrences(
        template: RecurringTemplate,
        fromDate: AppDate,
        untilDate: AppDate,
    ): List<AppDate> = when (template.repeatType) {
        RecurrenceTypes.DAILY -> dailyOccurrences(fromDate, untilDate)
        RecurrenceTypes.WEEKLY -> weeklyOccurrences(template.weeklyDaysSet(), fromDate, untilDate)
        RecurrenceTypes.MONTHLY -> monthlyOccurrences(template.monthlyDay, fromDate, untilDate)
        else -> emptyList()
    }

    private fun dailyOccurrences(fromDate: AppDate, untilDate: AppDate): List<AppDate> {
        val dates = mutableListOf<AppDate>()
        var current = fromDate
        while (current <= untilDate) {
            dates += current
            current = current.addDays(1)
        }
        return dates
    }

    private fun weeklyOccurrences(
        weeklyDays: Set<Int>,
        fromDate: AppDate,
        untilDate: AppDate,
    ): List<AppDate> {
        if (weeklyDays.isEmpty()) return emptyList()

        return dailyOccurrences(fromDate, untilDate)
            .filter { date -> date.calendarDayOfWeek() in weeklyDays }
    }

    private fun monthlyOccurrences(
        monthlyDay: Int,
        fromDate: AppDate,
        untilDate: AppDate,
    ): List<AppDate> {
        val dates = mutableListOf<AppDate>()
        val cal = Calendar.getInstance().apply {
            set(fromDate.year, fromDate.month - 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        while (true) {
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            if (monthlyDay in 1..maxDay) {
                val date = AppDate(year, month, monthlyDay)
                if (date in fromDate..untilDate) {
                    dates += date
                }
            }

            cal.add(Calendar.MONTH, 1)
            val nextMonthStart = AppDate(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                1,
            )
            if (nextMonthStart > untilDate) break
        }

        return dates
    }
}

fun RecurringTemplate.nextGenerationStart(today: AppDate): AppDate {
    val nextAfterPreviousRun = lastGeneratedUntil?.addDays(1) ?: today
    return maxOf(repeatStartDate, nextAfterPreviousRun, today)
}

fun RecurringTemplate.normalizedWeeklyDays(): String = weeklyDaysSet()
    .sorted()
    .joinToString(",")

fun RecurringTemplate.weeklyDaysSet(): Set<Int> = weeklyDays
    .split(",")
    .mapNotNull { it.trim().toIntOrNull() }
    .filter { it in Calendar.SUNDAY..Calendar.SATURDAY }
    .toSet()

private fun AppDate.calendarDayOfWeek(): Int {
    val cal = Calendar.getInstance()
    cal.set(year, month - 1, day)
    return cal.get(Calendar.DAY_OF_WEEK)
}
