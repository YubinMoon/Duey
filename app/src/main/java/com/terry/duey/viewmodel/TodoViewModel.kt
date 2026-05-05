package com.terry.duey.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.terry.duey.ai.ParsedScheduleDraft
import com.terry.duey.ai.ScheduleVoiceParser
import com.terry.duey.data.AppDatabase
import com.terry.duey.data.DEFAULT_CATEGORIES
import com.terry.duey.data.DEFAULT_CATEGORY
import com.terry.duey.data.DEFAULT_CATEGORY_ID
import com.terry.duey.data.normalizedWeeklyDays
import com.terry.duey.data.sampleTodos
import com.terry.duey.data.syncRecurringTemplate
import com.terry.duey.model.AppDate
import com.terry.duey.model.Category
import com.terry.duey.model.RecurrenceTypes
import com.terry.duey.model.RecurringTemplate
import com.terry.duey.model.TodoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.terry.duey.data.syncRecurringSchedules as syncRecurringSchedulesForDatabase

private data class TodoImportKey(
    val title: String,
    val description: String,
    val categoryName: String,
    val startDate: AppDate,
    val endDate: AppDate,
    val isCompleted: Boolean,
)

private data class RecurringTemplateImportKey(
    val title: String,
    val description: String,
    val categoryName: String,
    val repeatStartDate: AppDate,
    val repeatEndDate: AppDate,
    val repeatType: String,
    val weeklyDays: String,
    val monthlyDay: Int,
    val periodLengthDays: Int,
)

private val todoListComparator =
    compareBy<TodoItem>({ it.startDate }, { it.endDate }, { it.title }, { it.id })

private val recurringTemplateComparator =
    compareBy<RecurringTemplate>({ it.repeatStartDate }, { it.title }, { it.id })

private val categoryComparator =
    compareBy<Category>({ it.sortOrder }, { it.name })

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    sealed interface VoiceInputUiState {
        data object Idle : VoiceInputUiState
        data object Processing : VoiceInputUiState
        data class DraftReady(val draft: ParsedScheduleDraft) : VoiceInputUiState
        data class Error(val message: String) : VoiceInputUiState
    }

    private val database = AppDatabase.getDatabase(application)
    private val todoDao = database.todoDao()
    private val categoryDao = database.categoryDao()
    private val recurringTemplateDao = database.recurringTemplateDao()
    private val isDebugBuild = (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    private val voiceParser = ScheduleVoiceParser()
    private val _voiceInputState = MutableStateFlow<VoiceInputUiState>(VoiceInputUiState.Idle)
    val voiceInputState: StateFlow<VoiceInputUiState> = _voiceInputState

    val todos: StateFlow<List<TodoItem>> =
        todoDao.getAllTodos()
            .map { items -> items.sortedWith(todoListComparator) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    val categories: StateFlow<List<Category>> =
        categoryDao.getAllCategories()
            .map { items -> items.sortedWith(categoryComparator) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = defaultCategoryModels(),
            )

    val recurringTemplates: StateFlow<List<RecurringTemplate>> =
        recurringTemplateDao.getAllTemplates()
            .map { items -> items.sortedWith(recurringTemplateComparator) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    init {
        viewModelScope.launch {
            ensureDefaultCategories()
            syncRecurringSchedulesForDatabase(database)
            seedDebugTodosIfEmpty()
        }
    }

    fun addTodo(item: TodoItem) {
        viewModelScope.launch {
            todoDao.insertTodo(item.normalized())
        }
    }

    fun submitVoiceAudio(audioBytes: ByteArray, mimeType: String) {
        if (audioBytes.isEmpty()) {
            _voiceInputState.value = VoiceInputUiState.Error("음성 데이터가 비어 있습니다.")
            return
        }
        _voiceInputState.value = VoiceInputUiState.Processing
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { voiceParser.parseAudio(audioBytes, mimeType) }
            _voiceInputState.value = result.fold(
                onSuccess = { VoiceInputUiState.DraftReady(it) },
                onFailure = { VoiceInputUiState.Error(it.message ?: "음성 일정 변환에 실패했습니다.") },
            )
        }
    }

    fun clearVoiceInputState() {
        _voiceInputState.value = VoiceInputUiState.Idle
    }

    fun updateTodo(updatedItem: TodoItem) {
        viewModelScope.launch {
            todoDao.updateTodo(
                updatedItem
                    .normalized()
                    .copy(recurringTemplateId = null, recurringOccurrenceDate = null),
            )
        }
    }

    fun removeTodosByIds(idsToRemove: Set<Long>) {
        if (idsToRemove.isEmpty()) return

        viewModelScope.launch {
            todoDao.deleteTodosByIds(idsToRemove.toList())
        }
    }

    fun toggleTodoCompletion(id: Long) {
        viewModelScope.launch {
            todoDao.toggleCompletion(id)
        }
    }

    fun deleteTodo(id: Long) {
        viewModelScope.launch {
            todoDao.deleteTodo(id)
        }
    }

    fun syncRecurringSchedules() {
        viewModelScope.launch {
            syncRecurringSchedulesForDatabase(database)
        }
    }

    fun addRecurringTemplate(template: RecurringTemplate) {
        viewModelScope.launch {
            database.withTransaction {
                val normalizedTemplate = template.normalized().copy(id = 0, lastGeneratedUntil = null)
                val id = recurringTemplateDao.insertTemplate(normalizedTemplate)
                syncRecurringTemplate(database, normalizedTemplate.copy(id = id))
            }
        }
    }

    fun deleteRecurringTemplate(templateId: Long) {
        viewModelScope.launch {
            database.withTransaction {
                todoDao.detachCompletedTodosByTemplateId(templateId)
                todoDao.deleteIncompleteTodosByTemplateId(templateId)
                recurringTemplateDao.deleteTemplate(templateId)
            }
        }
    }

    fun categoryName(categoryId: Long): String = categories.value.firstOrNull { it.id == categoryId }?.name ?: DEFAULT_CATEGORY

    fun categoryIdForName(name: String): Long = categories.value.firstOrNull { it.name == name.trim() }?.id ?: DEFAULT_CATEGORY_ID

    fun addCategory(name: String, onAdded: (Category) -> Unit = {}) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) return

        viewModelScope.launch {
            if (categories.value.any { it.name == normalizedName }) return@launch

            val nextSortOrder = (categories.value.maxOfOrNull { it.sortOrder } ?: -1) + 1
            val category = Category(name = normalizedName, sortOrder = nextSortOrder)
            val id = categoryDao.insertCategory(category)
            if (id > 0L) {
                onAdded(category.copy(id = id))
            }
        }
    }

    fun updateCategory(categoryId: Long, newName: String) {
        val normalizedName = newName.trim()
        val current = categories.value.firstOrNull { it.id == categoryId } ?: return
        if (current.name == DEFAULT_CATEGORY || normalizedName.isBlank()) return

        viewModelScope.launch {
            if (categories.value.any { it.id != categoryId && it.name == normalizedName }) return@launch

            categoryDao.updateCategoryName(categoryId, normalizedName)
        }
    }

    fun deleteCategory(categoryId: Long, moveToCategoryId: Long = DEFAULT_CATEGORY_ID) {
        val current = categories.value.firstOrNull { it.id == categoryId } ?: return
        if (current.name == DEFAULT_CATEGORY || categoryId == moveToCategoryId) return

        viewModelScope.launch {
            database.withTransaction {
                todoDao.moveTodosToCategory(categoryId, moveToCategoryId)
                recurringTemplateDao.moveTemplatesToCategory(categoryId, moveToCategoryId)
                categoryDao.deleteCategory(categoryId)
            }
        }
    }

    fun reorderCategories(orderedCategories: List<Category>) {
        viewModelScope.launch {
            categoryDao.updateCategories(
                orderedCategories.mapIndexed { index, category ->
                    category.copy(sortOrder = index)
                },
            )
        }
    }

    fun exportToJson(): String {
        val root =
            JSONObject().apply {
                put(
                    "categories",
                    JSONArray().apply {
                        categories.value.forEach { category ->
                            put(
                                JSONObject().apply {
                                    put("name", category.name)
                                    put("sortOrder", category.sortOrder)
                                },
                            )
                        }
                    },
                )
                put(
                    "todos",
                    JSONArray().apply {
                        todos.value.forEach { todo ->
                            put(
                                JSONObject().apply {
                                    put("title", todo.title)
                                    put("description", todo.description)
                                    put("category", categoryName(todo.categoryId))
                                    put("startDate", todo.startDate.toStorageString())
                                    put("endDate", todo.endDate.toStorageString())
                                    put("isCompleted", todo.isCompleted)
                                    if (todo.recurringTemplateId != null) {
                                        put("recurringTemplateId", todo.recurringTemplateId)
                                    }
                                    if (todo.recurringOccurrenceDate != null) {
                                        put("recurringOccurrenceDate", todo.recurringOccurrenceDate.toStorageString())
                                    }
                                },
                            )
                        }
                    },
                )
                put(
                    "recurringTemplates",
                    JSONArray().apply {
                        recurringTemplates.value.forEach { template ->
                            put(
                                JSONObject().apply {
                                    put("id", template.id)
                                    put("title", template.title)
                                    put("description", template.description)
                                    put("category", categoryName(template.categoryId))
                                    put("repeatStartDate", template.repeatStartDate.toStorageString())
                                    put("repeatEndDate", template.repeatEndDate.toStorageString())
                                    put("repeatType", template.repeatType)
                                    put("weeklyDays", template.weeklyDays)
                                    put("monthlyDay", template.monthlyDay)
                                    put("periodLengthDays", template.periodLengthDays)
                                    if (template.lastGeneratedUntil != null) {
                                        put("lastGeneratedUntil", template.lastGeneratedUntil.toStorageString())
                                    }
                                },
                            )
                        }
                    },
                )
            }

        return root.toString(4)
    }

    suspend fun importFromJson(jsonString: String): Int = try {
        val root = JSONObject(jsonString)
        val importedCategories = root.optJSONArray("categories")
        val importedTemplates = root.optJSONArray("recurringTemplates")
        val importedTodos = root.optJSONArray("todos")
        val existingKeys = todos.value.mapTo(linkedSetOf()) { todo -> todo.toImportKey(::categoryName) }
        val templateIdMap = mutableMapOf<Long, Long>()
        var addedCount = 0

        database.withTransaction {
            if (importedCategories != null) {
                val existingCategoryNames = categoryDao.getCategoriesSnapshot().mapTo(mutableSetOf(), Category::name)
                var nextSortOrder = (categoryDao.getCategoriesSnapshot().maxOfOrNull { it.sortOrder } ?: -1) + 1
                for (index in 0 until importedCategories.length()) {
                    val raw = importedCategories.get(index)
                    val name = when (raw) {
                        is JSONObject -> raw.optString("name", "")
                        else -> raw.toString()
                    }.trim()
                    if (name.isNotBlank() && existingCategoryNames.add(name)) {
                        val sortOrder = (raw as? JSONObject)?.optInt("sortOrder", nextSortOrder) ?: nextSortOrder
                        categoryDao.insertCategory(Category(name = name, sortOrder = sortOrder))
                        nextSortOrder = maxOf(nextSortOrder, sortOrder + 1)
                    }
                }
            }

            val categoryIdsByName = categoryDao.getCategoriesSnapshot().associate { it.name to it.id }

            if (importedTemplates != null) {
                val existingTemplates = recurringTemplateDao.getTemplatesSnapshot()
                val existingTemplateIdsByKey =
                    existingTemplates.associate { template -> template.toImportKey(::categoryName) to template.id }

                for (index in 0 until importedTemplates.length()) {
                    val source = importedTemplates.getJSONObject(index)
                    val originalId = source.optLong("id", 0L)
                    val template = source.toRecurringTemplate(categoryIdsByName).normalized()
                    val existingId = existingTemplateIdsByKey[template.toImportKey(::categoryName)]
                    val mappedId = existingId ?: recurringTemplateDao.insertTemplate(template.copy(id = 0))
                    if (originalId > 0L) {
                        templateIdMap[originalId] = mappedId
                    }
                }
            }

            if (importedTodos != null) {
                for (index in 0 until importedTodos.length()) {
                    val item = importedTodos.getJSONObject(index).toTodoItem(templateIdMap, categoryIdsByName).normalized()
                    val key = item.toImportKey(::categoryName)
                    if (existingKeys.add(key)) {
                        todoDao.insertTodo(item)
                        addedCount++
                    }
                }
            }
        }

        syncRecurringSchedulesForDatabase(database)

        addedCount
    } catch (_: Exception) {
        -1
    }

    private suspend fun ensureDefaultCategories() {
        if (categoryDao.getAllCategories().first().isNotEmpty()) return

        database.withTransaction {
            defaultCategoryModels().forEach { category ->
                categoryDao.insertCategory(category)
            }
        }
    }

    private suspend fun seedDebugTodosIfEmpty() {
        if (!isDebugBuild || todoDao.getAllTodos().first().isNotEmpty()) return

        sampleTodos(AppDate.today()).forEach { todo ->
            todoDao.insertTodo(todo)
        }
    }
}

private fun defaultCategoryModels(): List<Category> = DEFAULT_CATEGORIES.mapIndexed { index, name ->
    Category(id = index + 1L, name = name, sortOrder = index)
}

private fun JSONObject.toTodoItem(
    templateIdMap: Map<Long, Long> = emptyMap(),
    categoryIdsByName: Map<String, Long> = emptyMap(),
): TodoItem {
    val importedTemplateId = if (has("recurringTemplateId")) optLong("recurringTemplateId", 0L) else 0L
    val mappedTemplateId = templateIdMap[importedTemplateId]
    val occurrenceDate = optString("recurringOccurrenceDate", "")

    return TodoItem(
        title = getString("title"),
        description = optString("description", ""),
        categoryId = categoryIdsByName[optString("category", DEFAULT_CATEGORY).trim()] ?: DEFAULT_CATEGORY_ID,
        startDate = AppDate.fromStorageString(getString("startDate")),
        endDate = AppDate.fromStorageString(getString("endDate")),
        isCompleted = optBoolean("isCompleted", false),
        recurringTemplateId = mappedTemplateId,
        recurringOccurrenceDate = occurrenceDate.takeIf(String::isNotBlank)?.let(AppDate::fromStorageString),
    )
}

private fun JSONObject.toRecurringTemplate(categoryIdsByName: Map<String, Long> = emptyMap()): RecurringTemplate = RecurringTemplate(
    title = getString("title"),
    description = optString("description", ""),
    categoryId = categoryIdsByName[optString("category", DEFAULT_CATEGORY).trim()] ?: DEFAULT_CATEGORY_ID,
    repeatStartDate = AppDate.fromStorageString(getString("repeatStartDate")),
    repeatEndDate = AppDate.fromStorageString(getString("repeatEndDate")),
    repeatType = optString("repeatType", RecurrenceTypes.DAILY),
    weeklyDays = optString("weeklyDays", ""),
    monthlyDay = optInt("monthlyDay", 1),
    periodLengthDays = optInt("periodLengthDays", 1),
    lastGeneratedUntil = optString("lastGeneratedUntil", "")
        .takeIf(String::isNotBlank)
        ?.let(AppDate::fromStorageString),
)

private fun TodoItem.normalized(): TodoItem = copy(
    title = title.trim(),
    description = description.trim(),
)

private fun RecurringTemplate.normalized(): RecurringTemplate {
    val cleanType = when (repeatType) {
        RecurrenceTypes.WEEKLY -> RecurrenceTypes.WEEKLY
        RecurrenceTypes.MONTHLY -> RecurrenceTypes.MONTHLY
        else -> RecurrenceTypes.DAILY
    }
    val cleanStart = repeatStartDate
    val cleanEnd = maxOf(repeatEndDate, cleanStart)

    return copy(
        title = title.trim(),
        description = description.trim(),
        repeatStartDate = cleanStart,
        repeatEndDate = cleanEnd,
        repeatType = cleanType,
        weeklyDays = normalizedWeeklyDays(),
        monthlyDay = monthlyDay.coerceIn(1, 31),
        periodLengthDays = periodLengthDays.coerceAtLeast(1),
    )
}

private fun TodoItem.toImportKey(categoryNameForId: (Long) -> String): TodoImportKey = TodoImportKey(
    title = title,
    description = description,
    categoryName = categoryNameForId(categoryId),
    startDate = startDate,
    endDate = endDate,
    isCompleted = isCompleted,
)

private fun RecurringTemplate.toImportKey(categoryNameForId: (Long) -> String): RecurringTemplateImportKey = RecurringTemplateImportKey(
    title = title,
    description = description,
    categoryName = categoryNameForId(categoryId),
    repeatStartDate = repeatStartDate,
    repeatEndDate = repeatEndDate,
    repeatType = repeatType,
    weeklyDays = normalizedWeeklyDays(),
    monthlyDay = monthlyDay,
    periodLengthDays = periodLengthDays,
)
