package com.example.musicplayer.domain.model

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val artistId: Long = 0,
    val songCount: Int,
    val year: Int = 0,
    val albumArtUriString: String? = null,
    val firstSongContentUriString: String? = null,
    val firstSongPath: String? = null
)
