package com.terry.duey.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.terry.duey.model.Category
import com.terry.duey.model.RecurringTemplate
import com.terry.duey.model.TodoItem

@Database(entities = [TodoItem::class, Category::class, RecurringTemplate::class], version = 2, exportSchema = false)
@TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringTemplateDao(): RecurringTemplateDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE todos ADD COLUMN recurringTemplateId INTEGER DEFAULT NULL")
                    db.execSQL("ALTER TABLE todos ADD COLUMN recurringOccurrenceDate TEXT DEFAULT NULL")
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS index_todos_recurringTemplateId_recurringOccurrenceDate " +
                            "ON todos(recurringTemplateId, recurringOccurrenceDate)",
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS recurring_templates (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            title TEXT NOT NULL,
                            description TEXT NOT NULL,
                            category TEXT NOT NULL,
                            repeatStartDate TEXT NOT NULL,
                            repeatEndDate TEXT NOT NULL,
                            repeatType TEXT NOT NULL,
                            weeklyDays TEXT NOT NULL,
                            monthlyDay INTEGER NOT NULL,
                            periodLengthDays INTEGER NOT NULL,
                            lastGeneratedUntil TEXT
                        )
                        """.trimIndent(),
                    )
                }
            }

        fun getDatabase(context: Context): AppDatabase = instance ?: synchronized(this) {
            val database =
                Room
                    .databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "todo_database",
                    ).addMigrations(MIGRATION_1_2)
                    .build()
            instance = database
            database
        }
    }
}
