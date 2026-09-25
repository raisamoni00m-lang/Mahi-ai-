package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MahiTheme
import com.example.ui.theme.TextPureWhite
import com.example.ui.theme.TextSoftGray

@Composable
fun MahiSetupScreen(
    viewModel: SetupMahiViewModel,
    modifier: Modifier = Modifier,
    onContinueNext: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    // If marked complete, invoke continuation callback
    if (state.isComplete) {
        onContinueNext()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkNavyBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("setup_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ================= HEADER =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_header"),
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
                        modifier = Modifier.testTag("header_title")
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Step ${state.currentStep} of ${state.totalSteps}",
                        color = TextSoftGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.testTag("header_step")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3-section progress bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("progress_bar"),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Section 1 - Active blue
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ElectricBlue)
                        .testTag("progress_step_1")
                )
                // Section 2 - Inactive dark navy
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF1A263B))
                        .testTag("progress_step_2")
                )
                // Section 3 - Inactive dark navy
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF1A263B))
                        .testTag("progress_step_3")
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // ================= MAIN TITLE & DESCRIPTION =================
            Text(
                text = "Add your Gemini key",
                color = TextPureWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.testTag("main_title")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Mahi AI’s voice and brain run on Google’s Gemini. Without a key she cannot hear you or answer. The key is free and takes about a minute.",
                color = TextSoftGray,
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.testTag("main_description")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ================= INSTRUCTIONS =================
            InstructionItem(
                stepNumber = "1",
                text = "Tap \"Get a free key\" — Google AI Studio opens. Sign in with any Google account.",
                testTag = "instruction_1"
            )

            Spacer(modifier = Modifier.height(12.dp))

            InstructionItem(
                stepNumber = "2",
                text = "Tap \"Create API key\", pick any project, and copy the key. It starts with \"AQ.\" (older keys may start with \"AIza\").",
                testTag = "instruction_2"
            )

            Spacer(modifier = Modifier.height(12.dp))

            InstructionItem(
                stepNumber = "3",
                text = "Come back and paste it below — the key itself, not your name or email.",
                testTag = "instruction_3"
            )

            Spacer(modifier = Modifier.height(22.dp))

            // ================= "GET A FREE KEY" BUTTON =================
            OutlinedButton(
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://aistudio.google.com/app/apikey")
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("get_key_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF23324A)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF0E1626)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = "Open Google AI Studio",
                        tint = TextPureWhite,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Get a free key",
                        color = TextPureWhite,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ================= API KEY INPUT =================
            val inputBorderColor = when {
                state.isKeyValid -> Color(0xFF22C55E)
                state.isError -> Color(0xFFEF4444)
                else -> Color(0xFF202E44)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF131B2A))
                    .border(BorderStroke(1.dp, inputBorderColor), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp)
                    .testTag("api_key_input_container"),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (state.apiKey.isEmpty()) {
                            Text(
                                text = "AQ....",
                                color = Color(0xFF4F5C72),
                                fontSize = 15.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }

                        BasicTextField(
                            value = state.apiKey,
                            onValueChange = { viewModel.onApiKeyChanged(it) },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = TextPureWhite,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.SansSerif
                            ),
                            cursorBrush = SolidColor(ElectricBlue),
                            visualTransformation = if (state.isKeyObscured) PasswordVisualTransformation() else VisualTransformation.None,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    if (state.apiKey.isNotBlank()) {
                                        viewModel.testKey()
                                    }
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api_key_text_field")
                        )
                    }

                    // Lock Icon with ability to toggle preview
                    IconButton(
                        onClick = { viewModel.toggleKeyVisibility() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("lock_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (state.isKeyObscured) Icons.Outlined.Lock else Icons.Outlined.Visibility,
                            contentDescription = if (state.isKeyObscured) "API Key masked" else "API Key visible",
                            tint = if (state.isKeyObscured) Color(0xFF6B7A92) else ElectricBlue,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ================= "TEST KEY" BUTTON =================
            val isTestEnabled = state.apiKey.trim().isNotEmpty() && !state.isValidating

            OutlinedButton(
                onClick = {
                    keyboardController?.hide()
                    viewModel.testKey()
                },
                enabled = isTestEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .alpha(if (isTestEnabled) 1f else 0.45f)
                    .testTag("test_key_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF23324A)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFF0E1626),
                    disabledContainerColor = Color(0xFF0E1626)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (state.isValidating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = ElectricBlue,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Testing key...",
                            color = TextPureWhite,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Test key icon",
                            tint = TextPureWhite,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Test key",
                            color = TextPureWhite,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }

            // ================= FEEDBACK MESSAGE =================
            AnimatedVisibility(
                visible = state.feedbackMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val isSuccess = state.isKeyValid
                val cardBg = if (isSuccess) Color(0xFF0D251C) else Color(0xFF2A1216)
                val cardBorder = if (isSuccess) Color(0xFF1E523A) else Color(0xFF5A2229)
                val cardText = if (isSuccess) Color(0xFF4ADE80) else Color(0xFFF87171)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(cardBg)
                        .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("feedback_banner"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Error,
                        contentDescription = null,
                        tint = cardText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.feedbackMessage.orEmpty(),
                        color = cardText,
                        fontSize = 12.5.sp,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ================= BOTTOM INFORMATION =================
            Text(
                text = "The key is free. It is stored encrypted and used only to run Mahi AI for you.",
                color = Color(0xFF758399),
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .testTag("bottom_information")
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ================= BOTTOM ACTION =================
            Button(
                onClick = { viewModel.saveAndContinue() },
                enabled = state.isKeyValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_and_continue_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricBlue,
                    disabledContainerColor = Color(0xFF141D2D),
                    contentColor = TextPureWhite,
                    disabledContentColor = Color(0xFF465369)
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
                        text = "Save & continue",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ================= BOTTOM SKIP OPTION =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .testTag("skip_row"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Added a key on this phone before? ",
                    color = Color(0xFF758399),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "Skip",
                    color = Color(0xFFB4CDF7),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            viewModel.skip()
                        }
                        .testTag("skip_button")
                )
            }
        }
    }
}

@Composable
fun InstructionItem(
    stepNumber: String,
    text: String,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        verticalAlignment = Alignment.Top
    ) {
        // Circle badge
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xFF16233B)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = Color(0xFF5B92FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            color = Color(0xFFCBD6E4),
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F19, widthDp = 390, heightDp = 844)
@Composable
fun MahiSetupScreenPreview() {
    MahiTheme {
        // Preview layout with default parameters
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkNavyBackground)
        )
    }
}
