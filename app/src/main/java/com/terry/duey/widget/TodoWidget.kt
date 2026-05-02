package com.terry.duey.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.terry.duey.MainActivity
import com.terry.duey.data.AppDatabase
import com.terry.duey.model.AppDate
import com.terry.duey.model.TodoItem
import com.terry.duey.ui.theme.DueyGlanceColorScheme

private val TodoIdKey = ActionParameters.Key<Long>("todo_id")

class TodoWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),
            DpSize(180.dp, 140.dp),
            DpSize(280.dp, 180.dp),
            DpSize(320.dp, 260.dp),
        ),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val todoDao = AppDatabase.getDatabase(context).todoDao()
        val today = AppDate.today()

        provideContent {
            val todos by todoDao.getAllTodos().collectAsState(initial = emptyList())
            val todayTodos = todos
                .filter { it.startDate <= today && today <= it.endDate }
                .sortedWith(
                    compareBy<TodoItem> { it.isCompleted }
                        .thenBy { it.endDate }
                        .thenBy { it.startDate }
                        .thenBy { it.title },
                )

            GlanceTheme(colors = DueyGlanceColorScheme) {
                TodayTodoWidgetContent(
                    today = today,
                    todos = todayTodos,
                )
            }
        }
    }
}

@Composable
private fun TodayTodoWidgetContent(
    today: AppDate,
    todos: List<TodoItem>,
) {
    val widgetSize = LocalSize.current
    val compact = widgetSize.height < 140.dp || widgetSize.width < 160.dp
    val horizontalPadding = if (compact) 8.dp else 12.dp
    val verticalPadding = if (compact) 8.dp else 12.dp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    ) {
        WidgetHeader(todos = todos, compact = compact)

        Spacer(GlanceModifier.height(if (compact) 4.dp else 8.dp))

        if (todos.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "오늘 할 일이 없습니다.",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = if (compact) 12.sp else 14.sp,
                    ),
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(todos) { todo ->
                    WidgetTodoRow(
                        todo = todo,
                        remainingText = todo.remainingText(today),
                        compact = compact,
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetHeader(
    todos: List<TodoItem>,
    compact: Boolean,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (compact) "오늘" else "오늘의 할 일",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                color = GlanceTheme.colors.onSurface,
                fontSize = if (compact) 14.sp else 16.sp,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.defaultWeight())
        Text(
            text = "${todos.count { !it.isCompleted }}/${todos.size}",
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 12.sp else 14.sp,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun WidgetTodoRow(
    todo: TodoItem,
    remainingText: String,
    compact: Boolean,
) {
    val completedColor = GlanceTheme.colors.onSurfaceVariant
    val titleColor = if (todo.isCompleted) completedColor else GlanceTheme.colors.onSurface
    val statusColor = when {
        todo.isCompleted -> completedColor
        remainingText == "기한 초과" -> GlanceTheme.colors.error
        else -> GlanceTheme.colors.primary
    }

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 2.dp else 4.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CheckBox(
                checked = todo.isCompleted,
                onCheckedChange = actionRunCallback<ToggleTodoActionCallback>(
                    actionParametersOf(TodoIdKey to todo.id),
                ),
                text = "",
            )
            Spacer(GlanceModifier.width(if (compact) 2.dp else 4.dp))
            Text(
                text = todo.title,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = titleColor,
                    fontWeight = if (todo.isCompleted) FontWeight.Normal else FontWeight.Medium,
                    fontSize = if (compact) 12.sp else 14.sp,
                    textDecoration = if (todo.isCompleted) {
                        TextDecoration.LineThrough
                    } else {
                        TextDecoration.None
                    },
                ),
                maxLines = 1,
            )
            Spacer(GlanceModifier.width(if (compact) 6.dp else 10.dp))
            Text(
                text = remainingText,
                style = TextStyle(
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 11.sp else 12.sp,
                ),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.height(if (compact) 2.dp else 4.dp))
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(1.dp)
                .background(GlanceTheme.colors.surfaceVariant),
        ) {}
    }
}

private fun TodoItem.remainingText(today: AppDate): String {
    if (isCompleted) return "DONE"

    val remainingDays = today.daysUntil(endDate)
    return when {
        remainingDays < 0 -> "기한 초과"
        remainingDays == 0 -> "오늘까지"
        else -> "D-$remainingDays"
    }
}

class ToggleTodoActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val todoId = parameters[TodoIdKey] ?: return
        AppDatabase.getDatabase(context).todoDao().toggleCompletion(todoId)
        TodoWidget().update(context, glanceId)
    }
}

class TodoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodoWidget()
}
