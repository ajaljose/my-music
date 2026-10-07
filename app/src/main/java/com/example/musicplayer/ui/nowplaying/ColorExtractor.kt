package com.example.musicplayer.ui.nowplaying

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun extractProminentColor(
    context: Context,
    artworkUri: Uri?,
    songUri: Uri?,
    path: String?
): Color {
    return withContext(Dispatchers.IO) {
        var bitmap: Bitmap? = null

        // 1. Try artworkUri
        if (artworkUri != null) {
            try {
                context.contentResolver.openInputStream(artworkUri)?.use { stream ->
                    bitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {}
        }

        // 2. Try MediaMetadataRetriever if bitmap is null
        if (bitmap == null) {
            val retriever = MediaMetadataRetriever()
            try {
                if (songUri != null) {
                    retriever.setDataSource(context, songUri)
                } else if (!path.isNullOrBlank()) {
                    retriever.setDataSource(path)
                }
                val picture = retriever.embeddedPicture
                if (picture != null) {
                    bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.size)
                }
            } catch (_: Exception) {
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
        }

        if (bitmap != null) {
            try {
                val palette = Palette.from(bitmap!!).generate()
                val dominantRgb = palette.getDominantColor(0xFF008B9B.toInt())
                val vibrantRgb = palette.getVibrantColor(dominantRgb)
                val darkVibrantRgb = palette.getDarkVibrantColor(vibrantRgb)
                val selectedRgb = if (vibrantRgb != dominantRgb) vibrantRgb else darkVibrantRgb
                Color(selectedRgb)
            } catch (_: Exception) {
                Color(0xFF008B9B)
            }
        } else {
            Color(0xFF008B9B)
        }
    }
}
