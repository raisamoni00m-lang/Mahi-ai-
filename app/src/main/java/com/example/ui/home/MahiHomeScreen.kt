package com.example.ui.home

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.home.components.MahiAiOrb
import com.example.ui.home.components.MahiDrawerContent
import com.example.ui.home.components.OrbState
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.DeepMidnightNavy
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.TextPureWhite
import com.example.ui.theme.TextSoftGray
import com.example.ui.memories.MahiMemoriesScreen
import com.example.ui.backup.MahiBackupRestoreScreen
import com.example.ui.scanner.MahiScannerScreen
import kotlinx.coroutines.launch

@Composable
fun MahiHomeScreen(
    viewModel: MahiHomeViewModel,
    onNavigateToPermissions: () -> Unit,
    onNavigateToSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var showBackupScreen by remember { mutableStateOf(false) }

    // System Back button handling
    BackHandler(enabled = showBackupScreen) {
        showBackupScreen = false
    }
    BackHandler(enabled = !showBackupScreen && state.currentTab != BottomTab.HOME) {
        viewModel.onTabSelected(BottomTab.HOME)
    }

    // Photo picker launcher for attachments or vision scanning
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (_: Exception) {
                null
            }
            if (bitmap != null) {
                viewModel.onScanImage(bitmap)
                viewModel.onTabSelected(BottomTab.SCAN)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0A0F1C)
            ) {
                MahiDrawerContent(
                    onNavigateToPermissions = onNavigateToPermissions,
                    onNavigateToChat = {
                        showBackupScreen = false
                        viewModel.onTabSelected(BottomTab.CHAT)
                    },
                    onNavigateToMemories = {
                        showBackupScreen = false
                        viewModel.onTabSelected(BottomTab.MEMORIES)
                    },
                    onNavigateToScan = {
                        showBackupScreen = false
                        viewModel.onTabSelected(BottomTab.SCAN)
                    },
                    onNavigateToBackup = {
                        showBackupScreen = true
                    },
                    onOpenVoiceSettings = { viewModel.toggleVoiceSettings(true) },
                    onOpenProfile = onNavigateToSetup,
                    onCloseDrawer = { coroutineScope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        if (showBackupScreen) {
            MahiBackupRestoreScreen(
                onNavigateBack = { showBackupScreen = false }
            )
        } else if (state.currentTab == BottomTab.SCAN) {
            MahiScannerScreen(
                onNavigateToHome = { viewModel.onTabSelected(BottomTab.HOME) },
                onNavigateToMemories = { viewModel.onTabSelected(BottomTab.MEMORIES) },
                onNavigateToChat = { viewModel.onTabSelected(BottomTab.CHAT) },
                onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
            )
        } else if (state.currentTab == BottomTab.MEMORIES) {
            MahiMemoriesScreen(
                onNavigateToHome = { viewModel.onTabSelected(BottomTab.HOME) },
                onNavigateToScan = { viewModel.onTabSelected(BottomTab.SCAN) },
                onNavigateToChat = { viewModel.onTabSelected(BottomTab.CHAT) },
                onNavigateToBackup = { showBackupScreen = true },
                onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                onVoiceClick = { viewModel.toggleVoiceInteraction() }
            )
        } else {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(DarkNavyBackground)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .drawBehind {
                        // Deep celestial sci-fi background atmosphere
                        val center = Offset(size.width * 0.5f, size.height * 0.42f)
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF131D33).copy(alpha = 0.65f),
                                    DeepMidnightNavy,
                                    DarkNavyBackground
                                ),
                                center = center,
                                radius = size.width * 0.95f
                            )
                        )
                    }
                    .testTag("home_screen_root")
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // ================= TOP HEADER =================
                    MahiTopHeader(
                        onOpenMenu = { coroutineScope.launch { drawerState.open() } },
                        onNotificationClick = { viewModel.toggleNotificationAlert(true) },
                        onAvatarClick = { coroutineScope.launch { drawerState.open() } }
                    )

                    // Tab Content
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (state.currentTab) {
                            BottomTab.HOME -> {
                                HomeMainContent(
                                    state = state,
                                    viewModel = viewModel,
                                    onAttachClick = { photoPickerLauncher.launch("image/*") }
                                )
                            }
                            BottomTab.CHAT -> {
                                ChatTabContent(
                                    messages = state.chatMessages,
                                    onSendMessage = { text -> viewModel.sendMessage(text) }
                                )
                            }
                            else -> {}
                        }
                    }

                    // ================= BOTTOM INPUT & FLOATING MIC & NAV =================
                    BottomControlBar(
                        state = state,
                        viewModel = viewModel,
                        onAttachClick = { photoPickerLauncher.launch("image/*") }
                    )
                }
            }
        }

            // Voice settings dialog
            if (state.showVoiceSettingsDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.toggleVoiceSettings(false) },
                    title = {
                        Text(
                            text = "Mahi AI Voice",
                            color = TextPureWhite,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = "Natural female persona: Mahi (Aoede).\nContinuous conversation with natural Bangla, English and code-switching.\nReal voice barge-in and instant interruption enabled.",
                            color = TextSoftGray
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.toggleVoiceSettings(false) }) {
                            Text("Done", color = ElectricBlue)
                        }
                    },
                    containerColor = Color(0xFF0F1726)
                )
            }
        }
    }

@Composable
fun MahiTopHeader(
    onOpenMenu: () -> Unit,
    onNotificationClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("top_header_bar"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hamburger Menu on the left
        IconButton(
            onClick = onOpenMenu,
            modifier = Modifier
                .size(40.dp)
                .testTag("hamburger_menu_button")
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Open Navigation Menu",
                tint = TextPureWhite,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Center Title: "Mahi AI"
        Text(
            text = "Mahi AI",
            color = TextPureWhite,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.5.sp,
            modifier = Modifier.testTag("header_title_mahi")
        )

        Spacer(modifier = Modifier.weight(1f))

        // Notification Bell
        IconButton(
            onClick = onNotificationClick,
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

        // Mahi AI Avatar Badge on far right
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0E1A2E))
                .border(BorderStroke(1.dp, Color(0xFF1E3A66)), RoundedCornerShape(8.dp))
                .clickable { onAvatarClick() }
                .testTag("avatar_button"),
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
}

@Composable
fun HomeMainContent(
    state: HomeUiState,
    viewModel: MahiHomeViewModel,
    onAttachClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // ================= FREE MODE ACCESS CARD =================
        FreeModeAccessCard(
            mode = state.accessMode,
            status = state.accessStatusText
        )

        Spacer(modifier = Modifier.height(18.dp))

        // ================= GREETING & STATUS =================
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.greetingPrefix,
                    color = Color(0xFFB0BDD1),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = state.greetingName,
                    color = TextPureWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.greetingSubtitle,
                    color = TextSoftGray,
                    fontSize = 12.5.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }

            // Unlimited Status Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF10192A))
                    .border(BorderStroke(1.dp, Color(0xFF1E2D44)), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("status_badge")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34D399))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Unlimited",
                            color = TextPureWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Continuous",
                        color = Color(0xFF75859E),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ================= MAIN AI ORB =================
        MahiAiOrb(
            state = state.orbState,
            onClick = { viewModel.toggleVoiceInteraction() },
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // Barge-in and listening indicators
        if (state.orbState == com.example.ui.home.components.OrbState.SPEAKING) {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = { viewModel.interruptSpeaking() },
                modifier = Modifier
                    .height(36.dp)
                    .testTag("interrupt_button"),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF1C1322)
                ),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop speaking",
                    tint = Color(0xFFF87171),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "থামো / Stop",
                    color = Color(0xFFFCA5A5),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else if (state.orbState == com.example.ui.home.components.OrbState.LISTENING) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F1E36))
                    .border(BorderStroke(1.dp, Color(0xFF1E3A66)), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("listening_indicator")
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ElectricBlue)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mahi শুনছে... Speak in Bangla or English",
                    color = Color(0xFF93C5FD),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ================= QUICK ACTIONS =================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick_actions_row"),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionButton(
                label = "Music",
                icon = Icons.Outlined.MusicNote,
                onClick = { viewModel.triggerQuickAction("Music") },
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                label = "Study",
                icon = Icons.Outlined.Book,
                onClick = { viewModel.triggerQuickAction("Study") },
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                label = "Journal",
                icon = Icons.Outlined.Edit,
                onClick = { viewModel.triggerQuickAction("Journal") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ================= INFO CARDS ROW =================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("info_cards_row"),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Weather Card
            InfoCard(
                modifier = Modifier.weight(1f),
                headerIcon = state.weather.icon,
                headerTitle = "Weather",
                mainValue = state.weather.temperature,
                subValue = state.weather.condition
            )

            // Today Date Card
            InfoCard(
                modifier = Modifier.weight(1f),
                headerIcon = "📅",
                headerTitle = "Today",
                mainValue = state.todayDateNumber,
                subValue = state.todayDayAndMonth
            )

            // Mood Card
            InfoCard(
                modifier = Modifier.weight(1f),
                headerIcon = "♥",
                headerTitle = "Mood",
                mainValue = state.activeMood,
                subValue = state.moodSubtitle,
                isMood = true
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun FreeModeAccessCard(
    mode: String,
    status: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF101A2E))
            .border(BorderStroke(1.dp, Color(0xFF1E2E4A)), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("free_mode_access_card"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Glowing status indicator icon
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFF162540)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = "Unlimited Access Active",
                tint = Color(0xFF34D399),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mode,
                color = TextPureWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = status,
                color = Color(0xFF93C5FD),
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif
            )
        }

        // Live Active Tag
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF064E3B))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = "ACTIVE",
                color = Color(0xFF6EE7B7),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF0F1727))
            .border(BorderStroke(1.dp, Color(0xFF1E2C44)), RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ElectricBlue,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = TextPureWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
fun InfoCard(
    headerIcon: String,
    headerTitle: String,
    mainValue: String,
    subValue: String,
    isMood: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(86.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF101827))
            .border(BorderStroke(1.dp, Color(0xFF1D2A3F)), RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = headerIcon,
                fontSize = 11.5.sp,
                color = if (isMood) Color(0xFFF472B6) else Color(0xFF93A2B8)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = headerTitle,
                color = Color(0xFF8C9BAE),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif
            )
        }

        Column {
            Text(
                text = mainValue,
                color = TextPureWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = subValue,
                color = Color(0xFF75859B),
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BottomControlBar(
    state: HomeUiState,
    viewModel: MahiHomeViewModel,
    onAttachClick: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val pulseTransition = rememberInfiniteTransition(label = "mic_pulse")
    val micScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (state.orbState == OrbState.LISTENING) 1.15f else 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF070B14))
            .testTag("bottom_control_bar")
    ) {
        // Chat Input Row with Floating Mic overlayed
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Input Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F1828))
                    .border(BorderStroke(1.dp, Color(0xFF1E2E48)), RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attachment Icon
                Icon(
                    imageVector = Icons.Outlined.AttachFile,
                    contentDescription = "Attach File/Photo",
                    tint = Color(0xFF7E8EA5),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onAttachClick() }
                        .testTag("attach_button")
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Input Text
                Box(modifier = Modifier.weight(1f)) {
                    if (state.inputText.isEmpty()) {
                        Text(
                            text = "Ask Mahi AI anything...",
                            color = Color(0xFF56657B),
                            fontSize = 13.5.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    BasicTextField(
                        value = state.inputText,
                        onValueChange = { viewModel.onInputTextChanged(it) },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = TextPureWhite,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.SansSerif
                        ),
                        cursorBrush = SolidColor(ElectricBlue),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                keyboardController?.hide()
                                viewModel.sendMessage()
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 40.dp)
                            .testTag("chat_input_field")
                    )
                }

                // Send Button
                IconButton(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.sendMessage()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = if (state.inputText.isNotBlank()) ElectricBlue else Color(0xFF4C5B72),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Large Circular Blue Microphone Button Centered
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-14).dp)
                    .size(56.dp)
                    .scale(micScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF60A5FA),
                                ElectricBlue,
                                Color(0xFF2563EB)
                            )
                        )
                    )
                    .border(BorderStroke(2.dp, Color(0xFF93C5FD).copy(alpha = 0.6f)), CircleShape)
                    .clickable { viewModel.toggleVoiceInteraction() }
                    .testTag("floating_mic_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = "Voice Interaction",
                    tint = TextPureWhite,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // 4 Bottom Navigation Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 12.dp)
                .testTag("bottom_nav_row"),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTabItem(
                label = "Home",
                icon = Icons.Outlined.Home,
                isSelected = state.currentTab == BottomTab.HOME,
                onClick = { viewModel.onTabSelected(BottomTab.HOME) }
            )
            NavTabItem(
                label = "Scan",
                icon = Icons.Outlined.QrCodeScanner,
                isSelected = state.currentTab == BottomTab.SCAN,
                onClick = { viewModel.onTabSelected(BottomTab.SCAN) }
            )
            NavTabItem(
                label = "Memories",
                icon = Icons.Outlined.Psychology,
                isSelected = state.currentTab == BottomTab.MEMORIES,
                onClick = { viewModel.onTabSelected(BottomTab.MEMORIES) }
            )
            NavTabItem(
                label = "Chat",
                icon = Icons.AutoMirrored.Outlined.Chat,
                isSelected = state.currentTab == BottomTab.CHAT,
                onClick = { viewModel.onTabSelected(BottomTab.CHAT) }
            )
        }
    }
}

@Composable
fun NavTabItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) ElectricBlue else Color(0xFF6B7B94),
            modifier = Modifier.size(21.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = if (isSelected) ElectricBlue else Color(0xFF6B7B94),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.SansSerif
        )
    }
}

// ================= SCAN TAB =================
@Composable
fun ScanTabContent(
    state: HomeUiState,
    onPickImage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.CameraAlt,
            contentDescription = null,
            tint = ElectricBlue,
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Mahi AI Vision Scanner",
            color = TextPureWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Select any photo, document, or screen to analyze with Gemini Vision.",
            color = TextSoftGray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onPickImage,
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Select Photo to Analyze")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.isScanning) {
            CircularProgressIndicator(color = ElectricBlue)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Mahi AI is analyzing the image...", color = Color(0xFF93C5FD))
        } else if (state.scanResultText != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10192A))
            ) {
                Text(
                    text = state.scanResultText,
                    color = TextPureWhite,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

// ================= MEMORIES TAB =================
@Composable
fun MemoriesTabContent(
    memories: List<com.example.data.MemoryItem>,
    onAddMemory: (String, String) -> Unit,
    onDeleteMemory: (String) -> Unit
) {
    var newMemoryText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Text(
            text = "Mahi's Memory Vault",
            color = TextPureWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Key details, preferences, and notes Mahi AI remembers about you.",
            color = TextSoftGray,
            fontSize = 12.5.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = newMemoryText,
                onValueChange = { newMemoryText = it },
                placeholder = { Text("Add something to remember...", color = Color(0xFF6B7B94)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(color = TextPureWhite, fontSize = 13.5.sp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newMemoryText.isNotBlank()) {
                        onAddMemory(newMemoryText.trim(), "Note")
                        newMemoryText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(memories, key = { it.id }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F1728))
                        .border(BorderStroke(1.dp, Color(0xFF1E2C44)), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = TextPureWhite,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = item.category,
                            color = Color(0xFF60A5FA),
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = { onDeleteMemory(item.id) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ================= CHAT TAB =================
@Composable
fun ChatTabContent(
    messages: List<com.example.data.ChatMessage>,
    onSendMessage: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        reverseLayout = true
    ) {
        items(messages.reversed(), key = { it.id }) { msg ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (msg.isUser) 16.dp else 2.dp,
                                bottomEnd = if (msg.isUser) 2.dp else 16.dp
                            )
                        )
                        .background(if (msg.isUser) ElectricBlue else Color(0xFF121B2B))
                        .border(
                            BorderStroke(
                                1.dp,
                                if (msg.isUser) ElectricBlue else Color(0xFF1E2D44)
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = msg.text,
                        color = TextPureWhite,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

