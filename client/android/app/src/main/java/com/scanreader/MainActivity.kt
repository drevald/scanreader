package com.scanreader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.scanreader.navigation.AppNavigation
import com.scanreader.ui.theme.ScanReaderTheme

class MainActivity : ComponentActivity() {

    private val openUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openUri.value = if (intent?.action == Intent.ACTION_VIEW) intent.data else null
        setContent {
            ScanReaderTheme {
                AppNavigation(openUri = openUri.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_VIEW) {
            openUri.value = intent.data
        }
    }
}
