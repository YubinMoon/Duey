package com.terry.duey.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.terry.duey.model.AppDate
import com.terry.duey.model.TodoItem
import com.terry.duey.ui.theme.MyTodoTheme
import com.terry.duey.ui.theme.SaturdayBlue
import com.terry.duey.ui.theme.SundayRed
import com.terry.duey.viewmodel.TodoViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.util.Calendar

@Composable
fun NewScheduleScreen(viewModel: TodoViewModel, onSaved: () -> Unit = {}) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("기본") }

    var startDate by remember { mutableStateOf(AppDate.today()) }
    var endDate by remember { mutableStateOf(AppDate.today()) }

    var showRangePicker by remember { mutableStateOf(false) }
    var showCategorySelect by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val voiceState by viewModel.voiceInputState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val voiceRecorder = remember { HoldVoiceRecorder() }
    var isRecordingVoice by remember { mutableStateOf(false) }
    val voiceLevels = remember { mutableStateListOf<Float>() }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) viewModel.clearVoiceInputState()
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceRecorder.cancel()
        }
    }

    LaunchedEffect(isRecordingVoice) {
        while (isRecordingVoice) {
            val level = voiceRecorder.currentLevel()
            if (voiceLevels.size >= VOICE_LEVEL_COUNT) {
                voiceLevels.removeAt(0)
            }
            voiceLevels.add(level)
            delay(80)
        }
    }

    LaunchedEffect(voiceState) {
        if (voiceState is TodoViewModel.VoiceInputUiState.DraftReady) {
            val draft = (voiceState as TodoViewModel.VoiceInputUiState.DraftReady).draft
            title = draft.title
            description = draft.description
            category = draft.category
            startDate = draft.startDate
            endDate = draft.endDate
        }
    }

    if (showRangePicker) {
        RangeDatePickerDialog(
            initialStart = startDate,
            initialEnd = endDate,
            onRangeSelected = { start, end ->
                startDate = start
                endDate = end
                showRangePicker = false
            },
            onDismiss = { showRangePicker = false },
        )
    }

    if (showCategorySelect) {
        CategorySelectionDialog(
            viewModel = viewModel,
            selectedCategory = category,
            onCategorySelected = {
                category = it
                showCategorySelect = false
            },
            onDismiss = { showCategorySelect = false },
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
            ) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "새 일정 만들기",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
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
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // 제목 입력
                    Column {
                        Text(
                            "제목",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("무엇을 해야 하나요?") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                        )
                    }

                    // 설명 입력
                    Column {
                        Text(
                            "설명",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("상세한 내용을 적어주세요 (선택)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 1,
                            maxLines = 5,
                        )
                    }

                    // 카테고리 선택
                    Column {
                        Text(
                            "카테고리",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { showCategorySelect = true }
                                .padding(16.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }

                    // 기간 선택
                    Column {
                        Text(
                            "기간",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { showRangePicker = true }
                                .padding(16.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "$startDate ~ $endDate",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Icon(
                                    imageVector = Icons.Filled.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        viewModel.addTodo(
                            TodoItem(
                                title = title.trim(),
                                description = description.trim(),
                                category = category,
                                startDate = startDate,
                                endDate = endDate,
                            ),
                        )
                        onSaved()
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(100),
                    enabled = title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text("일정 저장하기", style = MaterialTheme.typography.titleMedium)
                }
                Box(contentAlignment = Alignment.BottomCenter) {
                    if (isRecordingVoice) {
                        VoiceRecordingBubble(levels = voiceLevels)
                    }
                    FilledIconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("btn_voice_add_schedule")
                            .pointerInput(voiceState) {
                                detectTapGestures(
                                    onPress = {
                                        if (voiceState is TodoViewModel.VoiceInputUiState.Processing) return@detectTapGestures
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO,
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (!hasPermission) {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            return@detectTapGestures
                                        }

                                        voiceLevels.clear()
                                        if (!voiceRecorder.start(context)) {
                                            viewModel.clearVoiceInputState()
                                            return@detectTapGestures
                                        }
                                        isRecordingVoice = true
                                        val released = tryAwaitRelease()
                                        isRecordingVoice = false
                                        val recording = if (released) {
                                            voiceRecorder.stop()
                                        } else {
                                            voiceRecorder.cancel()
                                            null
                                        }
                                        if (recording != null && recording.durationMillis >= MIN_VOICE_RECORD_MILLIS) {
                                            viewModel.submitVoiceAudio(recording.file.readBytes(), recording.mimeType)
                                        } else {
                                            viewModel.clearVoiceInputState()
                                        }
                                        recording?.file?.delete()
                                    },
                                )
                            },
                        enabled = voiceState !is TodoViewModel.VoiceInputUiState.Processing,
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "음성으로 일정 입력")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (voiceState is TodoViewModel.VoiceInputUiState.Error) {
        AlertDialog(
            onDismissRequest = viewModel::clearVoiceInputState,
            confirmButton = { TextButton(onClick = viewModel::clearVoiceInputState) { Text("확인") } },
            title = { Text("음성 입력 오류") },
            text = { Text((voiceState as TodoViewModel.VoiceInputUiState.Error).message) },
        )
    }
}

private const val VOICE_LEVEL_COUNT = 18
private const val MIN_VOICE_RECORD_MILLIS = 500L

private data class VoiceRecording(
    val file: File,
    val mimeType: String,
    val durationMillis: Long,
)

private class HoldVoiceRecorder {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMillis: Long = 0L

    fun start(context: Context): Boolean {
        cancel()
        val file = File.createTempFile("duey_voice_", ".m4a", context.cacheDir)
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        return try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setAudioSamplingRate(44_100)
            mediaRecorder.setAudioEncodingBitRate(96_000)
            mediaRecorder.setOutputFile(file.absolutePath)
            mediaRecorder.prepare()
            mediaRecorder.start()
            recorder = mediaRecorder
            outputFile = file
            startedAtMillis = SystemClock.elapsedRealtime()
            true
        } catch (_: RuntimeException) {
            mediaRecorder.release()
            file.delete()
            false
        }
    }

    fun currentLevel(): Float {
        val amplitude = try {
            recorder?.maxAmplitude ?: 0
        } catch (_: RuntimeException) {
            0
        }
        return (amplitude / 32767f).coerceIn(0.05f, 1f)
    }

    fun stop(): VoiceRecording? {
        val mediaRecorder = recorder ?: return null
        val file = outputFile ?: return null
        val duration = SystemClock.elapsedRealtime() - startedAtMillis
        recorder = null
        outputFile = null
        return try {
            mediaRecorder.stop()
            mediaRecorder.release()
            VoiceRecording(file = file, mimeType = "audio/mp4", durationMillis = duration)
        } catch (_: RuntimeException) {
            mediaRecorder.release()
            file.delete()
            null
        }
    }

    fun cancel() {
        val mediaRecorder = recorder
        val file = outputFile
        recorder = null
        outputFile = null
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop()
            } catch (_: RuntimeException) {
            }
            mediaRecorder.release()
        }
        file?.delete()
    }
}

@Composable
private fun VoiceRecordingBubble(levels: List<Float>) {
    Surface(
        modifier = Modifier
            .padding(bottom = 68.dp)
            .width(172.dp)
            .height(52.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(VOICE_LEVEL_COUNT) { index ->
                val level = levels.getOrNull(index) ?: 0.08f
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height((8 + (24 * level)).dp)
                        .clip(RoundedCornerShape(100))
                        .background(MaterialTheme.colorScheme.onPrimaryContainer),
                )
            }
        }
    }
}

@Composable
fun CategoryAddDialog(
    onCategoryAdded: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
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
                    text = "새 카테고리 추가",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("카테고리 이름을 입력하세요") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            "취소",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { if (name.isNotBlank()) onCategoryAdded(name) },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(100),
                    ) {
                        Text("추가")
                    }
                }
            }
        }
    }
}

@Composable
internal fun RangeDatePickerDialog(
    initialStart: AppDate,
    initialEnd: AppDate,
    onRangeSelected: (AppDate, AppDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var currentYear by remember { mutableIntStateOf(initialStart.year) }
    var currentMonth by remember { mutableIntStateOf(initialStart.month) }
    var tempStart by remember { mutableStateOf<AppDate?>(initialStart) }
    var tempEnd by remember { mutableStateOf<AppDate?>(initialEnd) }
    var prevMonthIndex by remember { mutableIntStateOf(currentYear * 12 + currentMonth) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .pointerInput(Unit) {
                    var dragAccum = 0f
                    detectHorizontalDragGestures(onDragEnd = {
                        if (dragAccum < -80) {
                            prevMonthIndex = currentYear * 12 + currentMonth
                            if (currentMonth == 12) {
                                currentMonth = 1
                                currentYear++
                            } else {
                                currentMonth++
                            }
                        } else if (dragAccum > 80) {
                            prevMonthIndex = currentYear * 12 + currentMonth
                            if (currentMonth == 1) {
                                currentMonth = 12
                                currentYear--
                            } else {
                                currentMonth--
                            }
                        }
                        dragAccum = 0f
                    }) { _, dragAmount -> dragAccum += dragAmount }
                },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "기간 선택",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "시작일",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            tempStart?.toString() ?: "-",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        "→",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "종료일",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            tempEnd?.toString() ?: "-",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = {
                        prevMonthIndex = currentYear * 12 + currentMonth
                        if (currentMonth == 1) {
                            currentMonth = 12
                            currentYear--
                        } else {
                            currentMonth--
                        }
                    }) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = "${currentYear}년 ${currentMonth}월",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    IconButton(onClick = {
                        prevMonthIndex = currentYear * 12 + currentMonth
                        if (currentMonth == 12) {
                            currentMonth = 1
                            currentYear++
                        } else {
                            currentMonth++
                        }
                    }) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("일", "월", "화", "수", "목", "금", "토").forEachIndexed { index, day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = when (index) {
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
                AnimatedContent(targetState = currentYear to currentMonth, transitionSpec = {
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
                }, label = "dialog_month_transition") { (targetYear, targetMonth) ->
                    val cal = Calendar.getInstance()
                    cal.set(targetYear, targetMonth - 1, 1)
                    val firstDay = cal.get(Calendar.DAY_OF_WEEK) - 1
                    val daysInM = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val pCal = Calendar.getInstance()
                    pCal.set(
                        targetYear,
                        targetMonth - 1,
                        1,
                    )
                    pCal.add(Calendar.MONTH, -1)
                    val pY = pCal.get(Calendar.YEAR)
                    val pM = pCal.get(Calendar.MONTH) + 1
                    val pDays = pCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val nCal = Calendar.getInstance()
                    nCal.set(
                        targetYear,
                        targetMonth - 1,
                        1,
                    )
                    nCal.add(Calendar.MONTH, 1)
                    val nY = nCal.get(Calendar.YEAR)
                    val nM = nCal.get(Calendar.MONTH) + 1
                    val today = AppDate.today()
                    Column {
                        for (row in 0..5) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (col in 0..6) {
                                    val dayNum = row * 7 + col - firstDay + 1
                                    val isO = dayNum < 1 || dayNum > daysInM
                                    val date = when {
                                        dayNum < 1 -> AppDate(
                                            pY,
                                            pM,
                                            pDays + dayNum,
                                        )
                                        dayNum > daysInM -> AppDate(
                                            nY,
                                            nM,
                                            dayNum - daysInM,
                                        )
                                        else -> AppDate(targetYear, targetMonth, dayNum)
                                    }
                                    val isS = date == tempStart
                                    val isE = date == tempEnd
                                    val isInR =
                                        tempStart != null && tempEnd != null && date > tempStart!! && date < tempEnd!!
                                    val alpha = if (isO) 0.32f else 1f
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (isInR || (isS && tempEnd != null) || (isE && tempStart != null)) {
                                            val shape = when {
                                                isS && isE -> CircleShape
                                                isS -> RoundedCornerShape(
                                                    topStartPercent = 50,
                                                    bottomStartPercent = 50,
                                                )
                                                isE -> RoundedCornerShape(
                                                    topEndPercent = 50,
                                                    bottomEndPercent = 50,
                                                )
                                                else -> RoundedCornerShape(0)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(32.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f * alpha),
                                                        shape,
                                                    ),
                                            )
                                        }
                                        if (isS || isE) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.primary,
                                                        CircleShape,
                                                    ),
                                            )
                                        } else if (date == today) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                                                        CircleShape,
                                                    ),
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .clickable {
                                                    if (isO) {
                                                        prevMonthIndex =
                                                            currentYear * 12 + currentMonth
                                                        currentYear =
                                                            date.year
                                                        currentMonth = date.month
                                                    }
                                                    if (tempStart == null || (tempStart != null && tempEnd != null)) {
                                                        tempStart = date
                                                        tempEnd = null
                                                    } else if (date < tempStart!!) {
                                                        tempStart =
                                                            date
                                                    } else {
                                                        tempEnd = date
                                                    }
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = date.day.toString(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isS || isE || date == today) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isS || isE -> MaterialTheme.colorScheme.onPrimary
                                                    col == 0 -> SundayRed.copy(
                                                        alpha = alpha,
                                                    )
                                                    col == 6 -> SaturdayBlue.copy(alpha = alpha)
                                                    else -> MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = alpha,
                                                    )
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            "취소",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onRangeSelected(
                                tempStart ?: AppDate.today(),
                                tempEnd ?: (tempStart ?: AppDate.today()),
                            )
                        },
                        shape = RoundedCornerShape(100),
                        enabled = tempStart != null,
                    ) { Text("선택 완료") }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NewScheduleScreenPreview() {
    val startDate = AppDate.today()
    val endDate = startDate.addDays(3)

    MyTodoTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "새 일정 만들기",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
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
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Column {
                            Text(
                                "제목",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                            )
                            OutlinedTextField(
                                value = "중간고사 계획 정리",
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                            )
                        }
                        Column {
                            Text(
                                "설명",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                            )
                            OutlinedTextField(
                                value = "과목별 남은 분량과 복습 일정을 적어두기",
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 2,
                                maxLines = 5,
                            )
                        }
                        Column {
                            Text(
                                "카테고리",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(16.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "학업",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.List,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                        Column {
                            Text(
                                "기간",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(16.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "$startDate ~ $endDate",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.DateRange,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(40.dp))
                }

                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(100),
                ) {
                    Text("일정 저장하기", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
