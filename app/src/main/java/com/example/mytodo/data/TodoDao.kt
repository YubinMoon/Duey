package com.example.mytodo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.mytodo.model.TodoItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos")
    fun getAllTodos(): Flow<List<TodoItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoItem)

    @Update
    suspend fun updateTodo(todo: TodoItem)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteTodo(id: Long)

    @Query("DELETE FROM todos WHERE id IN (:ids)")
    suspend fun deleteTodosByIds(ids: List<Long>)

    @Query("UPDATE todos SET isCompleted = NOT isCompleted WHERE id = :id")
    suspend fun toggleCompletion(id: Long)

    @Query("UPDATE todos SET category = :newCategory WHERE category = :oldCategory")
    suspend fun updateTodoCategory(oldCategory: String, newCategory: String)

    @Query("UPDATE todos SET category = :defaultCategory WHERE category = :category")
    suspend fun resetCategory(category: String, defaultCategory: String)
}
