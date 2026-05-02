package com.terry.duey.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.terry.duey.model.AppDate
import com.terry.duey.model.TodoItem
import com.terry.duey.ui.theme.MyTodoTheme
import com.terry.duey.viewmodel.TodoViewModel
import kotlinx.coroutines.launch

private const val PULL_THRESHOLD = 120f

private data class DateGroup(val date: AppDate, val todos: List<TodoItem>)

@Composable
fun HomeworkScreen(viewModel: TodoViewModel) {
    val today = remember { AppDate.today() }
    val maxDate = remember { today.addDays(182) }

    val todos by viewModel.todos.collectAsStateWithLifecycle()

    // pagination state
    var visibleDays by remember { mutableIntStateOf(10) }

    // Derive groups efficiently
    val groups by remember(visibleDays, todos) {
        derivedStateOf {
            val dateToTodos = mutableMapOf<AppDate, MutableList<TodoItem>>()

            // Consider all todos and map them to dates within our window
            todos.forEach { todo ->
                // Start checking from today or the todo's start date, whichever is later
                var d = if (todo.startDate < today) today else todo.startDate
                val end = if (todo.endDate > maxDate) maxDate else todo.endDate

                while (d <= end) {
                    dateToTodos.getOrPut(d) { mutableListOf() }.add(todo)
                    d = d.addDays(1)
                }
            }

            // Convert to sorted groups and take the requested number of days
            dateToTodos.entries
                .asSequence()
                .map { (date, todos) ->
                    DateGroup(
                        date,
                        todos.sortedWith(
                            compareBy(
                                { it.isCompleted },
                                { it.endDate },
                                { it.startDate },
                            ),
                        ),
                    )
                }
                .sortedBy { it.date }
                .take(visibleDays)
                .toList()
        }
    }

    var selectedTodoId by remember { mutableStateOf<Long?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()

    if (showDetailDialog && selectedTodoId != null) {
        val currentTodo = remember(selectedTodoId, todos) {
            todos.find { it.id == selectedTodoId }
        }
        if (currentTodo != null) {
            DetailDialog(
                todo = currentTodo,
                viewModel = viewModel,
                today = today,
                onDismiss = { showDetailDialog = false },
            )
        } else {
            showDetailDialog = false
        }
    }

    HomeworkContent(
        today = today,
        groups = groups,
        lazyListState = lazyListState,
        onTodayClick = {
            visibleDays = 10
        },
        onLoadMore = {
            visibleDays += 5
        },
        onTodoClick = {
            selectedTodoId = it.id
            showDetailDialog = true
        },
        onToggleComplete = {
            viewModel.toggleTodoCompletion(it.id)
        },
    )
}

@Composable
private fun HomeworkContent(
    today: AppDate,
    groups: List<DateGroup>,
    lazyListState: LazyListState,
    onTodayClick: () -> Unit,
    onLoadMore: () -> Unit,
    onTodoClick: (TodoItem) -> Unit,
    onToggleComplete: (TodoItem) -> Unit,
) {
    val pullProgressState = remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset = if (available.y < 0) {
                pullProgressState.floatValue =
                    (pullProgressState.floatValue + (-available.y)).coerceAtMost(PULL_THRESHOLD * 1.5f)
                available
            } else {
                Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullProgressState.floatValue >= PULL_THRESHOLD) {
                    onLoadMore()
                }
                pullProgressState.floatValue = 0f
                return Velocity.Zero
            }
        }
    }

    // Infinite scroll logic - triggered only when reaching near the end
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { lastIndex ->
                val totalItems = lazyListState.layoutInfo.totalItemsCount
                if (totalItems > 0 && lastIndex >= totalItems - 2) {
                    onLoadMore()
                }
            }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "오늘의 숙제",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    ),
                    modifier = Modifier.padding(0.dp),
                ) {
                    Box(
                        modifier = Modifier.clickable {
                            onTodayClick()
                            scope.launch { lazyListState.animateScrollToItem(0) }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "오늘",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection),
            ) {
                if (groups.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "현재 등록된 숙제가 없습니다",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                    ) {
                        items(
                            items = groups,
                            key = { "${it.date.year}_${it.date.month}_${it.date.day}" },
                            contentType = { "date_group" },
                        ) { group ->
                            DateGroupTile(
                                date = group.date,
                                todos = group.todos,
                                today = today,
                                onTodoClick = onTodoClick,
                                onToggleComplete = onToggleComplete,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateGroupTile(
    date: AppDate,
    todos: List<TodoItem>,
    today: AppDate,
    onTodoClick: (TodoItem) -> Unit,
    onToggleComplete: (TodoItem) -> Unit,
) {
    val daysFromToday = today.daysUntil(date)
    val relativeLabel = when (daysFromToday) {
        0 -> "오늘"
        1 -> "내일"
        2 -> "모레"
        else -> "${daysFromToday}일 후"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(18.dp),
            )
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = relativeLabel,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (daysFromToday == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${date.month}월 ${date.day}일 (${date.dayOfWeekLabel()})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = "${todos.count { !it.isCompleted }}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        todos.forEachIndexed { index, todo ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            }
            TodoRow(
                todo = todo,
                today = today,
                onToggleComplete = { onToggleComplete(todo) },
                onClick = { onTodoClick(todo) },
            )
        }
    }
}

@Composable
private fun TodoRow(
    todo: TodoItem,
    today: AppDate,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val remainingDays = today.daysUntil(todo.endDate)
    val statusColor = when {
        todo.isCompleted -> Color(0xFF4CAF50)
        remainingDays <= 0 -> Color(0xFFE30000) // Red for overdue and due today
        remainingDays <= 2 -> Color(0xFFFF9500) // Orange for upcoming
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = todo.isCompleted,
            onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleComplete()
            },
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = todo.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (todo.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            )
            if (todo.description.isNotBlank()) {
                Text(
                    text = todo.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (todo.isCompleted) 0.5f else 1f),
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = if (todo.isCompleted) {
                "완료"
            } else if (remainingDays < 0) {
                "기한 초과"
            } else if (remainingDays == 0) {
                "오늘까지"
            } else {
                "D-$remainingDays"
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = statusColor,
        )
    }
}

@Preview(showBackground = true)
// @Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeworkScreenPreview() {
    val today = AppDate.today()
    val mockTodos = listOf(
        TodoItem(
            id = 1,
            title = "수학 숙제",
            description = "기하와 벡터 12p",
            startDate = today,
            endDate = today,
            category = "학업"
        ),
        TodoItem(
            id = 2,
            title = "영어 단어",
            description = "VOCA 2000",
            startDate = today,
            endDate = today.addDays(1),
            category = "학업"
        ),
        TodoItem(
            id = 3,
            title = "장보기",
            description = "우유, 사과",
            startDate = today.addDays(1),
            endDate = today.addDays(1),
            category = "개인"
        ),
        TodoItem(
            id = 4,
            title = "운동하기",
            description = "조깅 5km",
            startDate = today,
            endDate = today.addDays(3),
            category = "운동",
            isCompleted = true
        ),
    )

    val groups = listOf(
        DateGroup(today, mockTodos.filter { it.startDate <= today && today <= it.endDate }),
        DateGroup(
            today.addDays(1),
            mockTodos.filter { it.startDate <= today.addDays(1) && today.addDays(1) <= it.endDate }),
    )

    MyTodoTheme {
        HomeworkContent(
            today = today,
            groups = groups,
            lazyListState = rememberLazyListState(),
            onTodayClick = {},
            onLoadMore = {},
            onTodoClick = {},
            onToggleComplete = {},
        )
    }
}
