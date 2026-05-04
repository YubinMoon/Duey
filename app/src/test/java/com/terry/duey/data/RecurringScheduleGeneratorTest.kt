package com.terry.duey.data

import com.terry.duey.model.AppDate
import com.terry.duey.model.RecurrenceTypes
import com.terry.duey.model.RecurringTemplate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class RecurringScheduleGeneratorTest {
    @Test
    fun daily_generatesInclusiveDatesWithPeriodLength() {
        val template = template(
            repeatType = RecurrenceTypes.DAILY,
            repeatStartDate = AppDate(2026, 5, 1),
            repeatEndDate = AppDate(2026, 5, 3),
            periodLengthDays = 2,
        )

        val todos = RecurringScheduleGenerator.generatedTodos(
            template = template,
            fromDate = AppDate(2026, 5, 1),
            untilDate = AppDate(2026, 5, 10),
        )

        assertEquals(listOf(AppDate(2026, 5, 1), AppDate(2026, 5, 2), AppDate(2026, 5, 3)), todos.map { it.startDate })
        assertEquals(AppDate(2026, 5, 2), todos.first().endDate)
    }

    @Test
    fun weekly_generatesSelectedDaysOnly() {
        val template = template(
            repeatType = RecurrenceTypes.WEEKLY,
            repeatStartDate = AppDate(2026, 5, 1),
            repeatEndDate = AppDate(2026, 5, 10),
            weeklyDays = "${Calendar.MONDAY},${Calendar.WEDNESDAY}",
        )

        val todos = RecurringScheduleGenerator.generatedTodos(
            template = template,
            fromDate = AppDate(2026, 5, 1),
            untilDate = AppDate(2026, 5, 10),
        )

        assertEquals(listOf(AppDate(2026, 5, 4), AppDate(2026, 5, 6)), todos.map { it.startDate })
    }

    @Test
    fun monthly_skipsMonthsWithoutSelectedDay() {
        val template = template(
            repeatType = RecurrenceTypes.MONTHLY,
            repeatStartDate = AppDate(2026, 1, 1),
            repeatEndDate = AppDate(2026, 4, 30),
            monthlyDay = 31,
        )

        val todos = RecurringScheduleGenerator.generatedTodos(
            template = template,
            fromDate = AppDate(2026, 1, 1),
            untilDate = AppDate(2026, 4, 30),
        )

        assertEquals(listOf(AppDate(2026, 1, 31), AppDate(2026, 3, 31)), todos.map { it.startDate })
    }

    @Test
    fun nextGenerationStart_usesDayAfterLastGeneratedUntil() {
        val template = template(
            repeatStartDate = AppDate(2026, 5, 1),
            repeatEndDate = AppDate(2026, 12, 31),
            lastGeneratedUntil = AppDate(2026, 5, 10),
        )

        assertEquals(AppDate(2026, 5, 11), template.nextGenerationStart(AppDate(2026, 5, 5)))
    }

    private fun template(
        repeatType: String = RecurrenceTypes.DAILY,
        repeatStartDate: AppDate = AppDate(2026, 5, 1),
        repeatEndDate: AppDate = AppDate(2026, 5, 31),
        weeklyDays: String = "",
        monthlyDay: Int = 1,
        periodLengthDays: Int = 1,
        lastGeneratedUntil: AppDate? = null,
    ): RecurringTemplate = RecurringTemplate(
        id = 7,
        title = "Repeat",
        description = "Generated",
        category = "기본",
        repeatStartDate = repeatStartDate,
        repeatEndDate = repeatEndDate,
        repeatType = repeatType,
        weeklyDays = weeklyDays,
        monthlyDay = monthlyDay,
        periodLengthDays = periodLengthDays,
        lastGeneratedUntil = lastGeneratedUntil,
    )
}
