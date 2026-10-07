package com.example.musicplayer.playback.service

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MusicServiceCallback @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaLibraryService.MediaLibrarySession.Callback {

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().build()
        val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon().build()
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(sessionCommands)
            .setAvailablePlayerCommands(playerCommands)
            .build()
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> {
        val updatedMediaItems = mediaItems.map { mediaItem ->
            val artworkData = if (mediaItem.mediaMetadata.artworkData == null) {
                getArtworkBytes(context, mediaItem)
            } else {
                mediaItem.mediaMetadata.artworkData
            }

            val updatedMetadata = if (artworkData != null) {
                mediaItem.mediaMetadata.buildUpon()
                    .setArtworkData(artworkData, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    .build()
            } else {
                mediaItem.mediaMetadata
            }

            val baseItem = if (mediaItem.localConfiguration?.uri != null) {
                mediaItem
            } else {
                val uri = mediaItem.requestMetadata.mediaUri ?: Uri.parse(mediaItem.mediaId)
                mediaItem.buildUpon().setUri(uri).build()
            }

            baseItem.buildUpon()
                .setMediaMetadata(updatedMetadata)
                .build()
        }.toMutableList()
        return Futures.immediateFuture(updatedMediaItems)
    }

    private fun getArtworkBytes(context: Context, mediaItem: MediaItem): ByteArray? {
        val artworkUri = mediaItem.mediaMetadata.artworkUri
        val requestUri = mediaItem.requestMetadata.mediaUri ?: mediaItem.localConfiguration?.uri

        // 1. Try artworkUri
        if (artworkUri != null) {
            try {
                context.contentResolver.openInputStream(artworkUri)?.use { stream ->
                    val bytes = stream.readBytes()
                    if (bytes.isNotEmpty()) return bytes
                }
            } catch (_: Exception) {}
        }

        // 2. Try MediaMetadataRetriever on requestUri
        if (requestUri != null) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, requestUri)
                val picture = retriever.embeddedPicture
                if (picture != null && picture.isNotEmpty()) {
                    return picture
                }
            } catch (_: Exception) {
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
        }

        return null
    }

    override fun onGetLibraryRoot(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val rootItem = MediaItem.Builder()
            .setMediaId("harmonia_root")
            .build()
        return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
    }

    override fun onGetChildren(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.of(), params))
    }
}
