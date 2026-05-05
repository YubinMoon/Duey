package com.terry.duey.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.terry.duey.model.AppDate
import com.terry.duey.model.RecurringTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTemplateDao {
    @Query("SELECT * FROM recurring_templates")
    fun getAllTemplates(): Flow<List<RecurringTemplate>>

    @Query("SELECT * FROM recurring_templates")
    suspend fun getTemplatesSnapshot(): List<RecurringTemplate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: RecurringTemplate): Long

    @Query("DELETE FROM recurring_templates WHERE id = :id")
    suspend fun deleteTemplate(id: Long)

    @Query("UPDATE recurring_templates SET lastGeneratedUntil = :lastGeneratedUntil WHERE id = :id")
    suspend fun updateLastGeneratedUntil(id: Long, lastGeneratedUntil: AppDate)

    @Query("UPDATE recurring_templates SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategory(categoryId: Long)
}
