package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.SecureKeyStorage
import com.example.ui.MahiLoadingScreen
import com.example.ui.MahiPermissionsScreen
import com.example.ui.MahiSetupScreen
import com.example.ui.MahiStep3Screen
import com.example.ui.SetupMahiViewModel
import com.example.ui.home.MahiHomeScreen
import com.example.ui.home.MahiHomeViewModel
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.MahiTheme
import kotlinx.coroutines.delay

enum class ScreenState {
    SPLASH_LOADING,
    SETUP_GEMINI_KEY,      // Step 1 of 3
    SETUP_PERMISSIONS,     // Step 2 of 3
    SETUP_BACKGROUND_STEP3,// Step 3 of 3
    HOME_MAIN              // Main Futuristic Home Screen
}

class MainActivity : ComponentActivity() {

    private val setupViewModel: SetupMahiViewModel by viewModels()
    private val homeViewModel: MahiHomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MahiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkNavyBackground
                ) {
                    MahiAppNavHost(
                        setupViewModel = setupViewModel,
                        homeViewModel = homeViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun MahiAppNavHost(
    setupViewModel: SetupMahiViewModel,
    homeViewModel: MahiHomeViewModel
) {
    val context = LocalContext.current
    val storage = remember { SecureKeyStorage(context) }
    var currentScreen by remember { mutableStateOf(ScreenState.SPLASH_LOADING) }

    // Brief splash screen on initial startup, then navigate directly to Home in Free Mode
    LaunchedEffect(Unit) {
        delay(1200)
        currentScreen = ScreenState.HOME_MAIN
    }

    androidx.activity.compose.BackHandler(enabled = currentScreen != ScreenState.HOME_MAIN && currentScreen != ScreenState.SPLASH_LOADING) {
        currentScreen = ScreenState.HOME_MAIN
    }

    Crossfade(
        targetState = currentScreen,
        animationSpec = tween(durationMillis = 350),
        label = "screen_crossfade"
    ) { screen ->
        when (screen) {
            ScreenState.SPLASH_LOADING -> {
                MahiLoadingScreen()
            }
            ScreenState.SETUP_GEMINI_KEY -> {
                MahiSetupScreen(
                    viewModel = setupViewModel,
                    onContinueNext = {
                        currentScreen = ScreenState.SETUP_PERMISSIONS
                    }
                )
            }
            ScreenState.SETUP_PERMISSIONS -> {
                MahiPermissionsScreen(
                    onContinueToStep3 = {
                        currentScreen = ScreenState.SETUP_BACKGROUND_STEP3
                    }
                )
            }
            ScreenState.SETUP_BACKGROUND_STEP3 -> {
                MahiStep3Screen(
                    onFinishSetup = {
                        storage.setOnboardingCompleted(true)
                        currentScreen = ScreenState.HOME_MAIN
                    }
                )
            }
            ScreenState.HOME_MAIN -> {
                MahiHomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToPermissions = {
                        currentScreen = ScreenState.SETUP_PERMISSIONS
                    },
                    onNavigateToSetup = {
                        currentScreen = ScreenState.SETUP_GEMINI_KEY
                    }
                )
            }
        }
    }
}
