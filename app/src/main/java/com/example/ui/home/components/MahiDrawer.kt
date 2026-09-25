package com.example.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.DeepMidnightNavy
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.TextPureWhite
import com.example.ui.theme.TextSoftGray

data class DrawerMenuItem(
    val title: String,
    val icon: ImageVector,
    val action: () -> Unit
)

@Composable
fun MahiDrawerContent(
    onNavigateToPermissions: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToMemories: () -> Unit,
    onNavigateToScan: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onOpenVoiceSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(Color(0xFF0A0F1C))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = 20.dp, horizontal = 18.dp)
            .verticalScroll(scrollState)
            .testTag("mahi_side_drawer")
    ) {
        // App Avatar & Branding
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F1A2E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Mahi AI Logo",
                    tint = ElectricBlue,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "Mahi AI",
                    color = TextPureWhite,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "Your personal AI assistant",
                    color = Color(0xFF75859E),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = Color(0xFF162136), thickness = 1.dp)
        Spacer(modifier = Modifier.height(14.dp))

        // Menu Sections
        DrawerSectionHeader(title = "NAVIGATION")

        DrawerRow(
            title = "Profile",
            icon = Icons.Outlined.Person,
            onClick = {
                onOpenProfile()
                onCloseDrawer()
            }
        )

        DrawerRow(
            title = "Vision Scanner",
            icon = Icons.Outlined.QrCodeScanner,
            onClick = {
                onNavigateToScan()
                onCloseDrawer()
            }
        )

        DrawerRow(
            title = "Memories",
            icon = Icons.Outlined.Psychology,
            onClick = {
                onNavigateToMemories()
                onCloseDrawer()
            }
        )

        DrawerRow(
            title = "Backup & restore",
            icon = Icons.Outlined.CloudDownload,
            onClick = {
                onNavigateToBackup()
                onCloseDrawer()
            }
        )

        DrawerRow(
            title = "Chat history",
            icon = Icons.AutoMirrored.Outlined.Chat,
            onClick = {
                onNavigateToChat()
                onCloseDrawer()
            }
        )

        Spacer(modifier = Modifier.height(12.dp))
        DrawerSectionHeader(title = "PREFERENCES & SECURITY")

        DrawerRow(
            title = "Voice settings",
            icon = Icons.Outlined.RecordVoiceOver,
            onClick = {
                onOpenVoiceSettings()
                onCloseDrawer()
            }
        )

        DrawerRow(
            title = "Permissions",
            icon = Icons.Outlined.Security,
            onClick = {
                onNavigateToPermissions()
                onCloseDrawer()
            }
        )

        DrawerRow(
            title = "AI settings",
            icon = Icons.Outlined.Lock,
            onClick = {
                onOpenProfile()
                onCloseDrawer()
            }
        )

        DrawerRow(
            title = "Appearance",
            icon = Icons.Outlined.Palette,
            onClick = {
                onCloseDrawer()
            }
        )

        Spacer(modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider(color = Color(0xFF162136), thickness = 1.dp)
        Spacer(modifier = Modifier.height(14.dp))

        // About section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0D1424))
                .padding(14.dp)
                .testTag("drawer_about_section")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mahi AI v1.0",
                    color = TextPureWhite,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your personal AI assistant",
                color = TextSoftGray,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            // Subtle branding requested: "Made by Mizan"
            Text(
                text = "Made by Mizan",
                color = Color(0xFF93C5FD),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

@Composable
fun DrawerSectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF4C5B72),
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
    )
}

@Composable
fun DrawerRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFF8697AF),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = Color(0xFFD6DFEC),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif
        )
    }
}
