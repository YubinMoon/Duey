package com.terry.duey.viewmodel

import com.terry.duey.data.DateConverters
import com.terry.duey.model.AppDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TodoViewModelTest {
    private val converters = DateConverters()

    @Test
    fun appDate_addDaysCrossesMonthBoundary() {
        assertEquals(AppDate(2026, 5, 2), AppDate(2026, 4, 29).addDays(3))
    }

    @Test
    fun appDate_daysUntilHandlesReverseRange() {
        assertEquals(-5, AppDate(2026, 5, 10).daysUntil(AppDate(2026, 5, 5)))
    }

    @Test
    fun dateConverters_roundTripStorageFormat() {
        val date = AppDate(2026, 4, 8)
        val stored = converters.fromAppDate(date)

        assertEquals("2026-04-08", stored)
        assertEquals(date, converters.toAppDate(stored))
    }
}
