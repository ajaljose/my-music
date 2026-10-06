package com.example.musicplayer.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.musicplayer.domain.model.Playlist

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isSystem: Boolean = false
) {
    fun toDomain(songCount: Int = 0): Playlist = Playlist(
        id = id,
        name = name,
        songCount = songCount,
        createdAt = createdAt,
        isSystem = isSystem
    )
}
