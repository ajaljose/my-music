package com.example.musicplayer.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.musicplayer.ui.components.SleepTimerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Appearance Category
            item {
                SettingsCategoryHeader(title = "Appearance")
            }

            item {
                val themeLabel = when (uiState.themeMode) {
                    "LIGHT" -> "Light Theme"
                    "DARK" -> "Dark Theme"
                    else -> "System Default"
                }

                SettingsClickableItem(
                    icon = Icons.Rounded.Palette,
                    title = "App Theme",
                    subtitle = themeLabel,
                    onClick = { showThemeDialog = true }
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    SettingsSwitchItem(
                        icon = Icons.Rounded.ColorLens,
                        title = "Dynamic Colors (Material You)",
                        subtitle = "Use colors derived from your device wallpaper",
                        checked = uiState.dynamicColor,
                        onCheckedChange = { viewModel.setDynamicColor(it) }
                    )
                }
            }

            // Playback Category
            item {
                Spacer(modifier = Modifier.height(12.dp))
                SettingsCategoryHeader(title = "Playback")
            }

            item {
                val timerSubtitle = if (uiState.sleepTimerState.isActive) {
                    val minutesLeft = uiState.sleepTimerState.remainingMillis / 1000 / 60
                    "Active: ~$minutesLeft min remaining"
                } else {
                    "Off"
                }

                SettingsClickableItem(
                    icon = Icons.Rounded.Timer,
                    title = "Sleep Timer",
                    subtitle = timerSubtitle,
                    onClick = { showSleepTimerDialog = true }
                )
            }

            item {
                SettingsSwitchItem(
                    icon = Icons.Rounded.Headphones,
                    title = "Pause on Disconnect",
                    subtitle = "Automatically pause playback when headphones or Bluetooth disconnect",
                    checked = uiState.pauseOnHeadphoneDisconnect,
                    onCheckedChange = { viewModel.setPauseOnHeadphoneDisconnect(it) }
                )
            }

            // Library Category
            item {
                Spacer(modifier = Modifier.height(12.dp))
                SettingsCategoryHeader(title = "Library")
            }

            item {
                SettingsClickableItem(
                    icon = Icons.Rounded.Refresh,
                    title = "Rescan Music Library",
                    subtitle = if (uiState.isRescanning) "Scanning files..." else "${uiState.totalSongs} audio tracks in library",
                    onClick = { viewModel.rescanLibrary() }
                )
            }

            // About Category
            item {
                Spacer(modifier = Modifier.height(12.dp))
                SettingsCategoryHeader(title = "About")
            }

            item {
                SettingsClickableItem(
                    icon = Icons.Rounded.Info,
                    title = "My Music Player",
                    subtitle = "Version 1.0.0 (Native Android with Jetpack Compose & Media3)",
                    onClick = {}
                )
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column {
                    listOf(
                        "SYSTEM" to "System Default",
                        "LIGHT" to "Light",
                        "DARK" to "Dark"
                    ).forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.themeMode == mode,
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            sleepTimerState = uiState.sleepTimerState,
            onSetTimerMinutes = { viewModel.startSleepTimer(it) },
            onSetTimerEndOfSong = { viewModel.startSleepTimerEndOfSong() },
            onCancelTimer = { viewModel.cancelSleepTimer() },
            onDismiss = { showSleepTimerDialog = false }
        )
    }
}

@Composable
private fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
