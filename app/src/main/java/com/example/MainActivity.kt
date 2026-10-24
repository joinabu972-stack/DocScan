package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.DocScanViewModel
import com.example.ui.Screen
import com.example.ui.screens.CameraScannerScreen
import com.example.ui.screens.CloudSyncVaultScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DocumentDetailScreen
import com.example.ui.screens.FolderManagerScreen
import com.example.ui.screens.PageEditorScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DocScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DocScanApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun DocScanApp(viewModel: DocScanViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            is Screen.Dashboard -> DashboardScreen(viewModel = viewModel)
            is Screen.CameraScanner -> CameraScannerScreen(viewModel = viewModel)
            is Screen.PageEditor -> PageEditorScreen(viewModel = viewModel)
            is Screen.DocumentDetail -> DocumentDetailScreen(docId = screen.docId, viewModel = viewModel)
            is Screen.CloudSyncVault -> CloudSyncVaultScreen(viewModel = viewModel)
            is Screen.FolderManager -> FolderManagerScreen(viewModel = viewModel)
        }
    }
}
