package com.example.musicplayer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.musicplayer.ui.theme.AccentGold

@Composable
fun AnimatedEqualizer(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    barColor: Color = AccentGold,
    barWidth: Dp = 3.dp,
    barSpacing: Dp = 2.dp,
    maxBarHeight: Dp = 18.dp
) {
    val transition = rememberInfiniteTransition(label = "equalizer")

    val bar1HeightFraction by if (isPlaying) {
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar1"
        )
    } else {
        rememberUpdatedState(0.35f)
    }

    val bar2HeightFraction by if (isPlaying) {
        transition.animateFloat(
            initialValue = 0.85f,
            targetValue = 0.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 550, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar2"
        )
    } else {
        rememberUpdatedState(0.7f)
    }

    val bar3HeightFraction by if (isPlaying) {
        transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 380, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar3"
        )
    } else {
        rememberUpdatedState(0.45f)
    }

    Row(
        modifier = modifier.height(maxBarHeight),
        horizontalArrangement = Arrangement.spacedBy(barSpacing),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(barWidth)
                .fillMaxHeight(bar1HeightFraction)
                .clip(RoundedCornerShape(barWidth / 2))
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(barWidth)
                .fillMaxHeight(bar2HeightFraction)
                .clip(RoundedCornerShape(barWidth / 2))
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(barWidth)
                .fillMaxHeight(bar3HeightFraction)
                .clip(RoundedCornerShape(barWidth / 2))
                .background(barColor)
        )
    }
}
