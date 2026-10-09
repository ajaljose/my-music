package com.example.musicplayer.domain.model

import android.net.Uri

data class Song(
    val id: Long = 0,
    val mediaStoreId: Long,
    val title: String,
    val artist: String,
    val artistId: Long = 0,
    val album: String,
    val albumId: Long = 0,
    val duration: Long, // in milliseconds
    val path: String,
    val contentUriString: String,
    val albumArtUriString: String? = null,
    val dateAdded: Long = 0,
    val year: Int = 0,
    val composer: String = "",
    val trackNumber: Int = 0,
    val size: Long = 0,
    val mimeType: String = "audio/mpeg",
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedTimestamp: Long = 0
) {
    val contentUri: Uri get() = Uri.parse(contentUriString)
    val albumArtUri: Uri? get() = albumArtUriString?.let { Uri.parse(it) }

    val formattedDuration: String
        get() {
            val totalSeconds = duration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes % 60, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }
}
