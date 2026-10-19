package com.example.musicplayer.data.media

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.LruCache
import com.example.musicplayer.data.local.dao.SongDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

/**
 * ContentProvider to safely serve album art images to external processes like Android Auto
 * without requiring permission sharing or raw file access.
 */
class AlbumArtContentProvider : ContentProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AlbumArtEntryPoint {
        fun songDao(): SongDao
    }

    companion object {
        const val AUTHORITY = "com.example.musicplayer.albumart"
        private const val CODE_SONG = 1
        private const val CODE_ALBUM = 2

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "song/#", CODE_SONG)
            addURI(AUTHORITY, "album/#", CODE_ALBUM)
        }

        fun getSongArtUri(songId: Long): Uri? {
            return runCatching { Uri.parse("content://$AUTHORITY/song/$songId") }.getOrNull()
        }

        fun getAlbumArtUri(albumId: Long): Uri? {
            return runCatching { Uri.parse("content://$AUTHORITY/album/$albumId") }.getOrNull()
        }

        // Cache up to 10MB of decoded artwork bytes
        private val artworkCache = object : LruCache<String, ByteArray>(10 * 1024 * 1024) {
            override fun sizeOf(key: String, value: ByteArray): Int = value.size
        }

        /**
         * Reusable helper to extract artwork bytes from a song's album art URI or audio file metadata.
         */
        fun loadArtworkBytes(
            context: Context,
            albumArtUriString: String?,
            contentUriString: String?,
            path: String?
        ): ByteArray? {
            // 1. Try MediaStore album art URI if present
            if (!albumArtUriString.isNullOrBlank()) {
                try {
                    val uri = Uri.parse(albumArtUriString)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bytes = stream.readBytes()
                        if (bytes.isNotEmpty()) return bytes
                    }
                } catch (_: Exception) {
                    // Fallthrough to embedded picture extraction
                }
            }

            // 2. Fallback: extract embedded picture using MediaMetadataRetriever
            try {
                val retriever = MediaMetadataRetriever()
                if (!contentUriString.isNullOrBlank()) {
                    retriever.setDataSource(context, Uri.parse(contentUriString))
                } else if (!path.isNullOrBlank()) {
                    retriever.setDataSource(path)
                } else {
                    return null
                }
                val picture = retriever.embeddedPicture
                retriever.release()
                if (picture != null && picture.isNotEmpty()) {
                    return picture
                }
            } catch (_: Exception) {
                // Ignore extraction failures
            }

            return null
        }
    }

    private var songDao: SongDao? = null

    override fun onCreate(): Boolean {
        return true
    }

    private fun getDao(): SongDao? {
        if (songDao == null) {
            val ctx = context?.applicationContext ?: return null
            try {
                val entryPoint = EntryPointAccessors.fromApplication(ctx, AlbumArtEntryPoint::class.java)
                songDao = entryPoint.songDao()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return songDao
    }

    override fun getType(uri: Uri): String = "image/jpeg"

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val match = uriMatcher.match(uri)
        if (match != CODE_SONG && match != CODE_ALBUM) {
            throw FileNotFoundException("Unsupported album art URI: $uri")
        }

        val cacheKey = uri.toString()
        val cached = artworkCache.get(cacheKey)
        if (cached != null) {
            return writeBytesToPipe(uri, cached)
        }

        val bytes = loadArtworkBytesForUri(uri, match)
        if (bytes != null && bytes.isNotEmpty()) {
            artworkCache.put(cacheKey, bytes)
            return writeBytesToPipe(uri, bytes)
        }

        throw FileNotFoundException("No album artwork found for: $uri")
    }

    private fun writeBytesToPipe(uri: Uri, bytes: ByteArray): ParcelFileDescriptor {
        return openPipeHelper(uri, "image/jpeg", null, bytes) { output, _, _, _, data ->
            try {
                FileOutputStream(output.fileDescriptor).use { stream ->
                    stream.write(data)
                    stream.flush()
                }
            } catch (_: IOException) {
                // Pipe broken or caller closed connection
            }
        }
    }

    private fun loadArtworkBytesForUri(uri: Uri, match: Int): ByteArray? {
        val ctx = context ?: return null
        val id = ContentUris.parseId(uri)

        if (match == CODE_SONG) {
            val dao = getDao() ?: return null
            val song = runBlocking { dao.getSongById(id) } ?: return null
            return loadArtworkBytes(ctx, song.albumArtUriString, song.contentUriString, song.path)
        } else if (match == CODE_ALBUM) {
            // 1. Try MediaStore album art URI directly
            try {
                val albumArtUri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, id)
                ctx.contentResolver.openInputStream(albumArtUri)?.use { stream ->
                    val bytes = stream.readBytes()
                    if (bytes.isNotEmpty()) return bytes
                }
            } catch (_: Exception) {}

            // 2. Try getting a song in the album and extracting its artwork
            val dao = getDao() ?: return null
            val songInAlbum = runBlocking {
                dao.getSongsByAlbumId(id).firstOrNull() ?: dao.getAllSongs().firstOrNull {
                    (it.albumId.takeIf { a -> a != 0L } ?: it.album.hashCode().toLong()) == id
                }
            }
            if (songInAlbum != null) {
                return loadArtworkBytes(ctx, songInAlbum.albumArtUriString, songInAlbum.contentUriString, songInAlbum.path)
            }
        }

        return null
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
