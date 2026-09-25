package com.example.ui.memories

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MahiMemoryRepository
import com.example.data.TrainedTask
import com.example.ui.home.BottomTab
import com.example.ui.home.NavTabItem
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.TextPureWhite
import com.example.ui.theme.TextSoftGray
import kotlinx.coroutines.delay

@Composable
fun MahiMemoriesScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onOpenDrawer: () -> Unit,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val memoryRepo = remember { MahiMemoryRepository(context) }
    var taskList by remember { mutableStateOf(memoryRepo.getTrainedTasks()) }

    var executingTask by remember { mutableStateOf<TrainedTask?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TrainedTask?>(null) }
    var deletingTaskId by remember { mutableStateOf<String?>(null) }

    // Intercept back button to return to Home
    BackHandler {
        onNavigateToHome()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkNavyBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("memories_screen_root")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ================= HEADER =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("memories_header"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = TextPureWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Memories",
                    color = TextPureWhite,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.testTag("memories_title")
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = { /* notification */ },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = Color(0xFFBAC5D6),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0E1A2E))
                        .border(BorderStroke(1.dp, Color(0xFF1E3A66)), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Mahi AI Avatar",
                        tint = ElectricBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Main Content List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 18.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Trained tasks",
                            color = TextPureWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.testTag("trained_tasks_section_header")
                        )

                        // Add Custom Task Button
                        IconButton(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add custom task",
                                tint = ElectricBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 5 Built-in / Custom Task Cards
                items(taskList, key = { it.id }) { task ->
                    TrainedTaskCard(
                        task = task,
                        onPlayClick = { executingTask = task },
                        onEditClick = { editingTask = task },
                        onDeleteClick = { deletingTaskId = task.id }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))

                    // Backup & restore Card
                    BackupCardRow(onClick = onNavigateToBackup)

                    Spacer(modifier = Modifier.height(30.dp))
                }
            }

            // Bottom Navigation with Floating Mic
            MemoriesBottomBar(
                onNavigateToHome = onNavigateToHome,
                onNavigateToScan = onNavigateToScan,
                onNavigateToChat = onNavigateToChat,
                onVoiceClick = onVoiceClick
            )
        }

        // Execution Task Dialog
        if (executingTask != null) {
            TaskExecutionDialog(
                task = executingTask!!,
                onDismiss = { executingTask = null }
            )
        }

        // Create Custom Task Dialog
        if (showCreateDialog) {
            CreateTaskDialog(
                onDismiss = { showCreateDialog = false },
                onTaskCreated = { title, steps, prompt ->
                    memoryRepo.addCustomTask(title, steps, "$steps steps • custom", prompt)
                    taskList = memoryRepo.getTrainedTasks()
                    showCreateDialog = false
                }
            )
        }

        // Edit Custom Task Dialog
        if (editingTask != null) {
            EditTaskDialog(
                task = editingTask!!,
                onDismiss = { editingTask = null },
                onTaskUpdated = { title, steps, prompt ->
                    memoryRepo.updateCustomTask(editingTask!!.id, title, steps, prompt)
                    taskList = memoryRepo.getTrainedTasks()
                    editingTask = null
                }
            )
        }

        // Delete Task Confirmation
        if (deletingTaskId != null) {
            AlertDialog(
                onDismissRequest = { deletingTaskId = null },
                title = { Text("Delete Trained Task", color = TextPureWhite, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete this custom task?", color = TextSoftGray) },
                confirmButton = {
                    Button(
                        onClick = {
                            deletingTaskId?.let { memoryRepo.deleteCustomTask(it) }
                            taskList = memoryRepo.getTrainedTasks()
                            deletingTaskId = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletingTaskId = null }) {
                        Text("Cancel", color = TextSoftGray)
                    }
                },
                containerColor = Color(0xFF0F1726)
            )
        }
    }
}

@Composable
fun TrainedTaskCard(
    task: TrainedTask,
    onPlayClick: () -> Unit,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF10192A))
            .border(BorderStroke(1.dp, Color(0xFF1E2D44)), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("trained_task_${task.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                color = TextPureWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = task.subtitle,
                color = Color(0xFF5A85D6),
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif
            )
        }

        // Edit and Delete icons for custom tasks
        if (!task.isBuiltIn) {
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit task",
                    tint = Color(0xFF93C5FD),
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete task",
                    tint = Color(0xFFF87171),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onPlayClick() }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = ElectricBlue,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Play",
                color = ElectricBlue,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

@Composable
fun BackupCardRow(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF10192A))
            .border(BorderStroke(1.dp, Color(0xFF1E2D44)), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("backup_restore_card"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudDownload,
            contentDescription = null,
            tint = Color(0xFF8697AF),
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Backup & restore",
                color = TextPureWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Move your memories and chats to another phone",
                color = Color(0xFF8697AF),
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Open",
            tint = Color(0xFF6B7B94),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun TaskExecutionDialog(
    task: TrainedTask,
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableStateOf(1) }
    var isRunning by remember { mutableStateOf(true) }

    LaunchedEffect(task) {
        for (i in 1..task.steps) {
            currentStep = i
            delay(350)
        }
        isRunning = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = task.title,
                color = TextPureWhite,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Executing trained routine with Mahi AI engine...",
                    color = TextSoftGray,
                    fontSize = 13.5.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = ElectricBlue,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Step $currentStep of ${task.steps}...",
                            color = Color(0xFF93C5FD),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "✓ Routine completed successfully!",
                            color = Color(0xFF4ADE80),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Text(if (isRunning) "Cancel" else "Done")
            }
        },
        containerColor = Color(0xFF0F1726)
    )
}

@Composable
fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onTaskCreated: (String, Int, String) -> Unit
) {
    var taskName by remember { mutableStateOf("") }
    var stepCountStr by remember { mutableStateOf("8") }
    var promptTemplate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Trained Task",
                color = TextPureWhite,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("Task Name (e.g. summarize notes)") },
                    textStyle = TextStyle(color = TextPureWhite),
                    singleLine = true
                )
                OutlinedTextField(
                    value = stepCountStr,
                    onValueChange = { stepCountStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Number of Steps") },
                    textStyle = TextStyle(color = TextPureWhite),
                    singleLine = true
                )
                OutlinedTextField(
                    value = promptTemplate,
                    onValueChange = { promptTemplate = it },
                    label = { Text("Prompt Instructions") },
                    textStyle = TextStyle(color = TextPureWhite),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (taskName.isNotBlank()) {
                        val steps = stepCountStr.toIntOrNull() ?: 8
                        onTaskCreated(taskName.trim(), steps, promptTemplate.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Text("Create Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSoftGray)
            }
        },
        containerColor = Color(0xFF0F1726)
    )
}

@Composable
fun EditTaskDialog(
    task: TrainedTask,
    onDismiss: () -> Unit,
    onTaskUpdated: (String, Int, String) -> Unit
) {
    var taskName by remember { mutableStateOf(task.title) }
    var stepCountStr by remember { mutableStateOf(task.steps.toString()) }
    var promptTemplate by remember { mutableStateOf(task.promptTemplate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Trained Task",
                color = TextPureWhite,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("Task Name") },
                    textStyle = TextStyle(color = TextPureWhite),
                    singleLine = true
                )
                OutlinedTextField(
                    value = stepCountStr,
                    onValueChange = { stepCountStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Number of Steps") },
                    textStyle = TextStyle(color = TextPureWhite),
                    singleLine = true
                )
                OutlinedTextField(
                    value = promptTemplate,
                    onValueChange = { promptTemplate = it },
                    label = { Text("Prompt Instructions") },
                    textStyle = TextStyle(color = TextPureWhite),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (taskName.isNotBlank()) {
                        val steps = stepCountStr.toIntOrNull() ?: task.steps
                        onTaskUpdated(taskName.trim(), steps, promptTemplate.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSoftGray)
            }
        },
        containerColor = Color(0xFF0F1726)
    )
}

@Composable
fun MemoriesBottomBar(
    onNavigateToHome: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToChat: () -> Unit,
    onVoiceClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF070B14))
    ) {
        // Floating Mic Centered
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(ElectricBlue)
                    .clickable { onVoiceClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = "Voice",
                    tint = TextPureWhite,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // 4 Tabs: Home | Scan | Memories | Chat
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTabItem(
                label = "Home",
                icon = Icons.Outlined.Home,
                isSelected = false,
                onClick = onNavigateToHome
            )
            NavTabItem(
                label = "Scan",
                icon = Icons.Outlined.QrCodeScanner,
                isSelected = false,
                onClick = onNavigateToScan
            )
            NavTabItem(
                label = "Memories",
                icon = Icons.Outlined.Psychology,
                isSelected = true,
                onClick = {}
            )
            NavTabItem(
                label = "Chat",
                icon = Icons.AutoMirrored.Outlined.Chat,
                isSelected = false,
                onClick = onNavigateToChat
            )
        }
    }
}
