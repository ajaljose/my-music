package com.example.musicplayer.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import kotlin.math.abs

@Composable
fun ArtworkImage(
    artworkUri: Uri?,
    modifier: Modifier = Modifier,
    songUri: Uri? = null,
    path: String? = null,
    size: Dp = 48.dp,
    shape: Shape = RoundedCornerShape(8.dp),
    contentDescription: String? = null,
    seed: String = ""
) {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                add(AudioArtworkFetcher.Factory(context))
            }
            .build()
    }

    val model = remember(artworkUri, songUri, path) {
        AudioArtwork(
            artworkUri = artworkUri,
            songUri = songUri,
            path = path
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        var isSuccess by remember(model) { mutableStateOf(false) }

        AsyncImage(
            model = model,
            imageLoader = imageLoader,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onState = { state ->
                isSuccess = state is AsyncImagePainter.State.Success
            }
        )

        if (!isSuccess) {
            DefaultArtworkPlaceholder(seed = seed)
        }
    }
}

@Composable
fun DefaultArtworkPlaceholder(
    seed: String = "",
    modifier: Modifier = Modifier
) {
    val gradientColors = remember(seed) {
        val hash = seed.hashCode()
        when (abs(hash) % 4) {
            0 -> listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))
            1 -> listOf(Color(0xFF11998E), Color(0xFF38EF7D))
            2 -> listOf(Color(0xFF4A00E0), Color(0xFF8E2DE2))
            else -> listOf(Color(0xFFFF416C), Color(0xFFFF4B2B))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.fillMaxSize(0.5f)
        )
    }
}
