package com.example.musicplayer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.ui.theme.AccentGold
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun WavyProgressBar(
    currentPositionMs: Long,
    totalDurationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = AccentGold,
    inactiveColor: Color = Color(0xFF383A42)
) {
    val progress = if (totalDurationMs > 0) {
        (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Start Time label (e.g. 2:33)
        Text(
            text = formatDuration(currentPositionMs),
            color = Color(0xFF8E929E),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(42.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Scrubber Area
        Box(
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
                .pointerInput(totalDurationMs) {
                    detectTapGestures { offset ->
                        val newFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((newFraction * totalDurationMs).toLong())
                    }
                }
                .pointerInput(totalDurationMs) {
                    detectHorizontalDragGestures { change, _ ->
                        val newFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeek((newFraction * totalDurationMs).toLong())
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val centerY = height / 2f
                val activeWidth = width * animatedProgress

                // 1. Inactive Track (straight sleek line)
                if (activeWidth < width) {
                    drawLine(
                        color = inactiveColor,
                        start = Offset(activeWidth.coerceAtLeast(0f), centerY),
                        end = Offset(width, centerY),
                        strokeWidth = 3.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // 2. Wavy Active Track (golden sinusoidal wave)
                if (activeWidth > 0f) {
                    val wavyPath = Path().apply {
                        moveTo(0f, centerY)
                        val waveLength = 22.dp.toPx()
                        val amplitude = 4.dp.toPx()
                        var x = 0f
                        while (x < activeWidth) {
                            val nextX = (x + waveLength).coerceAtMost(activeWidth)
                            val midX = (x + nextX) / 2f
                            val direction = if (((x / waveLength).toInt() % 2) == 0) -1f else 1f
                            quadraticTo(
                                midX, centerY + (amplitude * direction),
                                nextX, centerY
                            )
                            x = nextX
                        }
                    }

                    drawPath(
                        path = wavyPath,
                        color = activeColor,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 3. Vertical Capsule / Pill Scrubber Thumb (matching screenshot)
                    val thumbWidth = 4.5.dp.toPx()
                    val thumbHeight = 16.dp.toPx()
                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset(activeWidth - (thumbWidth / 2f), centerY - (thumbHeight / 2f)),
                        size = Size(thumbWidth, thumbHeight),
                        cornerRadius = CornerRadius(thumbWidth / 2f, thumbWidth / 2f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // End Time label (e.g. 6:00)
        Text(
            text = formatDuration(totalDurationMs),
            color = Color(0xFF8E929E),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(42.dp)
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds)
    val seconds = totalSeconds - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}
