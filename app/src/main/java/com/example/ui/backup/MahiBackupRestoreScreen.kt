package com.example.ui.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MahiMemoryRepository
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.TextPureWhite
import com.example.ui.theme.TextSoftGray

@Composable
fun MahiBackupRestoreScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val memoryRepo = remember { MahiMemoryRepository(context) }
    val scrollState = rememberScrollState()

    BackHandler {
        onNavigateBack()
    }

    var feedbackDialogTitle by remember { mutableStateOf<String?>(null) }
    var feedbackDialogMessage by remember { mutableStateOf<String?>(null) }

    val memoriesCount = memoryRepo.getMemories().size
    val conversationsCount = memoryRepo.getConversationCount()
    val contactsCount = memoryRepo.getFavoriteContacts().size

    // Save File Launcher
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val backupJson = memoryRepo.createBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(backupJson.toByteArray(Charsets.UTF_8))
                }
                feedbackDialogTitle = "Backup Saved"
                feedbackDialogMessage = "Your memories and tasks have been safely exported to your chosen location."
            } catch (e: Exception) {
                feedbackDialogTitle = "Export Error"
                feedbackDialogMessage = "Failed to write backup file: ${e.localizedMessage}"
            }
        }
    }

    // Restore File Launcher
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val jsonContent = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader().readText()
                }.orEmpty()

                if (jsonContent.isNotBlank()) {
                    val (importedMemories, importedTasks) = memoryRepo.restoreFromJson(jsonContent)
                    feedbackDialogTitle = "Restore Complete"
                    feedbackDialogMessage = "Successfully imported $importedMemories memories and $importedTasks custom tasks. Existing data was safely preserved."
                } else {
                    feedbackDialogTitle = "Empty File"
                    feedbackDialogMessage = "The selected backup file appears to be empty."
                }
            } catch (e: Exception) {
                feedbackDialogTitle = "Restore Failed"
                feedbackDialogMessage = "Could not parse backup file: ${e.localizedMessage}"
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkNavyBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("backup_restore_screen_root")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ================= HEADER =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("backup_header"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPureWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Backup & Restore",
                    color = TextPureWhite,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.testTag("backup_screen_title")
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

            // Scrollable Cards Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp)
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // ================= CARD 1: WHAT GETS BACKED UP =================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF10192A))
                        .border(BorderStroke(1.dp, Color(0xFF1E2D44)), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                        .testTag("card_what_gets_backed_up")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF182845)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudDownload,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "What gets backed up",
                                color = TextPureWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = "Everything Mahi AI has learned about you",
                                color = TextSoftGray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Row: Memories
                    BackupStatRow(
                        title = "Memories",
                        subtitle = "facts Mahi AI remembers",
                        count = memoriesCount
                    )

                    HorizontalDivider(color = Color(0xFF182338), thickness = 1.dp)

                    // Row: Conversations
                    BackupStatRow(
                        title = "Conversations",
                        subtitle = "saved chat transcripts",
                        count = conversationsCount
                    )

                    HorizontalDivider(color = Color(0xFF182338), thickness = 1.dp)

                    // Row: Favorite contacts
                    BackupStatRow(
                        title = "Favorite contacts",
                        subtitle = "including SOS contacts",
                        count = contactsCount
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Security information box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF132036))
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "API keys, your licence and your voice print are NOT included — a backup file is meant to be safe to send through Drive or WhatsApp.",
                            color = Color(0xFFC7D3E5),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ================= CARD 2: EXPORT =================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF10192A))
                        .border(BorderStroke(1.dp, Color(0xFF1E2D44)), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                        .testTag("card_export")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF182845)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Export",
                                color = TextPureWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = "Save a copy or send it to your other phone",
                                color = TextSoftGray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Primary Button: "Save backup file"
                    Button(
                        onClick = {
                            val timestamp = System.currentTimeMillis()
                            saveFileLauncher.launch("mahi_backup_$timestamp.json")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_backup_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor = TextPureWhite
                        )
                    ) {
                        Text(
                            text = "Save backup file",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary Button: "Share backup"
                    OutlinedButton(
                        onClick = {
                            val backupJson = memoryRepo.createBackupJson()
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, backupJson)
                                putExtra(Intent.EXTRA_TITLE, "Mahi AI Backup")
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share Mahi AI Backup")
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("share_backup_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF22324A)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF0E1626),
                            contentColor = TextPureWhite
                        )
                    ) {
                        Text(
                            text = "Share backup",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ================= CARD 3: RESTORE =================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF10192A))
                        .border(BorderStroke(1.dp, Color(0xFF1E2D44)), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                        .testTag("card_restore")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF182845)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Restore",
                                color = TextPureWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = "Bring a backup in from another device",
                                color = TextSoftGray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = { openFileLauncher.launch("application/json") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("choose_backup_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor = TextPureWhite
                        )
                    ) {
                        Text(
                            text = "Choose backup file",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF132036))
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Restoring adds to this phone — nothing already here is deleted, and importing the same file twice changes nothing.",
                            color = Color(0xFFC7D3E5),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        // Feedback Dialog
        if (feedbackDialogTitle != null) {
            AlertDialog(
                onDismissRequest = {
                    feedbackDialogTitle = null
                    feedbackDialogMessage = null
                },
                title = {
                    Text(
                        text = feedbackDialogTitle.orEmpty(),
                        color = TextPureWhite,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = feedbackDialogMessage.orEmpty(),
                        color = TextSoftGray,
                        fontSize = 13.5.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            feedbackDialogTitle = null
                            feedbackDialogMessage = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text("OK")
                    }
                },
                containerColor = Color(0xFF0F1726)
            )
        }
    }
}

@Composable
fun BackupStatRow(
    title: String,
    subtitle: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPureWhite,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = subtitle,
                color = Color(0xFF75859E),
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif
            )
        }

        Text(
            text = "$count",
            color = Color(0xFF60A5FA),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )
    }
}
