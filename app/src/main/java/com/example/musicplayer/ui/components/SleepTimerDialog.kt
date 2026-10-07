package com.example.musicplayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.domain.model.SleepTimerState
import com.example.musicplayer.ui.theme.AccentCyan
import com.example.musicplayer.ui.theme.AccentGold

@Composable
fun SleepTimerDialog(
    sleepTimerState: SleepTimerState,
    onSetTimerMinutes: (Int) -> Unit,
    onSetTimerEndOfSong: () -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    var showCustomInput by remember { mutableStateOf(false) }
    var customMinutesText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = Color(0xFF2C313B)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Sleep Timer",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        containerColor = Color(0xFF22242B),
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (sleepTimerState.isActive) {
                    val minutesLeft = sleepTimerState.remainingMillis / 1000 / 60
                    val secondsLeft = (sleepTimerState.remainingMillis / 1000) % 60
                    val timerLabel = if (sleepTimerState.finishEndOfSong) {
                        "Stopping at end of current song"
                    } else {
                        String.format("Active: %02d:%02d remaining", minutesLeft, secondsLeft)
                    }

                    Text(
                        text = timerLabel,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = {
                            onCancelTimer()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Turn Off Sleep Timer", fontWeight = FontWeight.Bold)
                    }
                } else if (showCustomInput) {
                    OutlinedTextField(
                        value = customMinutesText,
                        onValueChange = { customMinutesText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Minutes (1-480)", color = Color(0xFF8E929E)) },
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            cursorColor = AccentGold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCustomInput = false }) {
                            Text("Back", color = Color(0xFF8E929E))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val mins = customMinutesText.toIntOrNull()
                                if (mins != null && mins > 0) {
                                    onSetTimerMinutes(mins)
                                    onDismiss()
                                }
                            },
                            enabled = (customMinutesText.toIntOrNull() ?: 0) > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Start", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    val presets = listOf(15, 30, 45, 60, 90)
                    presets.forEach { minutes ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onSetTimerMinutes(minutes)
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp, horizontal = 6.dp)
                        ) {
                            Text(
                                text = "$minutes minutes",
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onSetTimerEndOfSong()
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 6.dp)
                    ) {
                        Text(
                            text = "End of current song",
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showCustomInput = true }
                            .padding(vertical = 12.dp, horizontal = 6.dp)
                    ) {
                        Text(
                            text = "Custom time...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentGold
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF8E929E))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
