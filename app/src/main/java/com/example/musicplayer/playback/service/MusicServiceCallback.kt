package com.example.musicplayer.playback.service

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.musicplayer.R
import com.example.musicplayer.data.media.AlbumArtContentProvider
import com.example.musicplayer.data.preferences.PlayerPreferences
import com.example.musicplayer.domain.model.Album
import com.example.musicplayer.domain.model.Artist
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import com.example.musicplayer.playback.manager.toMediaItem
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicServiceCallback @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    private val preferences: PlayerPreferences
) : MediaLibraryService.MediaLibrarySession.Callback {

    private val callbackScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val searchCache = ConcurrentHashMap<String, List<Song>>()

    companion object {
        const val ROOT_ID = "harmonia_root"
        const val MEDIA_ID_SONGS = "root/songs"
        const val MEDIA_ID_PLAYLISTS = "root/playlists"
        const val MEDIA_ID_ALBUMS = "root/albums"
        const val MEDIA_ID_ARTISTS = "root/artists"

        const val PLAYLIST_FAV = "playlist_fav"
        const val PLAYLIST_RECENT = "playlist_recent"
        const val PLAYLIST_MOST_PLAYED = "playlist_most_played"
        const val PLAYLIST_RECENTLY_ADDED = "playlist_recently_added"

        const val ACTION_TOGGLE_FAVORITE = "com.example.musicplayer.ACTION_TOGGLE_FAVORITE"

        private const val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
        private const val CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
        private const val CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
        private const val CONTENT_STYLE_LIST = 1
        private const val CONTENT_STYLE_GRID = 2
    }

    fun buildFavoriteCommandButton(isFavorite: Boolean): CommandButton {
        return CommandButton.Builder()
            .setDisplayName(if (isFavorite) "Favorited" else "Favorite")
            .setIconResId(if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border)
            .setSessionCommand(SessionCommand(ACTION_TOGGLE_FAVORITE, Bundle.EMPTY))
            .setEnabled(true)
            .build()
    }

    fun updateSessionCustomLayout(session: MediaSession, isFavorite: Boolean) {
        val button = buildFavoriteCommandButton(isFavorite)
        session.setCustomLayout(listOf(button))
    }

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        // Crucial: Call super.onConnect to inherit all library commands (COMMAND_GET_LIBRARY_ROOT,
        // COMMAND_GET_CHILDREN, COMMAND_GET_ITEM, COMMAND_SEARCH, etc.) necessary for Android Auto browsing
        val connectionResult = super.onConnect(session, controller)
        val sessionCommands = connectionResult.availableSessionCommands.buildUpon()
            .add(SessionCommand(ACTION_TOGGLE_FAVORITE, Bundle.EMPTY))
            .build()

        return MediaSession.ConnectionResult.accept(
            sessionCommands,
            connectionResult.availablePlayerCommands
        )
    }

    override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
        super.onPostConnect(session, controller)
        val currentItem = session.player.currentMediaItem
        val songId = currentItem?.mediaId?.toLongOrNull()
        if (songId != null) {
            callbackScope.launch(Dispatchers.IO) {
                val song = musicRepository.getSongById(songId)
                val isFav = song?.isFavorite ?: false
                callbackScope.launch(Dispatchers.Main) {
                    session.setCustomLayout(controller, listOf(buildFavoriteCommandButton(isFav)))
                }
            }
        } else {
            session.setCustomLayout(controller, listOf(buildFavoriteCommandButton(false)))
        }
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        if (customCommand.customAction == ACTION_TOGGLE_FAVORITE) {
            val currentItem = session.player.currentMediaItem
            val songId = currentItem?.mediaId?.toLongOrNull()
            if (songId != null) {
                callbackScope.launch(Dispatchers.IO) {
                    val song = musicRepository.getSongById(songId)
                    if (song != null) {
                        val newFavoriteStatus = !song.isFavorite
                        musicRepository.toggleFavorite(songId, newFavoriteStatus)
                        callbackScope.launch(Dispatchers.Main) {
                            updateSessionCustomLayout(session, newFavoriteStatus)
                        }
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
    }

    private fun getRootItem(params: MediaLibraryService.LibraryParams? = null): MediaItem {
        val rootExtras = Bundle().apply {
            putBoolean("android.media.browse.SEARCH_SUPPORTED", true)
            putBoolean(CONTENT_STYLE_SUPPORTED, true)
            putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_LIST)
            putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST)
        }
        params?.extras?.let { rootExtras.putAll(it) }

        val rootMetadata = MediaMetadata.Builder()
            .setTitle(context.getString(R.string.app_name))
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
            .setExtras(rootExtras)
            .build()

        return MediaItem.Builder()
            .setMediaId(ROOT_ID)
            .setMediaMetadata(rootMetadata)
            .build()
    }

    override fun onGetLibraryRoot(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val rootItem = getRootItem(params)
        val isRecent = params?.isRecent == true
        val libraryParams = MediaLibraryService.LibraryParams.Builder()
            .setRecent(isRecent)
            .setExtras(rootItem.mediaMetadata.extras ?: Bundle.EMPTY)
            .build()

        return Futures.immediateFuture(LibraryResult.ofItem(rootItem, libraryParams))
    }

    override fun onGetChildren(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        callbackScope.launch(Dispatchers.IO) {
            try {
                val isRoot = parentId == ROOT_ID || parentId == "root" || parentId == "/" || parentId.isEmpty()
                val items: List<MediaItem> = when {
                    isRoot -> getRootTabs()
                    parentId == MEDIA_ID_SONGS -> {
                        var songs = musicRepository.getAllSongs()
                        if (songs.isEmpty()) {
                            musicRepository.rescanLibrary()
                            songs = musicRepository.getAllSongs()
                        }
                        songs.map { it.toMediaItem() }
                    }
                    parentId == MEDIA_ID_PLAYLISTS -> getPlaylistsFolder()
                    parentId == PLAYLIST_FAV -> {
                        musicRepository.getFavoriteSongsFlow().first().map { it.toMediaItem() }
                    }
                    parentId == PLAYLIST_RECENT -> {
                        musicRepository.getRecentlyPlayedSongsFlow(50).first().map { it.toMediaItem() }
                    }
                    parentId == PLAYLIST_MOST_PLAYED -> {
                        musicRepository.getMostPlayedSongsFlow(50).first().map { it.toMediaItem() }
                    }
                    parentId == PLAYLIST_RECENTLY_ADDED -> {
                        musicRepository.getRecentlyAddedSongsFlow(50).first().map { it.toMediaItem() }
                    }
                    parentId == MEDIA_ID_ALBUMS -> {
                        var albums = musicRepository.getAllAlbumsFlow().first()
                        if (albums.isEmpty()) {
                            musicRepository.rescanLibrary()
                            albums = musicRepository.getAllAlbumsFlow().first()
                        }
                        albums.map { it.toMediaItem() }
                    }
                    parentId == MEDIA_ID_ARTISTS -> {
                        var artists = musicRepository.getAllArtistsFlow().first()
                        if (artists.isEmpty()) {
                            musicRepository.rescanLibrary()
                            artists = musicRepository.getAllArtistsFlow().first()
                        }
                        artists.map { it.toMediaItem() }
                    }
                    parentId.startsWith("playlist_") -> {
                        val playlistId = parentId.removePrefix("playlist_").toLongOrNull() ?: 0L
                        musicRepository.getSongsForPlaylistFlow(playlistId).first().map { it.toMediaItem() }
                    }
                    parentId.startsWith("album_") -> {
                        val albumId = parentId.removePrefix("album_").toLongOrNull() ?: 0L
                        musicRepository.getSongsByAlbumFlow(albumId).first().map { it.toMediaItem() }
                    }
                    parentId.startsWith("artist_") -> {
                        val parts = parentId.removePrefix("artist_").split("_", limit = 2)
                        val artistId = parts.getOrNull(0)?.toLongOrNull() ?: 0L
                        val artistName = parts.getOrNull(1) ?: ""
                        musicRepository.getSongsByArtistFlow(artistId, artistName).first().map { it.toMediaItem() }
                    }
                    else -> emptyList()
                }

                future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
            } catch (e: Exception) {
                e.printStackTrace()
                future.set(LibraryResult.ofItemList(ImmutableList.of(), params))
            }
        }

        return future
    }

    override fun onGetItem(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val future = SettableFuture.create<LibraryResult<MediaItem>>()

        callbackScope.launch(Dispatchers.IO) {
            try {
                // If mediaId is the root
                if (mediaId == ROOT_ID || mediaId == "root" || mediaId == "/" || mediaId.isEmpty()) {
                    future.set(LibraryResult.ofItem(getRootItem(), null))
                    return@launch
                }

                val songId = mediaId.removePrefix("song_").toLongOrNull()
                if (songId != null) {
                    val song = musicRepository.getSongById(songId)
                    if (song != null) {
                        future.set(LibraryResult.ofItem(song.toMediaItem(), null))
                        return@launch
                    }
                }

                // Check top-level root tabs
                val rootTabs = getRootTabs()
                val matchedTab = rootTabs.find { it.mediaId == mediaId }
                if (matchedTab != null) {
                    future.set(LibraryResult.ofItem(matchedTab, null))
                    return@launch
                }

                // Check system/user playlists
                val playlists = getPlaylistsFolder()
                val matchedPlaylist = playlists.find { it.mediaId == mediaId }
                if (matchedPlaylist != null) {
                    future.set(LibraryResult.ofItem(matchedPlaylist, null))
                    return@launch
                }

                // Check albums
                if (mediaId.startsWith("album_")) {
                    val albumId = mediaId.removePrefix("album_").toLongOrNull() ?: 0L
                    val album = musicRepository.getAllAlbumsFlow().first().find { it.id == albumId }
                    if (album != null) {
                        future.set(LibraryResult.ofItem(album.toMediaItem(), null))
                        return@launch
                    }
                }

                // Check artists
                if (mediaId.startsWith("artist_")) {
                    val parts = mediaId.removePrefix("artist_").split("_", limit = 2)
                    val artistId = parts.getOrNull(0)?.toLongOrNull() ?: 0L
                    val artist = musicRepository.getAllArtistsFlow().first().find { it.id == artistId }
                    if (artist != null) {
                        future.set(LibraryResult.ofItem(artist.toMediaItem(), null))
                        return@launch
                    }
                }

                future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
            } catch (e: Exception) {
                e.printStackTrace()
                future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_UNKNOWN))
            }
        }

        return future
    }

    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        // If items already have playback URIs (e.g. sent from phone app's MusicPlaybackManager), preserve directly
        if (mediaItems.isNotEmpty() && mediaItems.all { it.localConfiguration?.uri != null }) {
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs)
            )
        }

        val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()

        callbackScope.launch(Dispatchers.IO) {
            try {
                val resolved = resolveMediaItems(mediaItems, startIndex, startPositionMs)
                future.set(resolved)
            } catch (e: Exception) {
                e.printStackTrace()
                future.setException(e)
            }
        }

        return future
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> {
        if (mediaItems.all { it.localConfiguration?.uri != null }) {
            return Futures.immediateFuture(mediaItems)
        }

        val future = SettableFuture.create<MutableList<MediaItem>>()

        callbackScope.launch(Dispatchers.IO) {
            try {
                val updated = mediaItems.map { item ->
                    if (item.localConfiguration?.uri != null) {
                        item
                    } else {
                        val songId = item.mediaId.removePrefix("song_").toLongOrNull()
                        val song = if (songId != null) musicRepository.getSongById(songId) else null
                        song?.toMediaItem() ?: item.buildUpon()
                            .setUri(item.requestMetadata.mediaUri ?: Uri.parse(item.mediaId))
                            .build()
                    }
                }.toMutableList()
                future.set(updated)
            } catch (e: Exception) {
                e.printStackTrace()
                future.setException(e)
            }
        }

        return future
    }

    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()

        callbackScope.launch(Dispatchers.IO) {
            try {
                val lastPlayedId = preferences.lastPlayedSongIdFlow.first()
                val allSongs = musicRepository.getAllSongs()
                if (allSongs.isNotEmpty()) {
                    val targetIndex = if (lastPlayedId != null && lastPlayedId > 0) {
                        allSongs.indexOfFirst { it.id == lastPlayedId }.coerceAtLeast(0)
                    } else 0
                    future.set(createMediaItemsWithPosition(allSongs, targetIndex, 0L))
                } else {
                    future.set(MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0L))
                }
            } catch (_: Exception) {
                future.set(MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0L))
            }
        }

        return future
    }

    override fun onSearch(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<Void>> {
        val future = SettableFuture.create<LibraryResult<Void>>()

        callbackScope.launch(Dispatchers.IO) {
            try {
                val results = musicRepository.searchSongsFlow(query).first()
                searchCache[query] = results
                session.notifySearchResultChanged(browser, query, results.size, params)
                future.set(LibraryResult.ofVoid())
            } catch (e: Exception) {
                future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_UNKNOWN))
            }
        }

        return future
    }

    override fun onGetSearchResult(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()

        callbackScope.launch(Dispatchers.IO) {
            try {
                val results = searchCache[query] ?: musicRepository.searchSongsFlow(query).first()
                val mapped = results.map { it.toMediaItem() }
                future.set(LibraryResult.ofItemList(ImmutableList.copyOf(mapped), params))
            } catch (e: Exception) {
                future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_UNKNOWN))
            }
        }

        return future
    }

    private suspend fun resolveMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): MediaSession.MediaItemsWithStartPosition {
        if (mediaItems.isEmpty()) {
            return MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0L)
        }

        val firstItem = mediaItems[0]
        val firstId = firstItem.mediaId

        // Case A: Play a playlist container or root / all songs
        if (firstId == MEDIA_ID_SONGS || firstId == ROOT_ID) {
            val songs = musicRepository.getAllSongs()
            return createMediaItemsWithPosition(songs, 0, startPositionMs)
        }
        if (firstId == PLAYLIST_FAV) {
            val songs = musicRepository.getFavoriteSongsFlow().first()
            return createMediaItemsWithPosition(songs, 0, startPositionMs)
        }
        if (firstId == PLAYLIST_RECENT) {
            val songs = musicRepository.getRecentlyPlayedSongsFlow(50).first()
            return createMediaItemsWithPosition(songs, 0, startPositionMs)
        }
        if (firstId == PLAYLIST_MOST_PLAYED) {
            val songs = musicRepository.getMostPlayedSongsFlow(50).first()
            return createMediaItemsWithPosition(songs, 0, startPositionMs)
        }
        if (firstId == PLAYLIST_RECENTLY_ADDED) {
            val songs = musicRepository.getRecentlyAddedSongsFlow(50).first()
            return createMediaItemsWithPosition(songs, 0, startPositionMs)
        }
        if (firstId.startsWith("playlist_")) {
            val playlistId = firstId.removePrefix("playlist_").toLongOrNull() ?: 0L
            val songs = musicRepository.getSongsForPlaylistFlow(playlistId).first()
            return createMediaItemsWithPosition(songs, 0, startPositionMs)
        }

        // Case B: Play an album container
        if (firstId.startsWith("album_")) {
            val albumId = firstId.removePrefix("album_").toLongOrNull() ?: 0L
            val songs = musicRepository.getSongsByAlbumFlow(albumId).first()
            return createMediaItemsWithPosition(songs, 0, startPositionMs)
        }

        // Case C: Play an artist container
        if (firstId.startsWith("artist_")) {
            val parts = firstId.removePrefix("artist_").split("_", limit = 2)
            val artistId = parts.getOrNull(0)?.toLongOrNull() ?: 0L
            val artistName = parts.getOrNull(1) ?: ""
            val songs = musicRepository.getSongsByArtistFlow(artistId, artistName).first()
            return createMediaItemsWithPosition(songs, 0, startPositionMs)
        }

        // Case D: Single song requested by Android Auto / Google Assistant
        val songId = firstId.removePrefix("song_").toLongOrNull()
        if (songId != null && mediaItems.size == 1) {
            val allSongs = musicRepository.getAllSongs()
            val targetIndex = allSongs.indexOfFirst { it.id == songId }
            if (targetIndex >= 0) {
                return createMediaItemsWithPosition(allSongs, targetIndex, startPositionMs)
            }
            val singleSong = musicRepository.getSongById(songId)
            if (singleSong != null) {
                return createMediaItemsWithPosition(listOf(singleSong), 0, startPositionMs)
            }
        }

        // Case E: Multi-item list or fallback
        val resolvedSongs = mediaItems.mapNotNull { item ->
            val id = item.mediaId.removePrefix("song_").toLongOrNull()
            if (id != null) musicRepository.getSongById(id) else null
        }
        val safeIndex = if (startIndex == C.INDEX_UNSET) 0 else startIndex
        return if (resolvedSongs.isNotEmpty()) {
            createMediaItemsWithPosition(resolvedSongs, safeIndex.coerceIn(0, resolvedSongs.size - 1), startPositionMs)
        } else {
            MediaSession.MediaItemsWithStartPosition(mediaItems, safeIndex.coerceAtLeast(0), startPositionMs)
        }
    }

    private fun createMediaItemsWithPosition(
        songs: List<Song>,
        startIndex: Int,
        startPositionMs: Long
    ): MediaSession.MediaItemsWithStartPosition {
        val items = songs.mapIndexed { index, song ->
            val baseItem = song.toMediaItem()
            // Attach artwork bytes directly to the start track for instantaneous display on the car screen
            if (index == startIndex) {
                val artworkBytes = AlbumArtContentProvider.loadArtworkBytes(
                    context,
                    song.albumArtUriString,
                    song.contentUriString,
                    song.path
                )
                if (artworkBytes != null) {
                    val updatedMetadata = baseItem.mediaMetadata.buildUpon()
                        .setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                        .build()
                    baseItem.buildUpon().setMediaMetadata(updatedMetadata).build()
                } else baseItem
            } else baseItem
        }
        val safeIndex = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        return MediaSession.MediaItemsWithStartPosition(items, safeIndex, startPositionMs)
    }

    private fun getRootTabs(): List<MediaItem> {
        val songsTab = MediaItem.Builder()
            .setMediaId(MEDIA_ID_SONGS)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Songs")
                    .setSubtitle("All songs in your library")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .setExtras(Bundle().apply {
                        putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_LIST)
                        putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST)
                    })
                    .build()
            )
            .build()

        val playlistsTab = MediaItem.Builder()
            .setMediaId(MEDIA_ID_PLAYLISTS)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Playlists")
                    .setSubtitle("Favorites & playlists")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .setExtras(Bundle().apply {
                        putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_LIST)
                    })
                    .build()
            )
            .build()

        val albumsTab = MediaItem.Builder()
            .setMediaId(MEDIA_ID_ALBUMS)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Albums")
                    .setSubtitle("Browse by album")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS)
                    .setExtras(Bundle().apply {
                        putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_GRID)
                    })
                    .build()
            )
            .build()

        val artistsTab = MediaItem.Builder()
            .setMediaId(MEDIA_ID_ARTISTS)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Artists")
                    .setSubtitle("Browse by artist")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS)
                    .setExtras(Bundle().apply {
                        putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_LIST)
                    })
                    .build()
            )
            .build()

        return listOf(songsTab, playlistsTab, albumsTab, artistsTab)
    }

    private suspend fun getPlaylistsFolder(): List<MediaItem> {
        val systemPlaylists = listOf(
            MediaItem.Builder()
                .setMediaId(PLAYLIST_FAV)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Favorites")
                        .setSubtitle("Liked songs")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                        .build()
                )
                .build(),
            MediaItem.Builder()
                .setMediaId(PLAYLIST_RECENT)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Recently Played")
                        .setSubtitle("Recently played tracks")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                        .build()
                )
                .build(),
            MediaItem.Builder()
                .setMediaId(PLAYLIST_MOST_PLAYED)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Most Played")
                        .setSubtitle("Frequently played tracks")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                        .build()
                )
                .build(),
            MediaItem.Builder()
                .setMediaId(PLAYLIST_RECENTLY_ADDED)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Recently Added")
                        .setSubtitle("Newly added songs")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                        .build()
                )
                .build()
        )

        val userPlaylists = musicRepository.getAllPlaylistsFlow().first().map { it.toMediaItem() }
        return systemPlaylists + userPlaylists
    }
}

fun Album.toMediaItem(): MediaItem {
    val artUri = AlbumArtContentProvider.getAlbumArtUri(id)
    val extras = Bundle().apply {
        putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 2)
        putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", 2)
    }
    val metadata = MediaMetadata.Builder()
        .setTitle(name)
        .setArtist(artist)
        .setSubtitle("$songCount songs")
        .setArtworkUri(artUri)
        .setIsBrowsable(true)
        .setIsPlayable(false)
        .setMediaType(MediaMetadata.MEDIA_TYPE_ALBUM)
        .setExtras(extras)
        .build()

    return MediaItem.Builder()
        .setMediaId("album_$id")
        .setMediaMetadata(metadata)
        .build()
}

fun Artist.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(name)
        .setSubtitle("$songCount songs")
        .setIsBrowsable(true)
        .setIsPlayable(false)
        .setMediaType(MediaMetadata.MEDIA_TYPE_ARTIST)
        .build()

    return MediaItem.Builder()
        .setMediaId("artist_${id}_$name")
        .setMediaMetadata(metadata)
        .build()
}

fun Playlist.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(name)
        .setSubtitle("$songCount songs")
        .setIsBrowsable(true)
        .setIsPlayable(false)
        .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
        .build()

    return MediaItem.Builder()
        .setMediaId("playlist_$id")
        .setMediaMetadata(metadata)
        .build()
}
