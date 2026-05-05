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

@Database(entities = [TodoItem::class, Category::class, RecurringTemplate::class], version = 3, exportSchema = false)
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

        private val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE categories_new (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT NOT NULL,
                            sortOrder INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                    db.execSQL("CREATE UNIQUE INDEX index_categories_name ON categories_new(name)")
                    db.execSQL(
                        """
                        INSERT INTO categories_new(name, sortOrder)
                        SELECT name,
                            CASE name
                                WHEN '기본' THEN 0
                                WHEN '학업' THEN 1
                                WHEN '개인' THEN 2
                                WHEN '운동' THEN 3
                                ELSE 1000
                            END AS sortOrder
                        FROM categories
                        GROUP BY name
                        ORDER BY sortOrder ASC, name ASC
                        """.trimIndent(),
                    )
                    db.execSQL("DROP TABLE categories")
                    db.execSQL("ALTER TABLE categories_new RENAME TO categories")

                    db.execSQL("DROP INDEX IF EXISTS index_todos_recurringTemplateId_recurringOccurrenceDate")
                    db.execSQL(
                        """
                        CREATE TABLE todos_new (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            title TEXT NOT NULL,
                            description TEXT NOT NULL,
                            startDate TEXT NOT NULL,
                            endDate TEXT NOT NULL,
                            isCompleted INTEGER NOT NULL,
                            categoryId INTEGER NOT NULL,
                            recurringTemplateId INTEGER,
                            recurringOccurrenceDate TEXT
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        INSERT INTO todos_new(
                            id, title, description, startDate, endDate, isCompleted,
                            categoryId, recurringTemplateId, recurringOccurrenceDate
                        )
                        SELECT
                            todos.id,
                            todos.title,
                            todos.description,
                            todos.startDate,
                            todos.endDate,
                            todos.isCompleted,
                            COALESCE(categories.id, (SELECT id FROM categories WHERE name = '기본' LIMIT 1), 1),
                            todos.recurringTemplateId,
                            todos.recurringOccurrenceDate
                        FROM todos
                        LEFT JOIN categories ON categories.name = todos.category
                        """.trimIndent(),
                    )
                    db.execSQL("DROP TABLE todos")
                    db.execSQL("ALTER TABLE todos_new RENAME TO todos")
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS index_todos_recurringTemplateId_recurringOccurrenceDate " +
                            "ON todos(recurringTemplateId, recurringOccurrenceDate)",
                    )

                    db.execSQL(
                        """
                        CREATE TABLE recurring_templates_new (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            title TEXT NOT NULL,
                            description TEXT NOT NULL,
                            categoryId INTEGER NOT NULL,
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
                    db.execSQL(
                        """
                        INSERT INTO recurring_templates_new(
                            id, title, description, categoryId, repeatStartDate, repeatEndDate,
                            repeatType, weeklyDays, monthlyDay, periodLengthDays, lastGeneratedUntil
                        )
                        SELECT
                            recurring_templates.id,
                            recurring_templates.title,
                            recurring_templates.description,
                            COALESCE(categories.id, (SELECT id FROM categories WHERE name = '기본' LIMIT 1), 1),
                            recurring_templates.repeatStartDate,
                            recurring_templates.repeatEndDate,
                            recurring_templates.repeatType,
                            recurring_templates.weeklyDays,
                            recurring_templates.monthlyDay,
                            recurring_templates.periodLengthDays,
                            recurring_templates.lastGeneratedUntil
                        FROM recurring_templates
                        LEFT JOIN categories ON categories.name = recurring_templates.category
                        """.trimIndent(),
                    )
                    db.execSQL("DROP TABLE recurring_templates")
                    db.execSQL("ALTER TABLE recurring_templates_new RENAME TO recurring_templates")
                }
            }

        fun getDatabase(context: Context): AppDatabase = instance ?: synchronized(this) {
            val database =
                Room
                    .databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "todo_database",
                    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
            instance = database
            database
        }
    }
}
