package com.videonote.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.videonote.presentation.IntentHandler
import com.videonote.presentation.screens.DirectorySettingsScreen
import com.videonote.presentation.screens.MainScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.videonote.presentation.viewmodel.DirectorySettingsViewModel

@Composable
fun VideoNoteNavigation(
    navController: NavHostController = rememberNavController()
) {
    val navigationState = remember { NavigationState() }

    // 处理待处理的文件导入
    LaunchedEffect(Unit) {
        IntentHandler.pendingFileImport?.let { importInfo ->
            navigationState.pendingFileImport = importInfo
        }
    }

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }
        composable("settings") {
            val viewModel: DirectorySettingsViewModel = hiltViewModel()
            DirectorySettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

private const val TAG = "VideoNote.Navigation"
