package com.terry.duey.sync;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class SyncRepository {
    private final JdbcClient jdbcClient;

    public SyncRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public SyncPayload bootstrap(String userId) {
        return new SyncPayload(categories(userId), todos(userId), recurringTemplates(userId));
    }

    @Transactional
    public SyncPayload push(String userId, SyncPayload request) {
        request.categories().forEach(category -> upsertCategory(userId, category));
        request.recurringTemplates().forEach(template -> upsertRecurringTemplate(userId, template));
        request.todos().forEach(todo -> upsertTodo(userId, todo));
        return bootstrap(userId);
    }

    private List<SyncPayload.CategoryPayload> categories(String userId) {
        return jdbcClient
                .sql(
                        """
                        SELECT id, name, sort_order, created_at, updated_at, deleted_at
                        FROM categories WHERE user_id = :userId ORDER BY sort_order ASC, name ASC
                        """)
                .param("userId", userId)
                .query(
                        (rs, rowNum) ->
                                new SyncPayload.CategoryPayload(
                                        rs.getString("id"),
                                        rs.getString("name"),
                                        rs.getInt("sort_order"),
                                        rs.getString("created_at"),
                                        rs.getString("updated_at"),
                                        rs.getString("deleted_at")))
                .list();
    }

    private List<SyncPayload.TodoPayload> todos(String userId) {
        return jdbcClient
                .sql(
                        """
                        SELECT id, title, description, category_id, start_date, end_date, is_completed,
                               recurring_template_id, recurring_occurrence_date, created_at, updated_at, deleted_at
                        FROM todos WHERE user_id = :userId ORDER BY start_date ASC, end_date ASC, title ASC
                        """)
                .param("userId", userId)
                .query(
                        (rs, rowNum) ->
                                new SyncPayload.TodoPayload(
                                        rs.getString("id"),
                                        rs.getString("title"),
                                        rs.getString("description"),
                                        rs.getString("category_id"),
                                        rs.getString("start_date"),
                                        rs.getString("end_date"),
                                        rs.getInt("is_completed") == 1,
                                        rs.getString("recurring_template_id"),
                                        rs.getString("recurring_occurrence_date"),
                                        rs.getString("created_at"),
                                        rs.getString("updated_at"),
                                        rs.getString("deleted_at")))
                .list();
    }

    private List<SyncPayload.RecurringTemplatePayload> recurringTemplates(String userId) {
        return jdbcClient
                .sql(
                        """
                        SELECT id, title, description, category_id, repeat_start_date, repeat_end_date,
                               repeat_type, weekly_days, monthly_day, period_length_days, last_generated_until,
                               created_at, updated_at, deleted_at
                        FROM recurring_templates WHERE user_id = :userId ORDER BY repeat_start_date ASC, title ASC
                        """)
                .param("userId", userId)
                .query(
                        (rs, rowNum) ->
                                new SyncPayload.RecurringTemplatePayload(
                                        rs.getString("id"),
                                        rs.getString("title"),
                                        rs.getString("description"),
                                        rs.getString("category_id"),
                                        rs.getString("repeat_start_date"),
                                        rs.getString("repeat_end_date"),
                                        rs.getString("repeat_type"),
                                        rs.getString("weekly_days"),
                                        rs.getInt("monthly_day"),
                                        rs.getInt("period_length_days"),
                                        rs.getString("last_generated_until"),
                                        rs.getString("created_at"),
                                        rs.getString("updated_at"),
                                        rs.getString("deleted_at")))
                .list();
    }

    private void upsertCategory(String userId, SyncPayload.CategoryPayload category) {
        String id = idOrNew(category.id());
        if (exists("categories", userId, id)) {
            jdbcClient
                    .sql(
                            """
                            UPDATE categories SET name = :name, sort_order = :sortOrder,
                                   updated_at = :updatedAt, deleted_at = :deletedAt
                            WHERE user_id = :userId AND id = :id
                            """)
                    .param("name", category.name())
                    .param("sortOrder", category.sortOrder())
                    .param("updatedAt", timestamp(category.updatedAt()))
                    .param("deletedAt", category.deletedAt())
                    .param("userId", userId)
                    .param("id", id)
                    .update();
        } else {
            jdbcClient
                    .sql(
                            """
                            INSERT INTO categories (id, user_id, name, sort_order, created_at, updated_at, deleted_at)
                            VALUES (:id, :userId, :name, :sortOrder, :createdAt, :updatedAt, :deletedAt)
                            """)
                    .param("id", id)
                    .param("userId", userId)
                    .param("name", category.name())
                    .param("sortOrder", category.sortOrder())
                    .param("createdAt", timestamp(category.createdAt()))
                    .param("updatedAt", timestamp(category.updatedAt()))
                    .param("deletedAt", category.deletedAt())
                    .update();
        }
    }

    private void upsertTodo(String userId, SyncPayload.TodoPayload todo) {
        String id = idOrNew(todo.id());
        if (exists("todos", userId, id)) {
            jdbcClient
                    .sql(
                            """
                            UPDATE todos SET title = :title, description = :description, category_id = :categoryId,
                                   start_date = :startDate, end_date = :endDate, is_completed = :completed,
                                   recurring_template_id = :recurringTemplateId,
                                   recurring_occurrence_date = :recurringOccurrenceDate,
                                   updated_at = :updatedAt, deleted_at = :deletedAt
                            WHERE user_id = :userId AND id = :id
                            """)
                    .param("title", todo.title())
                    .param("description", value(todo.description()))
                    .param("categoryId", todo.categoryId())
                    .param("startDate", todo.startDate())
                    .param("endDate", todo.endDate())
                    .param("completed", todo.completed() ? 1 : 0)
                    .param("recurringTemplateId", todo.recurringTemplateId())
                    .param("recurringOccurrenceDate", todo.recurringOccurrenceDate())
                    .param("updatedAt", timestamp(todo.updatedAt()))
                    .param("deletedAt", todo.deletedAt())
                    .param("userId", userId)
                    .param("id", id)
                    .update();
        } else {
            jdbcClient
                    .sql(
                            """
                            INSERT INTO todos (
                                id, user_id, title, description, category_id, start_date, end_date, is_completed,
                                recurring_template_id, recurring_occurrence_date, created_at, updated_at, deleted_at
                            ) VALUES (
                                :id, :userId, :title, :description, :categoryId, :startDate, :endDate, :completed,
                                :recurringTemplateId, :recurringOccurrenceDate, :createdAt, :updatedAt, :deletedAt
                            )
                            """)
                    .param("id", id)
                    .param("userId", userId)
                    .param("title", todo.title())
                    .param("description", value(todo.description()))
                    .param("categoryId", todo.categoryId())
                    .param("startDate", todo.startDate())
                    .param("endDate", todo.endDate())
                    .param("completed", todo.completed() ? 1 : 0)
                    .param("recurringTemplateId", todo.recurringTemplateId())
                    .param("recurringOccurrenceDate", todo.recurringOccurrenceDate())
                    .param("createdAt", timestamp(todo.createdAt()))
                    .param("updatedAt", timestamp(todo.updatedAt()))
                    .param("deletedAt", todo.deletedAt())
                    .update();
        }
    }

    private void upsertRecurringTemplate(
            String userId, SyncPayload.RecurringTemplatePayload template) {
        String id = idOrNew(template.id());
        if (exists("recurring_templates", userId, id)) {
            jdbcClient
                    .sql(
                            """
                            UPDATE recurring_templates SET title = :title, description = :description,
                                   category_id = :categoryId, repeat_start_date = :repeatStartDate,
                                   repeat_end_date = :repeatEndDate, repeat_type = :repeatType,
                                   weekly_days = :weeklyDays, monthly_day = :monthlyDay,
                                   period_length_days = :periodLengthDays,
                                   last_generated_until = :lastGeneratedUntil,
                                   updated_at = :updatedAt, deleted_at = :deletedAt
                            WHERE user_id = :userId AND id = :id
                            """)
                    .param("title", template.title())
                    .param("description", value(template.description()))
                    .param("categoryId", template.categoryId())
                    .param("repeatStartDate", template.repeatStartDate())
                    .param("repeatEndDate", template.repeatEndDate())
                    .param("repeatType", template.repeatType())
                    .param("weeklyDays", value(template.weeklyDays()))
                    .param("monthlyDay", template.monthlyDay())
                    .param("periodLengthDays", template.periodLengthDays())
                    .param("lastGeneratedUntil", template.lastGeneratedUntil())
                    .param("updatedAt", timestamp(template.updatedAt()))
                    .param("deletedAt", template.deletedAt())
                    .param("userId", userId)
                    .param("id", id)
                    .update();
        } else {
            jdbcClient
                    .sql(
                            """
                            INSERT INTO recurring_templates (
                                id, user_id, title, description, category_id, repeat_start_date, repeat_end_date,
                                repeat_type, weekly_days, monthly_day, period_length_days, last_generated_until,
                                created_at, updated_at, deleted_at
                            ) VALUES (
                                :id, :userId, :title, :description, :categoryId, :repeatStartDate, :repeatEndDate,
                                :repeatType, :weeklyDays, :monthlyDay, :periodLengthDays, :lastGeneratedUntil,
                                :createdAt, :updatedAt, :deletedAt
                            )
                            """)
                    .param("id", id)
                    .param("userId", userId)
                    .param("title", template.title())
                    .param("description", value(template.description()))
                    .param("categoryId", template.categoryId())
                    .param("repeatStartDate", template.repeatStartDate())
                    .param("repeatEndDate", template.repeatEndDate())
                    .param("repeatType", template.repeatType())
                    .param("weeklyDays", value(template.weeklyDays()))
                    .param("monthlyDay", template.monthlyDay())
                    .param("periodLengthDays", template.periodLengthDays())
                    .param("lastGeneratedUntil", template.lastGeneratedUntil())
                    .param("createdAt", timestamp(template.createdAt()))
                    .param("updatedAt", timestamp(template.updatedAt()))
                    .param("deletedAt", template.deletedAt())
                    .update();
        }
    }

    private boolean exists(String table, String userId, String id) {
        return jdbcClient
                        .sql(
                                "SELECT COUNT(*) FROM "
                                        + table
                                        + " WHERE user_id = :userId AND id = :id")
                        .param("userId", userId)
                        .param("id", id)
                        .query(Integer.class)
                        .single()
                > 0;
    }

    private String idOrNew(String id) {
        return id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
    }

    private String timestamp(String value) {
        return value == null || value.isBlank() ? Instant.now().toString() : value;
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
