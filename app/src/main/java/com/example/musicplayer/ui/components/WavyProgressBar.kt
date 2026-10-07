package com.example.musicplayer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun WavyProgressBar(
    currentPositionMs: Long,
    totalDurationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFFFFD54F),
    inactiveColor: Color = Color(0xFF424242)
) {
    val progress = if (totalDurationMs > 0) {
        (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
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
            Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                val width = size.width
                val height = size.height
                val centerY = height / 2f
                val activeWidth = width * animatedProgress

                // 1. Inactive Line (remaining track)
                if (activeWidth < width) {
                    drawLine(
                        color = inactiveColor,
                        start = Offset(activeWidth, centerY),
                        end = Offset(width, centerY),
                        strokeWidth = 3.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // 2. Wavy Active Track (played track)
                if (activeWidth > 0f) {
                    val wavyPath = Path().apply {
                        moveTo(0f, centerY)
                        val waveLength = 20.dp.toPx()
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

                    // 3. Thumb
                    drawCircle(
                        color = activeColor,
                        radius = 6.5.dp.toPx(),
                        center = Offset(activeWidth, centerY)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(currentPositionMs),
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray.copy(alpha = 0.8f)
            )
            Text(
                text = formatDuration(totalDurationMs),
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray.copy(alpha = 0.8f)
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds)
    val seconds = totalSeconds - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}
