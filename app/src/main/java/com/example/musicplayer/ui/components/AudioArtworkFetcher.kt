package com.example.musicplayer.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.buffer
import okio.source

data class AudioArtwork(
    val artworkUri: Uri? = null,
    val songUri: Uri? = null,
    val path: String? = null
)

class AudioArtworkFetcher(
    private val context: Context,
    private val data: AudioArtwork,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        // 1. Try loading artworkUri if provided
        if (data.artworkUri != null) {
            try {
                val stream = context.contentResolver.openInputStream(data.artworkUri)
                if (stream != null) {
                    return SourceResult(
                        source = ImageSource(stream.source().buffer(), context),
                        mimeType = null,
                        dataSource = DataSource.DISK
                    )
                }
            } catch (_: Exception) {
                // Fallthrough to embedded picture extraction
            }
        }

        // 2. Fallback: extract embedded picture using MediaMetadataRetriever
        val retriever = MediaMetadataRetriever()
        try {
            if (data.songUri != null) {
                retriever.setDataSource(context, data.songUri)
            } else if (!data.path.isNullOrBlank()) {
                retriever.setDataSource(data.path)
            } else {
                return null
            }

            val picture = retriever.embeddedPicture
            if (picture != null) {
                val bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.size)
                if (bitmap != null) {
                    return DrawableResult(
                        drawable = BitmapDrawable(context.resources, bitmap),
                        isSampled = false,
                        dataSource = DataSource.DISK
                    )
                }
            }
        } catch (_: Exception) {
            // Ignore extraction errors
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
                // Ignore release errors
            }
        }

        return null
    }

    class Factory(private val context: Context) : Fetcher.Factory<AudioArtwork> {
        override fun create(data: AudioArtwork, options: Options, imageLoader: ImageLoader): Fetcher {
            return AudioArtworkFetcher(context, data, options)
        }
    }
}
