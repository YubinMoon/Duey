package com.example.mytodo.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.mytodo.data.AppDatabase
import com.example.mytodo.data.DEFAULT_CATEGORIES
import com.example.mytodo.data.DEFAULT_CATEGORY
import com.example.mytodo.data.sampleTodos
import com.example.mytodo.model.AppDate
import com.example.mytodo.model.Category
import com.example.mytodo.model.TodoItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private data class TodoImportKey(
    val title: String,
    val description: String,
    val category: String,
    val startDate: AppDate,
    val endDate: AppDate,
    val isCompleted: Boolean,
)

private val todoListComparator =
    compareBy<TodoItem>({ it.startDate }, { it.endDate }, { it.title }, { it.id })

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val todoDao = database.todoDao()
    private val categoryDao = database.categoryDao()
    private val isDebugBuild = (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    val todos: StateFlow<List<TodoItem>> =
        todoDao.getAllTodos()
            .map { items -> items.sortedWith(todoListComparator) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    val categories: StateFlow<List<String>> =
        categoryDao.getAllCategories()
            .map { items -> sortCategories(items.map(Category::name)) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DEFAULT_CATEGORIES,
            )

    init {
        viewModelScope.launch {
            ensureDefaultCategories()
            seedDebugTodosIfEmpty()
        }
    }

    fun addTodo(item: TodoItem) {
        viewModelScope.launch {
            todoDao.insertTodo(item.normalized())
        }
    }

    fun updateTodo(updatedItem: TodoItem) {
        viewModelScope.launch {
            todoDao.updateTodo(updatedItem.normalized())
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

    fun getTodosForDate(date: AppDate): List<TodoItem> = todos.value.filter { it.startDate <= date && date <= it.endDate }

    fun addCategory(name: String) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) return

        viewModelScope.launch {
            categoryDao.insertCategory(Category(normalizedName))
        }
    }

    fun updateCategory(oldName: String, newName: String) {
        val normalizedName = newName.trim()
        if (oldName == DEFAULT_CATEGORY || normalizedName.isBlank()) return

        viewModelScope.launch {
            if (normalizedName in categories.value) return@launch

            database.withTransaction {
                categoryDao.updateCategoryName(oldName, normalizedName)
                todoDao.updateTodoCategory(oldName, normalizedName)
            }
        }
    }

    fun deleteCategory(name: String) {
        if (name == DEFAULT_CATEGORY) return

        viewModelScope.launch {
            database.withTransaction {
                todoDao.resetCategory(name, DEFAULT_CATEGORY)
                categoryDao.deleteCategory(name)
            }
        }
    }

    fun exportToJson(): String {
        val root =
            JSONObject().apply {
                put("categories", JSONArray().apply { categories.value.forEach(::put) })
                put(
                    "todos",
                    JSONArray().apply {
                        todos.value.forEach { todo ->
                            put(
                                JSONObject().apply {
                                    put("title", todo.title)
                                    put("description", todo.description)
                                    put("category", todo.category)
                                    put("startDate", todo.startDate.toStorageString())
                                    put("endDate", todo.endDate.toStorageString())
                                    put("isCompleted", todo.isCompleted)
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
        val importedTodos = root.optJSONArray("todos") ?: return 0
        val existingKeys = todos.value.mapTo(linkedSetOf(), TodoItem::toImportKey)
        var addedCount = 0

        database.withTransaction {
            if (importedCategories != null) {
                for (index in 0 until importedCategories.length()) {
                    val name = importedCategories.getString(index).trim()
                    categoryDao.insertCategory(Category(name))
                }
            }

            for (index in 0 until importedTodos.length()) {
                val item = importedTodos.getJSONObject(index).toTodoItem().normalized()
                val key = item.toImportKey()
                if (existingKeys.add(key)) {
                    todoDao.insertTodo(item)
                    addedCount++
                }
            }
        }

        addedCount
    } catch (_: Exception) {
        -1
    }

    private suspend fun ensureDefaultCategories() {
        if (categoryDao.getAllCategories().first().isNotEmpty()) return

        database.withTransaction {
            DEFAULT_CATEGORIES.forEach { category ->
                categoryDao.insertCategory(Category(category))
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

private fun sortCategories(categories: List<String>): List<String> {
    val defaultOrder = DEFAULT_CATEGORIES.withIndex().associate { it.value to it.index }
    return categories.sortedWith(
        compareBy<String>({ defaultOrder[it] ?: Int.MAX_VALUE }, { it }),
    )
}

private fun JSONObject.toTodoItem(): TodoItem = TodoItem(
    title = getString("title"),
    description = optString("description", ""),
    category = optString("category", DEFAULT_CATEGORY),
    startDate = AppDate.fromStorageString(getString("startDate")),
    endDate = AppDate.fromStorageString(getString("endDate")),
    isCompleted = optBoolean("isCompleted", false),
)

private fun TodoItem.normalized(): TodoItem = copy(
    title = title.trim(),
    description = description.trim(),
    category = category.trim().ifBlank { DEFAULT_CATEGORY },
)

private fun TodoItem.toImportKey(): TodoImportKey = TodoImportKey(
    title = title,
    description = description,
    category = category,
    startDate = startDate,
    endDate = endDate,
    isCompleted = isCompleted,
)
