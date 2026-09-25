package com.example.ui.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.GeminiChatService
import com.example.ui.home.NavTabItem
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.TextPureWhite
import com.example.ui.theme.TextSoftGray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

@Composable
fun MahiScannerScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToMemories: () -> Unit,
    onNavigateToChat: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val chatService = remember { GeminiChatService(context) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    BackHandler {
        onNavigateToHome()
    }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var visionStatusText by remember { mutableStateOf("> STANDBY — start a session to give Mahi AI eyes 🎤") }
    var lastAnalysisResult by remember { mutableStateOf<String?>(null) }
    var selectedVisionMode by remember { mutableStateOf("What is this?") }

    val visionModes = listOf(
        "What is this?",
        "Read this text",
        "Describe what you see",
        "Find an object",
        "Explain this",
        "Translate this"
    )

    // Analyze snapshot helper
    fun analyzeCurrentFrame(prompt: String) {
        val capture = imageCapture
        if (capture == null) {
            visionStatusText = "> CAMERA NOT READY"
            return
        }

        isAnalyzing = true
        visionStatusText = "> CAPTURING & ANALYZING WITH GEMINI VISION..."

        capture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val bitmap = imageProxyToBitmap(imageProxy)
                    imageProxy.close()

                    CoroutineScope(Dispatchers.Main).launch {
                        val result = chatService.sendMessage(
                            prompt = prompt,
                            attachedBitmap = bitmap
                        )
                        isAnalyzing = false
                        result.onSuccess { reply ->
                            lastAnalysisResult = reply
                            visionStatusText = "> AI-FEED RESPONSE READY"
                        }.onFailure { err ->
                            lastAnalysisResult = "Vision analysis error: ${err.localizedMessage}"
                            visionStatusText = "> ANALYSIS ERROR"
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    isAnalyzing = false
                    visionStatusText = "> CAPTURE FAILED: ${exception.message}"
                }
            }
        )
    }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(hasCameraPermission, lensFacing, lifecycleOwner) {
        if (!hasCameraPermission) return@LaunchedEffect
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                imageCapture = capture

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    capture
                )
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("scanner_screen_root")
    ) {
        // ================= CAMERA PREVIEW LAYER =================
        if (hasCameraPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission request placeholder with camera style
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF090D18)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Camera Permission Required",
                        color = TextPureWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "To allow Mahi AI Vision to see objects and read text, please allow camera access.",
                        color = TextSoftGray,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text("Grant Camera Access")
                    }
                }
            }
        }

        // ================= FUTURISTIC HUD OVERLAYS =================
        FuturisticCameraHudOverlay(
            modifier = Modifier.fillMaxSize()
        )

        // ================= SCREEN CONTENTS OVERLAY =================
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // TOP HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("scanner_top_header"),
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
                    text = "Scanner",
                    color = TextPureWhite,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.testTag("scanner_title")
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

            // Status Bar Overlays (Top-left & Top-right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MAHI VISION",
                            color = Color(0xFF60A5FA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = visionStatusText,
                        color = Color(0xFFC7D2FE),
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Lens indicator
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (lensFacing == CameraSelector.LENS_FACING_BACK) "BACK CAM" else "FRONT CAM",
                        color = Color(0xFF93C5FD),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Quick Vision Mode Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visionModes) { mode ->
                    val isSelected = mode == selectedVisionMode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) Color(0xFF1E3A8A).copy(alpha = 0.85f)
                                else Color(0xFF0F172A).copy(alpha = 0.75f)
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF60A5FA) else Color(0xFF1E293B)
                                ),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                selectedVisionMode = mode
                                analyzeCurrentFrame(mode)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = mode,
                            color = if (isSelected) Color(0xFF93C5FD) else Color(0xFFCBD5E1),
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // AI Vision Response Card (if ready)
            if (lastAnalysisResult != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0C1424).copy(alpha = 0.90f))
                        .border(BorderStroke(1.dp, Color(0xFF1E3A66)), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Mahi Vision Analysis",
                                color = Color(0xFF60A5FA),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "✕",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                modifier = Modifier.clickable { lastAnalysisResult = null }
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = lastAnalysisResult.orEmpty(),
                            color = TextPureWhite,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // HUD Technical Readout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "FRM 004211   SIG 83",
                    color = Color(0xFF38BDF8).copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (isAnalyzing) "AI-FEED ANALYZING..." else "AI-FEED ONLINE",
                    color = if (isAnalyzing) Color(0xFFFBBF24) else Color(0xFF4ADE80),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // ================= BOTTOM CONTROLS =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 28.dp, vertical = 10.dp)
                    .testTag("scanner_controls_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // FLIP Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F1A2C))
                            .border(BorderStroke(1.dp, Color(0xFF1E3A66)), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Cameraswitch,
                            contentDescription = "Flip Camera",
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "FLIP",
                        color = Color(0xFF93C5FD),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Center Glowing Microphone / Trigger Button
                Box(
                    modifier = Modifier
                        .size(68.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Soft white/blue radial halo glow
                    Canvas(modifier = Modifier.size(68.dp)) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.75f),
                                    Color(0xFF60A5FA).copy(alpha = 0.45f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.minDimension / 2f
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFF60A5FA),
                                        ElectricBlue,
                                        Color(0xFF1D4ED8)
                                    )
                                )
                            )
                            .border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.8f)), CircleShape)
                            .clickable {
                                analyzeCurrentFrame(selectedVisionMode)
                            }
                            .testTag("scanner_mic_trigger"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = TextPureWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Mic,
                                contentDescription = "Voice Vision Trigger",
                                tint = TextPureWhite,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // REC Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        analyzeCurrentFrame("Describe this view in detail.")
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F1A2C))
                            .border(BorderStroke(1.dp, Color(0xFF1E3A66)), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Videocam,
                            contentDescription = "Capture/Record",
                            tint = Color(0xFFE2E8F0),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "REC",
                        color = Color(0xFF93C5FD),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // ================= BOTTOM NAVIGATION BAR =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF070B14))
                    .padding(vertical = 6.dp, horizontal = 12.dp)
                    .testTag("scanner_bottom_nav"),
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
                    isSelected = true,
                    onClick = {}
                )
                NavTabItem(
                    label = "Memories",
                    icon = Icons.Outlined.Psychology,
                    isSelected = false,
                    onClick = onNavigateToMemories
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
}

/**
 * Draws the futuristic camera HUD overlay:
 * - Corner brackets
 * - Outer circular scanning reticle
 * - Inner segmented arcs
 * - Center targeting dot and crosshairs
 */
@Composable
fun FuturisticCameraHudOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reticle_rotation"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h * 0.44f)
        val hudBlue = Color(0xFF4E85FF)
        val lightBlue = Color(0xFF60A5FA)

        // Corner Brackets Top-Left
        drawLine(hudBlue, Offset(24f, 90f), Offset(60f, 90f), strokeWidth = 2.dp.toPx())
        drawLine(hudBlue, Offset(24f, 90f), Offset(24f, 130f), strokeWidth = 2.dp.toPx())

        // Corner Brackets Top-Right
        drawLine(hudBlue, Offset(w - 60f, 90f), Offset(w - 24f, 90f), strokeWidth = 2.dp.toPx())
        drawLine(hudBlue, Offset(w - 24f, 90f), Offset(w - 24f, 130f), strokeWidth = 2.dp.toPx())

        // Corner Brackets Bottom-Left
        drawLine(hudBlue, Offset(24f, h - 160f), Offset(24f, h - 120f), strokeWidth = 2.dp.toPx())
        drawLine(hudBlue, Offset(24f, h - 120f), Offset(60f, h - 120f), strokeWidth = 2.dp.toPx())

        // Corner Brackets Bottom-Right
        drawLine(hudBlue, Offset(w - 24f, h - 160f), Offset(w - 24f, h - 120f), strokeWidth = 2.dp.toPx())
        drawLine(hudBlue, Offset(w - 60f, h - 120f), Offset(w - 24f, h - 120f), strokeWidth = 2.dp.toPx())

        // Circular Scanning Reticle (Center)
        val outerRadius = w * 0.38f
        val innerRadius = w * 0.28f

        // Outer Dashed Arc (4 segments)
        drawArc(
            color = lightBlue.copy(alpha = 0.55f),
            startAngle = rotation,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
            size = androidx.compose.ui.geometry.Size(outerRadius * 2, outerRadius * 2),
            style = Stroke(
                width = 1.8.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
            )
        )
        drawArc(
            color = lightBlue.copy(alpha = 0.55f),
            startAngle = rotation + 180f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
            size = androidx.compose.ui.geometry.Size(outerRadius * 2, outerRadius * 2),
            style = Stroke(
                width = 1.8.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))
            )
        )

        // Inner Sharp Bracket Arcs
        val bracketWidth = 2.5.dp.toPx()
        // Left arc bracket
        drawArc(
            color = hudBlue,
            startAngle = 150f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
            size = androidx.compose.ui.geometry.Size(innerRadius * 2, innerRadius * 2),
            style = Stroke(width = bracketWidth, cap = StrokeCap.Round)
        )
        // Right arc bracket
        drawArc(
            color = hudBlue,
            startAngle = 330f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
            size = androidx.compose.ui.geometry.Size(innerRadius * 2, innerRadius * 2),
            style = Stroke(width = bracketWidth, cap = StrokeCap.Round)
        )
        // Top arc bracket
        drawArc(
            color = hudBlue,
            startAngle = 250f,
            sweepAngle = 40f,
            useCenter = false,
            topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
            size = androidx.compose.ui.geometry.Size(innerRadius * 2, innerRadius * 2),
            style = Stroke(width = bracketWidth, cap = StrokeCap.Round)
        )
        // Bottom arc bracket
        drawArc(
            color = hudBlue,
            startAngle = 70f,
            sweepAngle = 40f,
            useCenter = false,
            topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
            size = androidx.compose.ui.geometry.Size(innerRadius * 2, innerRadius * 2),
            style = Stroke(width = bracketWidth, cap = StrokeCap.Round)
        )

        // Center Crosshair Lines
        val crossLen = 14.dp.toPx()
        drawLine(hudBlue, Offset(center.x - innerRadius, center.y), Offset(center.x - innerRadius + crossLen, center.y), strokeWidth = 1.5.dp.toPx())
        drawLine(hudBlue, Offset(center.x + innerRadius - crossLen, center.y), Offset(center.x + innerRadius, center.y), strokeWidth = 1.5.dp.toPx())
        drawLine(hudBlue, Offset(center.x, center.y - innerRadius), Offset(center.x, center.y - innerRadius + crossLen), strokeWidth = 1.5.dp.toPx())
        drawLine(hudBlue, Offset(center.x, center.y + innerRadius - crossLen), Offset(center.x, center.y + innerRadius), strokeWidth = 1.5.dp.toPx())

        // Center Targeting Dot
        drawCircle(
            color = Color(0xFF60A5FA),
            radius = 3.5.dp.toPx(),
            center = center
        )
    }
}

/**
 * Utility to convert CameraX ImageProxy to Bitmap.
 */
fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val planeProxy = image.planes[0]
    val buffer: ByteBuffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    return if (image.imageInfo.rotationDegrees != 0) {
        val matrix = Matrix().apply {
            postRotate(image.imageInfo.rotationDegrees.toFloat())
        }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }
}
