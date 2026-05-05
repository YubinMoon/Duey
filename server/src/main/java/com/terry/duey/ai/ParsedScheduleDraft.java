package com.terry.duey.ai;

public record ParsedScheduleDraft(
        String title,
        String description,
        String category,
        String startDate,
        String endDate
) {
}
