package com.example.musicplayer.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.musicplayer.domain.model.Song

@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["mediaStoreId"], unique = true),
        Index(value = ["artist"]),
        Index(value = ["album"]),
        Index(value = ["isFavorite"])
    ]
)
data class SongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaStoreId: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val path: String,
    val contentUriString: String,
    val albumArtUriString: String?,
    val dateAdded: Long,
    val year: Int,
    val trackNumber: Int,
    val size: Long,
    val mimeType: String,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedTimestamp: Long = 0
) {
    fun toDomain(): Song = Song(
        id = id,
        mediaStoreId = mediaStoreId,
        title = title,
        artist = artist,
        artistId = artistId,
        album = album,
        albumId = albumId,
        duration = duration,
        path = path,
        contentUriString = contentUriString,
        albumArtUriString = albumArtUriString,
        dateAdded = dateAdded,
        year = year,
        trackNumber = trackNumber,
        size = size,
        mimeType = mimeType,
        isFavorite = isFavorite,
        playCount = playCount,
        lastPlayedTimestamp = lastPlayedTimestamp
    )

    companion object {
        fun fromDomain(song: Song): SongEntity = SongEntity(
            id = song.id,
            mediaStoreId = song.mediaStoreId,
            title = song.title,
            artist = song.artist,
            artistId = song.artistId,
            album = song.album,
            albumId = song.albumId,
            duration = song.duration,
            path = song.path,
            contentUriString = song.contentUriString,
            albumArtUriString = song.albumArtUriString,
            dateAdded = song.dateAdded,
            year = song.year,
            trackNumber = song.trackNumber,
            size = song.size,
            mimeType = song.mimeType,
            isFavorite = song.isFavorite,
            playCount = song.playCount,
            lastPlayedTimestamp = song.lastPlayedTimestamp
        )
    }
}
