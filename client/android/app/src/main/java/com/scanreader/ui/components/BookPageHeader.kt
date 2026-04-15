package com.scanreader.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scanreader.model.ViewMode

/** Header shown during reading — font size controls + view mode toggle + overflow menu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookPageHeader(
    currentMode: ViewMode,
    onModeChange: (ViewMode) -> Unit,
    onFontDecrease: () -> Unit,
    onFontIncrease: () -> Unit,
    onSettingsClick: () -> Unit,
    onBack: () -> Unit
) {
    TopAppBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(94.dp),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ViewMode.entries.forEach { mode ->
                    val label = when (mode) {
                        ViewMode.ORIGINAL -> "Img"
                        ViewMode.REFLOW -> "Flow"
                        ViewMode.TEXT -> "Txt"
                    }
                    val isSelected = mode == currentMode
                    IconButton(onClick = { onModeChange(mode) }) {
                        Text(
                            text = label,
                            fontSize = if (isSelected) 18.sp else 14.sp,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        },
        actions = {
            Row(modifier = Modifier.padding(end = 4.dp)) {
                IconButton(onClick = onFontDecrease) {
                    Text("A", fontSize = 14.sp)
                }
                IconButton(onClick = onFontIncrease) {
                    Text("A", fontSize = 18.sp)
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Settings")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}
