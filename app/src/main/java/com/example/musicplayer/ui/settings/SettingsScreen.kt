package com.example.musicplayer.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.ui.components.CurvedHeader
import com.example.musicplayer.ui.components.SleepTimerDialog
import com.example.musicplayer.ui.theme.AccentCyan
import com.example.musicplayer.ui.theme.AccentGold
import com.example.musicplayer.ui.theme.BackgroundDark

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var versionClickCount by remember { mutableIntStateOf(0) }
    var lastVersionClickTime by remember { mutableLongStateOf(0L) }
    var showPiracyQuoteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CurvedHeader(
            title = "Settings",
            subtitle = "Preferences and audio configuration",
            navigationIcon = Icons.Rounded.ArrowBack,
            onNavigationClick = onBackClick,
            headerHeight = 140.dp
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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

            // Playback Category
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCategoryHeader(title = "Playback")
            }

            item {
                val timerSubtitle = if (uiState.sleepTimerState.isActive) {
                    val minutesLeft = uiState.sleepTimerState.remainingMillis / 1000 / 60
                    "Active: ~$minutesLeft min remaining"
                } else {
                    "Set a timer to automatically pause playback"
                }

                SettingsClickableItem(
                    icon = Icons.Rounded.Timer,
                    title = "Sleep Timer",
                    subtitle = timerSubtitle,
                    onClick = { showSleepTimerDialog = true }
                )
            }

            // Library Category
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCategoryHeader(title = "Library")
            }

            item {
                SettingsClickableItem(
                    icon = Icons.Rounded.Refresh,
                    title = "Rescan Library",
                    subtitle = if (uiState.isRescanning) "Scanning media files..." else "Scan device storage for new songs and audio files",
                    onClick = { viewModel.rescanLibrary() }
                )
            }

            // About Category
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCategoryHeader(title = "About")
            }

            item {
                SettingsInfoItem(
                    icon = Icons.Rounded.Info,
                    title = "App Version",
                    value = "1.0.0",
                    onClick = {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastVersionClickTime < 2000L) {
                            versionClickCount++
                        } else {
                            versionClickCount = 1
                        }
                        lastVersionClickTime = currentTime

                        if (versionClickCount >= 7) {
                            showPiracyQuoteDialog = true
                            versionClickCount = 0
                        }
                    }
                )
            }

            item {
                SettingsInfoItem(
                    icon = Icons.Rounded.MusicNote,
                    title = "My Music Player",
                    value = "Designed for seamless offline audio"
                )
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = uiState.themeMode,
            onThemeSelected = {
                viewModel.setThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
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

    if (showPiracyQuoteDialog) {
        AlertDialog(
            onDismissRequest = { showPiracyQuoteDialog = false },
            title = {
                Text(
                    text = "Developer Quote",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "if buying isn’t owning, pirating isn’t stealing",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = AccentGold
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {
                TextButton(onClick = { showPiracyQuoteDialog = false }) {
                    Text("OK", color = AccentGold, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun SettingsCategoryHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = AccentGold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

@Composable
private fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsInfoItem(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: String,
    onThemeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choose App Theme",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column {
                val themes = listOf(
                    "SYSTEM" to "System Default",
                    "DARK" to "Dark Theme (Signature)",
                    "LIGHT" to "Light Theme"
                )

                themes.forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onThemeSelected(key) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == key,
                            onClick = { onThemeSelected(key) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AccentGold,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (currentTheme == key) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AccentGold, fontWeight = FontWeight.Bold)
            }
        }
    )
}
