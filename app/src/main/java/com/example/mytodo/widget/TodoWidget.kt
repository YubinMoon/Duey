package com.example.mytodo.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.mytodo.data.AppDatabase
import com.example.mytodo.model.AppDate
import com.example.mytodo.model.TodoItem

class TodoWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getDatabase(context)
        val todoDao = database.todoDao()
        val today = AppDate.today()

        provideContent {
            val todos by todoDao.getAllTodos().collectAsState(initial = emptyList())
            val todayTodos = todos.filter { it.startDate <= today && today <= it.endDate }

            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .padding(12.dp),
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "오늘의 할 일",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 16.sp,
                            ),
                        )
                        Spacer(GlanceModifier.defaultWeight())
                        Text(
                            text = "${todayTodos.size}",
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }

                    Spacer(GlanceModifier.height(8.dp))

                    if (todayTodos.isEmpty()) {
                        Box(
                            modifier = GlanceModifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "오늘 일정이 없습니다.",
                                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                            )
                        }
                    } else {
                        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                            items(todayTodos) { todo ->
                                WidgetTodoRow(todo)
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetTodoRow(todo: TodoItem) {
    Column(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = GlanceModifier
                    .size(4.dp)
                    .background(if (todo.isCompleted) ColorProvider(android.graphics.Color.GRAY) else GlanceTheme.colors.primary),
            ) {}
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = todo.title,
                style = TextStyle(
                    color = if (todo.isCompleted) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.height(4.dp))
        Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(GlanceTheme.colors.onSurfaceVariant)) {}
    }
}

class TodoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodoWidget()
}
