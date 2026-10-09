package com.example.musicplayer.data.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.musicplayer.data.local.entities.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun scanAudioFiles(): List<SongEntity> = withContext(Dispatchers.IO) {
        val songList = mutableListOf<SongEntity>()

        val collectionUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.COMPOSER,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.IS_MUSIC
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            context.contentResolver.query(
                collectionUri,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val artistIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val yearColumn = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
                val composerColumn = cursor.getColumnIndex(MediaStore.Audio.Media.COMPOSER)
                val trackColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeTypeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)

                val artworkUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI

                while (cursor.moveToNext()) {
                    val mediaStoreId = cursor.getLong(idColumn)
                    val rawTitle = cursor.getString(titleColumn)
                    val rawArtist = cursor.getString(artistColumn)
                    val artistId = if (artistIdColumn != -1) cursor.getLong(artistIdColumn) else 0L
                    val rawAlbum = cursor.getString(albumColumn)
                    val albumId = if (albumIdColumn != -1) cursor.getLong(albumIdColumn) else 0L
                    val duration = cursor.getLong(durationColumn)
                    val path = cursor.getString(dataColumn) ?: ""
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val year = if (yearColumn != -1 && !cursor.isNull(yearColumn)) cursor.getInt(yearColumn) else 0
                    val rawComposer = if (composerColumn != -1 && !cursor.isNull(composerColumn)) cursor.getString(composerColumn) else null
                    val composer = if (!rawComposer.isNullOrBlank() && rawComposer != "<unknown>") rawComposer.trim() else ""
                    val track = if (trackColumn != -1 && !cursor.isNull(trackColumn)) cursor.getInt(trackColumn) else 0
                    val size = cursor.getLong(sizeColumn)
                    val mimeType = if (mimeTypeColumn != -1 && !cursor.isNull(mimeTypeColumn)) {
                        cursor.getString(mimeTypeColumn) ?: "audio/mpeg"
                    } else "audio/mpeg"

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        mediaStoreId
                    )

                    val albumArtUriString = if (albumId > 0) {
                        ContentUris.withAppendedId(artworkUri, albumId).toString()
                    } else null

                    // Fallbacks for missing metadata
                    val title = if (!rawTitle.isNullOrBlank() && rawTitle != "<unknown>") {
                        rawTitle.trim()
                    } else {
                        File(path).nameWithoutExtension.ifBlank { "Unknown Title" }
                    }

                    val artist = if (!rawArtist.isNullOrBlank() && rawArtist != "<unknown>") {
                        rawArtist.trim()
                    } else {
                        "Unknown Artist"
                    }

                    val album = if (!rawAlbum.isNullOrBlank() && rawAlbum != "<unknown>") {
                        rawAlbum.trim()
                    } else {
                        "Unknown Album"
                    }

                    songList.add(
                        SongEntity(
                            mediaStoreId = mediaStoreId,
                            title = title,
                            artist = artist,
                            artistId = artistId,
                            album = album,
                            albumId = albumId,
                            duration = duration,
                            path = path,
                            contentUriString = contentUri.toString(),
                            albumArtUriString = albumArtUriString,
                            dateAdded = dateAdded,
                            year = year,
                            composer = composer,
                            trackNumber = track,
                            size = size,
                            mimeType = mimeType,
                            isFavorite = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        songList
    }
}
