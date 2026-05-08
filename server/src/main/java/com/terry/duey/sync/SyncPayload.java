package com.terry.duey.sync;

import java.util.List;

public record SyncPayload(
        List<CategoryPayload> categories,
        List<TodoPayload> todos,
        List<RecurringTemplatePayload> recurringTemplates) {
    public SyncPayload {
        categories = categories == null ? List.of() : categories;
        todos = todos == null ? List.of() : todos;
        recurringTemplates = recurringTemplates == null ? List.of() : recurringTemplates;
    }

    public record CategoryPayload(
            String id,
            String name,
            int sortOrder,
            String createdAt,
            String updatedAt,
            String deletedAt) {}

    public record TodoPayload(
            String id,
            String title,
            String description,
            String categoryId,
            String startDate,
            String endDate,
            boolean completed,
            String recurringTemplateId,
            String recurringOccurrenceDate,
            String createdAt,
            String updatedAt,
            String deletedAt) {}

    public record RecurringTemplatePayload(
            String id,
            String title,
            String description,
            String categoryId,
            String repeatStartDate,
            String repeatEndDate,
            String repeatType,
            String weeklyDays,
            int monthlyDay,
            int periodLengthDays,
            String lastGeneratedUntil,
            String createdAt,
            String updatedAt,
            String deletedAt) {}
}
