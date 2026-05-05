package com.terry.duey.data

import androidx.room.withTransaction
import com.terry.duey.model.AppDate
import com.terry.duey.model.RecurringTemplate

suspend fun syncRecurringSchedules(
    database: AppDatabase,
    today: AppDate = AppDate.today(),
) {
    val targetUntil = today.addDays(RECURRING_GENERATION_DAYS)
    val templates = database.recurringTemplateDao().getTemplatesSnapshot()

    database.withTransaction {
        templates.forEach { template ->
            syncRecurringTemplate(database, template, today, targetUntil)
        }
    }
}

suspend fun syncRecurringTemplate(
    database: AppDatabase,
    template: RecurringTemplate,
    today: AppDate = AppDate.today(),
    targetUntil: AppDate = today.addDays(RECURRING_GENERATION_DAYS),
) {
    val generationStart = template.nextGenerationStart(today)
    val generatedTodos = RecurringScheduleGenerator.generatedTodos(
        template = template,
        fromDate = generationStart,
        untilDate = targetUntil,
    )
    if (generatedTodos.isNotEmpty()) {
        database.todoDao().insertTodos(generatedTodos)
    }
    database.recurringTemplateDao().updateLastGeneratedUntil(template.id, targetUntil)
}
