package com.example.ui

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Headset
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MahiTheme
import com.example.ui.theme.TextPureWhite
import com.example.ui.theme.TextSoftGray

enum class SystemSettingKey {
    BATTERY,
    OVERLAY,
    NOTIFICATIONS,
    ACCESSIBILITY,
    DEFAULT_ASSISTANT
}

data class SystemSettingItemInfo(
    val key: SystemSettingKey,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isRequired: Boolean = false
)

val ALL_SYSTEM_SETTING_ITEMS = listOf(
    SystemSettingItemInfo(
        key = SystemSettingKey.BATTERY,
        title = "Battery — no optimisation",
        description = "Required. Without it the phone kills Mahi AI in the background and she goes quiet. Tap, then choose Allow.",
        icon = Icons.Outlined.BatteryAlert,
        isRequired = true
    ),
    SystemSettingItemInfo(
        key = SystemSettingKey.OVERLAY,
        title = "Display over other apps",
        description = "So her orb can float on top of whatever you are doing.",
        icon = Icons.Outlined.Layers,
        isRequired = false
    ),
    SystemSettingItemInfo(
        key = SystemSettingKey.NOTIFICATIONS,
        title = "Notification access",
        description = "To read WhatsApp messages and know who is calling. Turn on \"Mahi AI\" in the list.",
        icon = Icons.Outlined.Notifications,
        isRequired = false
    ),
    SystemSettingItemInfo(
        key = SystemSettingKey.ACCESSIBILITY,
        title = "Accessibility service",
        description = "So she can open apps, tap, and read the screen for you. Turn on \"Mahi AI\" in the list.",
        icon = Icons.Outlined.AccessibilityNew,
        isRequired = false
    ),
    SystemSettingItemInfo(
        key = SystemSettingKey.DEFAULT_ASSISTANT,
        title = "Default assistant",
        description = "Long-press the power button or swipe from a corner to call her, even on the lock screen. Pick \"MAHI AI\".",
        icon = Icons.Outlined.Headset,
        isRequired = false
    )
)

/**
 * Checks genuine Android system settings for each permission/capability.
 */
fun isSystemSettingActive(context: Context, key: SystemSettingKey): Boolean {
    return try {
        when (key) {
            SystemSettingKey.BATTERY -> {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
            }
            SystemSettingKey.OVERLAY -> {
                Settings.canDrawOverlays(context)
            }
            SystemSettingKey.NOTIFICATIONS -> {
                val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
                if (enabledListeners.contains(context.packageName)) return true
                val flat = Settings.Secure.getString(
                    context.contentResolver,
                    "enabled_notification_listeners"
                ).orEmpty()
                flat.contains(context.packageName)
            }
            SystemSettingKey.ACCESSIBILITY -> {
                val enabledServices = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ).orEmpty()
                enabledServices.contains(context.packageName)
            }
            SystemSettingKey.DEFAULT_ASSISTANT -> {
                val assistant = Settings.Secure.getString(
                    context.contentResolver,
                    "assistant"
                ).orEmpty()
                val voiceInteraction = Settings.Secure.getString(
                    context.contentResolver,
                    "voice_interaction_service"
                ).orEmpty()
                assistant.contains(context.packageName) ||
                        voiceInteraction.contains(context.packageName) ||
                        assistant.contains("mahi", ignoreCase = true)
            }
        }
    } catch (_: Exception) {
        false
    }
}

/**
 * Opens the corresponding Android system settings screen for each card.
 */
fun openSystemSettingScreen(context: Context, key: SystemSettingKey) {
    try {
        when (key) {
            SystemSettingKey.BATTERY -> {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                    try {
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    } catch (_: Exception) {
                        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(fallback)
                    }
                }
            }
            SystemSettingKey.OVERLAY -> {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                }
            }
            SystemSettingKey.NOTIFICATIONS -> {
                try {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                } catch (_: Exception) {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
            SystemSettingKey.ACCESSIBILITY -> {
                try {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } catch (_: Exception) {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
            SystemSettingKey.DEFAULT_ASSISTANT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                            val roleIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT)
                            context.startActivity(roleIntent)
                            return
                        }
                    } catch (_: Exception) {
                        // fallback
                    }
                }
                try {
                    context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
                } catch (_: Exception) {
                    try {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                    } catch (_: Exception) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }
            }
        }
    } catch (_: Exception) {
        // Fallback to app details
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // ignore
        }
    }
}

@Composable
fun MahiStep3Screen(
    onFinishSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Live reactive map of settings states
    val settingsState = remember {
        mutableStateMapOf<SystemSettingKey, Boolean>().apply {
            ALL_SYSTEM_SETTING_ITEMS.forEach { item ->
                put(item.key, isSystemSettingActive(context, item.key))
            }
        }
    }

    // Refresh function
    fun refreshSettings() {
        ALL_SYSTEM_SETTING_ITEMS.forEach { item ->
            settingsState[item.key] = isSystemSettingActive(context, item.key)
        }
    }

    // Auto-refresh when user returns from Android Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshSettings()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isBatteryOptimisationOff = settingsState[SystemSettingKey.BATTERY] == true

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkNavyBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("step3_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // ================= HEADER =================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("step3_header"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Top-Left Mahi AI App Logo
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0E1626))
                            .border(
                                BorderStroke(1.dp, Color(0xFF1E3A66)),
                                RoundedCornerShape(10.dp)
                            )
                            .testTag("app_logo"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Mahi AI Logo",
                            tint = ElectricBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Set up Mahi AI",
                            color = TextPureWhite,
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.testTag("step3_header_title")
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Step 3 of 3",
                            color = TextSoftGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.testTag("step3_header_step")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3-section progress bar (all 3 filled blue)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("step3_progress_bar"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ElectricBlue)
                            .testTag("step3_progress_1")
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ElectricBlue)
                            .testTag("step3_progress_2")
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ElectricBlue)
                            .testTag("step3_progress_3")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ================= TITLE & DESCRIPTION =================
                Text(
                    text = "Keep Mahi AI alive in the background",
                    color = TextPureWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.testTag("step3_title")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Battery is required — it is what stops her going quiet when the screen is off. The rest are optional but make her far more useful.",
                    color = TextSoftGray,
                    fontSize = 13.5.sp,
                    lineHeight = 19.5.sp,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.testTag("step3_description")
                )

                Spacer(modifier = Modifier.height(18.dp))
            }

            // ================= 5 PERMISSION / SETTING CARDS =================
            items(ALL_SYSTEM_SETTING_ITEMS, key = { it.key }) { itemInfo ->
                val isGranted = settingsState[itemInfo.key] == true

                SystemSettingCard(
                    item = itemInfo,
                    isGranted = isGranted,
                    onClick = {
                        openSystemSettingScreen(context, itemInfo.key)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))

                // ================= INSTRUCTION FOOTNOTE =================
                Text(
                    text = "Tap a Pending row to open its system screen — come back here when you are done. You can change any of them later in Settings → Advanced → Permissions.",
                    color = Color(0xFF758399),
                    fontSize = 12.5.sp,
                    lineHeight = 17.5.sp,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .testTag("footnote_info")
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ================= BOTTOM "FINISH" BUTTON =================
                val finishButtonColor by animateColorAsState(
                    targetValue = if (isBatteryOptimisationOff) ElectricBlue else Color(0xFF141D2D),
                    label = "finish_button_color"
                )

                Button(
                    onClick = {
                        if (isBatteryOptimisationOff) {
                            onFinishSetup()
                        }
                    },
                    enabled = isBatteryOptimisationOff,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("finish_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = finishButtonColor,
                        disabledContainerColor = Color(0xFF121926),
                        contentColor = TextPureWhite,
                        disabledContentColor = Color(0xFF455167)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Finish",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Under Finish note
                Text(
                    text = if (isBatteryOptimisationOff) {
                        "Battery optimisation is disabled. Mahi AI can run in the background."
                    } else {
                        "Finish unlocks once battery optimisation is off for Mahi AI — tap the Battery row."
                    },
                    color = if (isBatteryOptimisationOff) Color(0xFF4ADE80) else Color(0xFF758399),
                    fontSize = 12.5.sp,
                    lineHeight = 16.5.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .testTag("finish_hint_text")
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SystemSettingCard(
    item: SystemSettingItemInfo,
    isGranted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = Color(0xFF101726)
    val cardBorder = if (isGranted) Color(0xFF163826) else Color(0xFF1C273C)

    val iconContainerBg = if (isGranted) Color(0xFF0E2A1C) else Color(0xFF16233B)
    val iconTint = if (isGranted) Color(0xFF4ADE80) else ElectricBlue

    val badgeBg = if (isGranted) Color(0xFF0E2A1A) else Color(0xFF332010)
    val badgeTextColor = if (isGranted) Color(0xFF4ADE80) else Color(0xFFF97316)
    val badgeText = if (isGranted) "Allowed" else "Pending"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp)
            .testTag("setting_card_${item.key.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Icon in rounded circle
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconContainerBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Outlined.Check else item.icon,
                contentDescription = item.title,
                tint = iconTint,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(13.dp))

        // Title and description
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.title,
                color = TextPureWhite,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.description,
                color = TextSoftGray,
                fontSize = 12.sp,
                lineHeight = 16.5.sp,
                fontFamily = FontFamily.SansSerif
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Right status badge (Pending / Allowed)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(badgeBg)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badgeText,
                color = badgeTextColor,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F19, widthDp = 390, heightDp = 844)
@Composable
fun MahiStep3ScreenPreview() {
    MahiTheme {
        MahiStep3Screen(onFinishSetup = {})
    }
}
