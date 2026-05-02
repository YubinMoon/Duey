package com.terry.duey.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.terry.duey.model.AppDate
import com.terry.duey.model.TodoItem
import com.terry.duey.ui.theme.MyTodoTheme
import com.terry.duey.ui.theme.SundayRed
import com.terry.duey.viewmodel.TodoViewModel
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun MoreScreen(viewModel: TodoViewModel) {
    var activeSubScreen by remember { mutableStateOf<SubScreen?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (activeSubScreen) {
            SubScreen.AllSchedules -> AllSchedulesScreen(
                viewModel = viewModel,
                onBack = { activeSubScreen = null },
            )

            SubScreen.CategoryManagement -> CategoryManagementScreen(
                viewModel = viewModel,
                onBack = { activeSubScreen = null },
            )

            SubScreen.BackupRestore -> BackupRestoreScreen(
                viewModel = viewModel,
                onBack = { activeSubScreen = null },
            )

            null -> MoreMenuContent(
                onAllSchedulesClick = { activeSubScreen = SubScreen.AllSchedules },
                onCategoryManagementClick = { activeSubScreen = SubScreen.CategoryManagement },
                onBackupRestoreClick = { activeSubScreen = SubScreen.BackupRestore },
            )
        }
    }
}

private enum class SubScreen { AllSchedules, CategoryManagement, BackupRestore }

@Composable
private fun MoreMenuContent(
    onAllSchedulesClick: () -> Unit,
    onCategoryManagementClick: () -> Unit,
    onBackupRestoreClick: () -> Unit,
) {
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
                text = "설정",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(18.dp),
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(18.dp),
                )
                .clip(RoundedCornerShape(18.dp)),
        ) {
            MenuItem(
                title = "전체 일정 보기",
                icon = Icons.AutoMirrored.Filled.List,
                onClick = onAllSchedulesClick,
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                thickness = 0.5.dp,
            )
            MenuItem(
                title = "카테고리 관리",
                icon = Icons.Default.Category,
                onClick = onCategoryManagementClick,
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                thickness = 0.5.dp,
            )
            MenuItem(
                title = "백업 및 복구",
                icon = Icons.Default.Backup,
                onClick = onBackupRestoreClick,
            )
        }
    }
}

@Composable
private fun MenuItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllSchedulesScreen(viewModel: TodoViewModel, onBack: () -> Unit) {
    var isDeleteMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    var selectedTodoId by remember { mutableStateOf<Long?>(null) }

    val todos by viewModel.todos.collectAsStateWithLifecycle()
    val allTodos = remember(todos) { todos.sortedByDescending { it.startDate } }

    val handleBack = {
        if (isDeleteMode) {
            isDeleteMode = false
            selectedIds.clear()
        } else {
            onBack()
        }
    }
    BackHandler(onBack = handleBack)

    if (selectedTodoId != null) {
        val current = todos.find { it.id == selectedTodoId }
        if (current != null) {
            DetailDialog(
                todo = current,
                viewModel = viewModel,
                today = AppDate.today(),
                onDismiss = { selectedTodoId = null },
            )
        } else {
            selectedTodoId = null
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isDeleteMode) "${selectedIds.size}개 선택됨" else "전체 일정") },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                        )
                    }
                },
                actions = {
                    if (!isDeleteMode) {
                        IconButton(onClick = { isDeleteMode = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "삭제 모드",
                                tint = SundayRed,
                            )
                        }
                    } else {
                        TextButton(onClick = {
                            if (selectedIds.size == allTodos.size) {
                                selectedIds.clear()
                            } else {
                                selectedIds.clear()
                                selectedIds.addAll(allTodos.map { it.id })
                            }
                        }) {
                            Text(if (selectedIds.size == allTodos.size) "전체 해제" else "전체 선택")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (isDeleteMode) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Button(
                        onClick = {
                            viewModel.removeTodosByIds(selectedIds.toSet())
                            isDeleteMode = false
                            selectedIds.clear()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SundayRed),
                        enabled = selectedIds.isNotEmpty(),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text("선택한 일정 삭제하기", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
    ) { padding ->
        if (allTodos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("등록된 일정이 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 20.dp),
            ) {
                itemsIndexed(allTodos, key = { _, todo -> todo.id }) { index, todo ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                    AllScheduleRow(
                        todo = todo,
                        isDeleteMode = isDeleteMode,
                        isSelected = selectedIds.contains(todo.id),
                        onToggleSelection = {
                            if (selectedIds.contains(todo.id)) {
                                selectedIds.remove(todo.id)
                            } else {
                                selectedIds.add(todo.id)
                            }
                        },
                        onClick = {
                            if (isDeleteMode) {
                                if (selectedIds.contains(todo.id)) {
                                    selectedIds.remove(todo.id)
                                } else {
                                    selectedIds.add(todo.id)
                                }
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

@Composable
private fun AllScheduleRow(
    todo: TodoItem,
    isDeleteMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isDeleteMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() },
                colors = CheckboxDefaults.colors(checkedColor = SundayRed),
                modifier = Modifier.padding(end = 8.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (todo.isCompleted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF4CAF50),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (todo.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = 0.6f,
                        )
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "${todo.startDate} ~ ${todo.endDate}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(6.dp),
        ) {
            Text(
                text = todo.category,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryManagementScreen(viewModel: TodoViewModel, onBack: () -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<String?>(null) }
    var selectedCategoryForTodos by remember { mutableStateOf<String?>(null) }

    val categories by viewModel.categories.collectAsStateWithLifecycle()

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("카테고리 추가") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("이름") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addCategory(name)
                    showAddDialog = false
                }, enabled = name.isNotBlank()) { Text("추가") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("취소") } },
        )
    }

    if (editingCategory != null) {
        var name by remember { mutableStateOf(editingCategory!!) }
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("카테고리 수정") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("이름") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateCategory(
                        editingCategory!!,
                        name,
                    )
                    editingCategory = null
                }, enabled = name.isNotBlank() && name != editingCategory) { Text("수정") }
            },
            dismissButton = { TextButton(onClick = { editingCategory = null }) { Text("취소") } },
        )
    }

    if (selectedCategoryForTodos != null) {
        CategoryTodoListDialog(
            category = selectedCategoryForTodos!!,
            viewModel = viewModel,
            onDismiss = { selectedCategoryForTodos = null },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("카테고리 관리") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "추가",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(categories) { cat ->
                CategoryItemTile(
                    category = cat,
                    onEdit = { editingCategory = cat },
                    onDelete = { viewModel.deleteCategory(cat) },
                    onClick = { selectedCategoryForTodos = cat },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupRestoreScreen(viewModel: TodoViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            try {
                val json = viewModel.exportToJson()
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(json.toByteArray())
                }
                scope.launch {
                    snackbarHostState.showSnackbar("일정을 내보냈습니다.")
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("내보내기 실패: ${e.message}")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val content = reader.use { it.readText() }
                    val addedCount = viewModel.importFromJson(content)
                    if (addedCount >= 0) {
                        snackbarHostState.showSnackbar("${addedCount}개의 일정을 새로 가져왔습니다.")
                    } else {
                        snackbarHostState.showSnackbar("복구 실패: 잘못된 형식의 파일입니다.")
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("가져오기 실패: ${e.message}")
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("백업 및 복구") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "데이터 관리",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(18.dp),
                    )
                    .clip(RoundedCornerShape(18.dp)),
            ) {
                MenuItem(
                    title = "일정 내보내기 (JSON)",
                    icon = Icons.Default.FileUpload,
                    onClick = { exportLauncher.launch("my_todo_backup_${System.currentTimeMillis()}.json") },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    thickness = 0.5.dp,
                )
                MenuItem(
                    title = "일정 가져오기 (JSON)",
                    icon = Icons.Default.FileDownload,
                    onClick = {
                        importLauncher.launch(
                            arrayOf(
                                "application/json",
                                "application/octet-stream",
                            ),
                        )
                    },
                )
            }

            Text(
                "일정을 파일로 저장하거나, 저장된 파일에서 일정을 다시 불러올 수 있습니다. 가져오기 시 기존 일정과 중복되는 항목은 자동으로 제외됩니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

@Composable
private fun CategoryItemTile(
    category: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    val isDefault = category == "기본"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (!isDefault) {
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "수정",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "삭제",
                        modifier = Modifier.size(20.dp),
                        tint = SundayRed,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryTodoListDialog(
    category: String,
    viewModel: TodoViewModel,
    onDismiss: () -> Unit,
) {
    val todos by viewModel.todos.collectAsStateWithLifecycle()
    val categoryTodos = remember(todos, category) {
        todos.filter { it.category == category }.sortedBy { it.startDate }
    }
    var selectedTodoId by remember { mutableStateOf<Long?>(null) }

    if (selectedTodoId != null) {
        val current = todos.find { it.id == selectedTodoId }
        if (current != null) {
            DetailDialog(
                todo = current,
                viewModel = viewModel,
                today = AppDate.today(),
                onDismiss = { selectedTodoId = null },
            )
        } else {
            selectedTodoId = null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "[$category] 일정",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(16.dp))
                if (categoryTodos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("일정이 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categoryTodos) { todo ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTodoId = todo.id }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(
                                            if (todo.isCompleted) Color.Gray else MaterialTheme.colorScheme.primary,
                                            CircleShape,
                                        ),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = todo.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                        color = if (todo.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "${todo.startDate} ~ ${todo.endDate}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("닫기") }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MoreScreenPreview() {
    MyTodoTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            MoreMenuContent(
                onAllSchedulesClick = {},
                onCategoryManagementClick = {},
                onBackupRestoreClick = {},
            )
        }
    }
}
