package com.example.mytodo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mytodo.model.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: Category)

    @Query("DELETE FROM categories WHERE name = :name")
    suspend fun deleteCategory(name: String)

    @Query("UPDATE categories SET name = :newName WHERE name = :oldName")
    suspend fun updateCategoryName(oldName: String, newName: String)
}
