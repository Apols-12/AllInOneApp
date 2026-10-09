package com.example.documentscanner

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.documentscanner.screens.DocumentScannerScreen
import com.example.documentscanner.screens.SelfieSegmentationScreen
import com.example.documentscanner.screens.TranslationScreen
import kotlinx.coroutines.delay

@Composable
fun MainApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    val context = LocalContext.current
    var showMainContent by remember { mutableStateOf(false) }
    val adManager = remember { AppOpenAdManager(context) }

    if (showMainContent) {
        // Show main app content
        NavHost(
            modifier = modifier,
            navController = navController,
            startDestination = Screen.DocumentScanner.route
        ) {
            composable(Screen.DocumentScanner.route) {
                DocumentScannerScreen(
                    onNavigateToTranslation = {
                        navController.navigate(Screen.Translation.route)
                    },
                    onNavigateToSelfieSegmentation = {
                        navController.navigate(Screen.SelfieSegmentation.route)
                    }
                )
            }

            composable(Screen.Translation.route) {
                TranslationScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.SelfieSegmentation.route) {
                SelfieSegmentationScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }

    } else {
        // Show splash/loading screen
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()

            LaunchedEffect(Unit) {
                adManager.loadAd()
                delay(5000) // Small delay for initialization
                // Try to show ad
                adManager.showAdIfAvailable(
                    activity = context as MainActivity,
                    onAdDismissed = { showMainContent = true },
                    onAdFailed = { showMainContent = true }
                )
            }
        }
    }
}
