package com.terry.duey.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.terry.duey.model.AppDate
import com.terry.duey.model.TodoItem
import com.terry.duey.ui.theme.MyTodoTheme
import com.terry.duey.ui.theme.SaturdayBlue
import com.terry.duey.ui.theme.SundayRed
import com.terry.duey.viewmodel.TodoViewModel
import java.util.Calendar

@Composable
fun ScheduleScreen(viewModel: TodoViewModel) {
    val today = remember { AppDate.today() }
    val todos by viewModel.todos.collectAsStateWithLifecycle()
    var detailTodoId by remember { mutableStateOf<Long?>(null) }

    if (detailTodoId != null) {
        val currentTodo = remember(detailTodoId, todos) {
            todos.find { it.id == detailTodoId }
        }
        if (currentTodo != null) {
            DetailDialog(
                todo = currentTodo,
                viewModel = viewModel,
                today = today,
                onDismiss = { detailTodoId = null },
            )
        } else {
            detailTodoId = null
        }
    }

    ScheduleContent(
        today = today,
        todos = todos,
        onOpenTodoDetail = { detailTodoId = it.id },
    )
}

@Composable
private fun ScheduleContent(
    today: AppDate,
    todos: List<TodoItem>,
    onOpenTodoDetail: (TodoItem) -> Unit,
) {
    var selectedDate by remember { mutableStateOf(today) }
    var calYear by remember { mutableIntStateOf(today.year) }
    var calMonth by remember { mutableIntStateOf(today.month) }
    var selectedTodoId by remember { mutableStateOf<Long?>(null) }

    var prevMonthIndex by remember { mutableIntStateOf(calYear * 12 + calMonth) }

    LaunchedEffect(selectedDate) { selectedTodoId = null }

    // Pre-calculate counts for the month to avoid O(N*T) filter in the loop
    val todoCounts by remember(calYear, calMonth, todos) {
        derivedStateOf {
            val counts = mutableMapOf<AppDate, Int>()

            // We only care about the range visible in the current view (roughly 1 month +/- a few days)
            // A simple approach is to calculate for all todos that overlap with the current year/month
            todos.forEach { todo ->
                if (!todo.isCompleted) {
                    var d = todo.startDate
                    while (d <= todo.endDate) {
                        // Only store counts for the current context to keep map small
                        if (d.year == calYear && (d.month == calMonth || d.month == calMonth - 1 || d.month == calMonth + 1)) {
                            counts[d] = (counts[d] ?: 0) + 1
                        }
                        d = d.addDays(1)
                        // Safety break for very long todos
                        if (d > todo.startDate.addDays(365)) break
                    }
                }
            }
            counts
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "내 일정",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "${calYear}년 ${calMonth}월",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(18.dp),
                    )
                    .pointerInput(Unit) {
                        var dragAccum = 0f
                        detectHorizontalDragGestures(onDragEnd = {
                            if (dragAccum < -80) {
                                prevMonthIndex = calYear * 12 + calMonth
                                val (ny, nm) = changeMonth(calYear, calMonth, 1)
                                calYear = ny
                                calMonth = nm
                            } else if (dragAccum > 80) {
                                prevMonthIndex = calYear * 12 + calMonth
                                val (ny, nm) = changeMonth(calYear, calMonth, -1)
                                calYear = ny
                                calMonth = nm
                            }
                            dragAccum = 0f
                        }) { _, dragAmount -> dragAccum += dragAmount }
                    }
                    .padding(16.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("일", "월", "화", "수", "목", "금", "토").forEachIndexed { idx, label ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = when (idx) {
                                0 -> SundayRed
                                6 -> SaturdayBlue
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.6f,
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                AnimatedContent(targetState = calYear to calMonth, transitionSpec = {
                    val targetIndex = targetState.first * 12 + targetState.second
                    val duration = 150
                    if (targetIndex > prevMonthIndex) {
                        (
                            slideInHorizontally(animationSpec = tween(duration)) { w -> w } + fadeIn(
                                tween(duration),
                            )
                            ).togetherWith(
                            slideOutHorizontally(animationSpec = tween(duration)) { w -> -w } + fadeOut(
                                tween(duration),
                            ),
                        )
                    } else {
                        (
                            slideInHorizontally(animationSpec = tween(duration)) { w -> -w } + fadeIn(
                                tween(duration),
                            )
                            ).togetherWith(
                            slideOutHorizontally(animationSpec = tween(duration)) { w -> w } + fadeOut(
                                tween(duration),
                            ),
                        )
                    }
                }, label = "month_transition") { (targetYear, targetMonth) ->
                    val firstDay = getFirstDayOfWeek(targetYear, targetMonth)
                    val daysInMonth = getDaysInMonth(targetYear, targetMonth)
                    val rows = (firstDay + daysInMonth + 6) / 7
                    val (pY, pM) = changeMonth(targetYear, targetMonth, -1)
                    val pDays = getDaysInMonth(pY, pM)
                    val (nY, nM) = changeMonth(targetYear, targetMonth, 1)

                    Column(modifier = Modifier.fillMaxWidth()) {
                        for (row in 0 until rows) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                            ) {
                                for (col in 0 until 7) {
                                    val dNum = row * 7 + col - firstDay + 1
                                    val isOverflow = dNum < 1 || dNum > daysInMonth
                                    val date = when {
                                        dNum < 1 -> AppDate(pY, pM, pDays + dNum)
                                        dNum > daysInMonth -> AppDate(nY, nM, dNum - daysInMonth)
                                        else -> AppDate(targetYear, targetMonth, dNum)
                                    }

                                    val currentSelectedTodo = if (selectedTodoId != null) {
                                        todos.find { it.id == selectedTodoId }
                                    } else {
                                        null
                                    }

                                    val isInRange = currentSelectedTodo?.let { date >= it.startDate && date <= it.endDate }
                                        ?: false

                                    val count = todoCounts[date] ?: 0

                                    DayCell(
                                        modifier = Modifier.weight(1f),
                                        dayNum = date.day,
                                        col = col,
                                        isToday = date == today,
                                        isSelected = date == selectedDate,
                                        isInRange = isInRange,
                                        roundLeft = isInRange && date == currentSelectedTodo.startDate,
                                        roundRight = isInRange && date == currentSelectedTodo.endDate,
                                        count = count,
                                        isOverflow = isOverflow,
                                        onClick = {
                                            selectedDate = date
                                            if (isOverflow) {
                                                prevMonthIndex = calYear * 12 + calMonth
                                                calYear = date.year
                                                calMonth = date.month
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 4.dp),
            ) {
                val dayTodos by remember(selectedDate, todos) {
                    derivedStateOf {
                        todos.filter { it.startDate <= selectedDate && selectedDate <= it.endDate }
                            .sortedWith(compareBy({ it.isCompleted }, { it.endDate }, { it.startDate }))
                    }
                }

                if (dayTodos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "일정이 없습니다",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                    ) {
                        items(dayTodos, key = { it.id }) { todo ->
                            ScheduleListItem(
                                todo = todo,
                                today = today,
                                isSelected = selectedTodoId == todo.id,
                                onClick = {
                                    if (selectedTodoId == todo.id) {
                                        onOpenTodoDetail(todo)
                                    } else {
                                        selectedTodoId = todo.id
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(
    modifier: Modifier,
    dayNum: Int,
    col: Int,
    isToday: Boolean,
    isSelected: Boolean,
    isInRange: Boolean,
    roundLeft: Boolean,
    roundRight: Boolean,
    count: Int,
    isOverflow: Boolean,
    onClick: () -> Unit,
) {
    val contentAlpha = if (isOverflow && !isSelected) 0.32f else 1f
    val rangeShape = RoundedCornerShape(
        topStartPercent = if (roundLeft) 50 else 0,
        bottomStartPercent = if (roundLeft) 50 else 0,
        topEndPercent = if (roundRight) 50 else 0,
        bottomEndPercent = if (roundRight) 50 else 0,
    )

    Box(modifier = modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
        if (isInRange) {
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), rangeShape),
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .then(
                    if (isSelected) {
                        Modifier.background(MaterialTheme.colorScheme.primary)
                    } else if (isToday) {
                        Modifier.border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha),
                            CircleShape,
                        )
                    } else {
                        Modifier
                    },
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((-14).dp, Alignment.CenterVertically),
            ) {
                Text(
                    text = dayNum.toString(),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        col == 0 -> SundayRed.copy(
                            alpha = contentAlpha,
                        )
                        col == 6 -> SaturdayBlue.copy(alpha = contentAlpha)
                        else -> MaterialTheme.colorScheme.onSurface.copy(
                            alpha = contentAlpha,
                        )
                    },
                )
                if (count > 0) {
                    Text(
                        text = count.toString(),
                        fontSize = 10.sp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = contentAlpha,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleListItem(
    todo: TodoItem,
    today: AppDate,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val remainingDays = today.daysUntil(todo.endDate)
    val selectionShape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                shape = selectionShape,
            )
            .clip(selectionShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    CircleShape,
                ),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = todo.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                color = if (todo.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
            )
            if (todo.description.isNotBlank()) {
                Text(
                    text = todo.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (todo.isCompleted) 0.5f else 1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (todo.isCompleted) {
                "완료"
            } else if (remainingDays < 0) {
                "초과"
            } else if (remainingDays == 0) {
                "오늘"
            } else {
                "D-$remainingDays"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (todo.isCompleted) {
                Color(0xFF4CAF50)
            } else if (remainingDays <= 2) {
                SundayRed
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
fun DetailDialog(todo: TodoItem, viewModel: TodoViewModel, today: AppDate, onDismiss: () -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf(todo.title) }
    var editDescription by remember { mutableStateOf(todo.description) }
    var editCategory by remember { mutableStateOf(todo.category) }
    var editStart by remember { mutableStateOf(todo.startDate) }
    var editEnd by remember { mutableStateOf(todo.endDate) }
    var showRangePicker by remember { mutableStateOf(false) }
    var showCategorySelect by remember { mutableStateOf(false) }

    if (showRangePicker) {
        RangeDatePickerDialog(
            initialStart = editStart,
            initialEnd = editEnd,
            onRangeSelected = { s, e ->
                editStart = s
                editEnd = e
                showRangePicker = false
            },
            onDismiss = { showRangePicker = false },
        )
    }
    if (showCategorySelect) {
        CategorySelectionDialog(
            viewModel = viewModel,
            selectedCategory = editCategory,
            onCategorySelected = {
                editCategory = it
                showCategorySelect = false
            },
            onDismiss = { showCategorySelect = false },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            if (isEditing) {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("제목") },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(todo.title, style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isEditing) {
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("설명") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 1,
                    )
                    OutlinedButton(
                        onClick = { showCategorySelect = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("카테고리: $editCategory") }
                    OutlinedButton(
                        onClick = { showRangePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("기간: $editStart ~ $editEnd") }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = todo.category,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = todo.description.ifBlank { "설명이 없습니다." },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider(thickness = 0.5.dp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "기간",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(60.dp),
                        )
                        Text(
                            "${todo.startDate} ~ ${todo.endDate}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    val rDays = today.daysUntil(todo.endDate)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "상태",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(60.dp),
                        )
                        Text(
                            if (todo.isCompleted) {
                                "완료됨"
                            } else if (rDays < 0) {
                                "기한 초과"
                            } else if (rDays == 0) {
                                "오늘 마감"
                            } else {
                                "D-$rDays"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (todo.isCompleted) {
                                Color(0xFF4CAF50)
                            } else if (rDays <= 2) {
                                SundayRed
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        viewModel.deleteTodo(todo.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("삭제") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isEditing) {
                        TextButton(onClick = { isEditing = false }) { Text("취소") }
                        Button(onClick = {
                            viewModel.updateTodo(
                                todo.copy(
                                    title = editTitle,
                                    description = editDescription,
                                    category = editCategory,
                                    startDate = editStart,
                                    endDate = editEnd,
                                ),
                            )
                            isEditing = false
                            onDismiss()
                        }, enabled = editTitle.isNotBlank()) { Text("저장") }
                    } else {
                        TextButton(onClick = {
                            viewModel.toggleTodoCompletion(todo.id)
                            onDismiss()
                        }) {
                            Text(
                                if (todo.isCompleted) "미완료" else "완료",
                            )
                        }
                        IconButton(onClick = { isEditing = true }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "수정",
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
    )
}

@Composable
fun CategorySelectionDialog(
    viewModel: TodoViewModel,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        CategoryAddDialog(onCategoryAdded = { newCat ->
            viewModel.addCategory(newCat)
            showAddDialog = false
            onCategorySelected(newCat)
        }, onDismiss = { showAddDialog = false })
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "카테고리 선택",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(20.dp))

                val cats by viewModel.categories.collectAsStateWithLifecycle()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp), // Reduced height
                ) {
                    items(cats) { cat ->
                        val isSelected = cat == selectedCategory
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    } else {
                                        Color.Transparent
                                    },
                                )
                                .clickable { onCategorySelected(cat) }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("새 카테고리 추가")
                }

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("닫기", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun getFirstDayOfWeek(year: Int, month: Int): Int {
    val cal = Calendar.getInstance()
    cal.set(
        year,
        month - 1,
        1,
    )
    return cal.get(Calendar.DAY_OF_WEEK) - 1
}

private fun getDaysInMonth(year: Int, month: Int): Int {
    val cal = Calendar.getInstance()
    cal.set(year, month - 1, 1)
    return cal.getActualMaximum(
        Calendar.DAY_OF_MONTH,
    )
}

private fun changeMonth(y: Int, m: Int, delta: Int): Pair<Int, Int> {
    val cal = Calendar.getInstance()
    cal.set(y, m - 1, 1)
    cal.add(
        Calendar.MONTH,
        delta,
    )
    return cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH) + 1
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ScheduleScreenPreview() {
    val today = AppDate.today()
    val selectedDate = today.addDays(1)
    val todos = listOf(
        TodoItem(
            id = 1,
            title = "Preview project meeting",
            description = "Review requirements and timeline",
            startDate = today,
            endDate = today.addDays(2),
            category = "Preview",
        ),
        TodoItem(
            id = 2,
            title = "Exercise",
            description = "5 km walk",
            startDate = selectedDate,
            endDate = selectedDate,
            category = "Personal",
        ),
        TodoItem(
            id = 3,
            title = "Reading",
            description = "Finish chapter 3",
            startDate = selectedDate,
            endDate = selectedDate.addDays(1),
            category = "Personal",
            isCompleted = true,
        ),
    )

    MyTodoTheme {
        ScheduleContent(
            today = today,
            todos = todos,
            onOpenTodoDetail = {},
        )
    }
}
