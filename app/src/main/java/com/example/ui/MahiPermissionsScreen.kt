package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Headset
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MahiTheme
import com.example.ui.theme.TextPureWhite
import com.example.ui.theme.TextSoftGray

enum class MahiPermissionKey {
    MICROPHONE,
    CAMERA,
    PHONE_CALLS,
    CONTACTS,
    SMS,
    LOCATION,
    GALLERY_FILES,
    MANAGE_CALLS
}

data class PermissionItemInfo(
    val key: MahiPermissionKey,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isRequired: Boolean = false,
    val permissions: List<String>
)

val ALL_PERMISSION_ITEMS = listOf(
    PermissionItemInfo(
        key = MahiPermissionKey.MICROPHONE,
        title = "Microphone",
        description = "So you can talk to Mahi AI. Required.",
        icon = Icons.Outlined.Mic,
        isRequired = true,
        permissions = listOf(Manifest.permission.RECORD_AUDIO)
    ),
    PermissionItemInfo(
        key = MahiPermissionKey.CAMERA,
        title = "Camera",
        description = "So she can take a photo or look at what you point the phone at.",
        icon = Icons.Outlined.PhotoCamera,
        isRequired = false,
        permissions = listOf(Manifest.permission.CAMERA)
    ),
    PermissionItemInfo(
        key = MahiPermissionKey.PHONE_CALLS,
        title = "Phone calls",
        description = "So she can place a call for you.",
        icon = Icons.Outlined.Call,
        isRequired = false,
        permissions = listOf(Manifest.permission.CALL_PHONE)
    ),
    PermissionItemInfo(
        key = MahiPermissionKey.CONTACTS,
        title = "Contacts",
        description = "So a name is enough — she looks up the number.",
        icon = Icons.Outlined.Contacts,
        isRequired = false,
        permissions = listOf(Manifest.permission.READ_CONTACTS)
    ),
    PermissionItemInfo(
        key = MahiPermissionKey.SMS,
        title = "SMS",
        description = "So she can send a text message.",
        icon = Icons.Outlined.Chat,
        isRequired = false,
        permissions = listOf(Manifest.permission.SEND_SMS)
    ),
    PermissionItemInfo(
        key = MahiPermissionKey.LOCATION,
        title = "Location",
        description = "Weather, navigation, and where you are.",
        icon = Icons.Outlined.LocationOn,
        isRequired = false,
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    ),
    PermissionItemInfo(
        key = MahiPermissionKey.GALLERY_FILES,
        title = "Gallery & files",
        description = "So she can find a photo or file and send it.",
        icon = Icons.Outlined.Image,
        isRequired = false,
        permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    ),
    PermissionItemInfo(
        key = MahiPermissionKey.MANAGE_CALLS,
        title = "Answer & manage calls",
        description = "So she can announce who is calling and answer or reject it.",
        icon = Icons.Outlined.Headset,
        isRequired = false,
        permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            listOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ANSWER_PHONE_CALLS
            )
        } else {
            listOf(Manifest.permission.READ_PHONE_STATE)
        }
    )
)

/**
 * Checks whether all permissions for an item are granted.
 */
fun isPermissionItemGranted(context: Context, item: PermissionItemInfo): Boolean {
    return item.permissions.all { perm ->
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun MahiPermissionsScreen(
    onContinueToStep3: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Live reactive map of permission states
    val permissionsState = remember {
        mutableStateMapOf<MahiPermissionKey, Boolean>().apply {
            ALL_PERMISSION_ITEMS.forEach { item ->
                put(item.key, isPermissionItemGranted(context, item))
            }
        }
    }

    // Refresh permission states helper
    fun refreshPermissions() {
        ALL_PERMISSION_ITEMS.forEach { item ->
            permissionsState[item.key] = isPermissionItemGranted(context, item)
        }
    }

    // Auto-refresh when app resumes from system dialog or settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Launcher for requesting multiple permissions together ("Allow all")
    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        refreshPermissions()
    }

    // Launcher for individual card tap
    var activeItemForRequest by remember { mutableStateOf<PermissionItemInfo?>(null) }
    val singleItemLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        refreshPermissions()
    }

    val isMicGranted = permissionsState[MahiPermissionKey.MICROPHONE] == true

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkNavyBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("permissions_screen")
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
                        .testTag("step2_header"),
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
                            modifier = Modifier.testTag("step2_header_title")
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Step 2 of 3",
                            color = TextSoftGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.testTag("step2_header_step")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3-section progress bar (first 2 blue, third dark)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("step2_progress_bar"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Section 1 - Blue
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ElectricBlue)
                            .testTag("step2_progress_1")
                    )
                    // Section 2 - Blue
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ElectricBlue)
                            .testTag("step2_progress_2")
                    )
                    // Section 3 - Dark
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF1A263B))
                            .testTag("step2_progress_3")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ================= TITLE & DESCRIPTION =================
                Text(
                    text = "Let Mahi AI use your phone",
                    color = TextPureWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.testTag("permissions_title")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "One tap — Android asks for each in turn. Only the microphone is required; the rest make her useful.",
                    color = TextSoftGray,
                    fontSize = 13.5.sp,
                    lineHeight = 19.5.sp,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.testTag("permissions_description")
                )

                Spacer(modifier = Modifier.height(18.dp))
            }

            // ================= 8 PERMISSION CARDS =================
            items(ALL_PERMISSION_ITEMS, key = { it.key }) { itemInfo ->
                val isGranted = permissionsState[itemInfo.key] == true

                PermissionCard(
                    item = itemInfo,
                    isGranted = isGranted,
                    onClick = {
                        if (!isGranted) {
                            activeItemForRequest = itemInfo
                            singleItemLauncher.launch(itemInfo.permissions.toTypedArray())
                        } else {
                            // If user taps an already granted permission, open settings to manage
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                // ================= "ALLOW ALL" BUTTON =================
                Button(
                    onClick = {
                        // Gather all ungranted permissions
                        val ungrantedPerms = ALL_PERMISSION_ITEMS
                            .filter { permissionsState[it.key] != true }
                            .flatMap { it.permissions }
                            .distinct()

                        if (ungrantedPerms.isNotEmpty()) {
                            multiplePermissionsLauncher.launch(ungrantedPerms.toTypedArray())
                        } else {
                            // All are already granted; refresh
                            refreshPermissions()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("allow_all_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        contentColor = TextPureWhite
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = TextPureWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Allow all",
                            color = TextPureWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ================= "CONTINUE" BUTTON =================
                val continueBorderColor by animateColorAsState(
                    targetValue = if (isMicGranted) Color(0xFF2B3C58) else Color(0xFF162030),
                    label = "continue_border"
                )

                OutlinedButton(
                    onClick = {
                        if (isMicGranted) {
                            onContinueToStep3()
                        }
                    },
                    enabled = isMicGranted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .alpha(if (isMicGranted) 1f else 0.45f)
                        .testTag("continue_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, continueBorderColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isMicGranted) Color(0xFF0E1626) else Color(0xFF0A101C),
                        disabledContainerColor = Color(0xFF0A101C),
                        contentColor = TextPureWhite,
                        disabledContentColor = Color(0xFF4B576D)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Continue",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Helper footnote when microphone is not allowed
                Text(
                    text = if (isMicGranted) {
                        "Microphone allowed. You are ready to continue."
                    } else {
                        "Continue unlocks once the microphone is allowed."
                    },
                    color = if (isMicGranted) Color(0xFF4ADE80) else Color(0xFF758399),
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .testTag("continue_hint_text")
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun PermissionCard(
    item: PermissionItemInfo,
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
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("permission_card_${item.key.name.lowercase()}"),
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
fun MahiPermissionsScreenPreview() {
    MahiTheme {
        MahiPermissionsScreen(onContinueToStep3 = {})
    }
}
