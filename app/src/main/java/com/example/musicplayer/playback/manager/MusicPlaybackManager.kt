package com.example.musicplayer.playback.manager

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musicplayer.data.media.AlbumArtContentProvider
import com.example.musicplayer.data.preferences.PlayerPreferences
import com.example.musicplayer.domain.model.PlaybackState
import com.example.musicplayer.domain.model.RepeatMode
import com.example.musicplayer.domain.model.SleepTimerState
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import com.example.musicplayer.playback.service.MusicService
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicPlaybackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    private val preferences: PlayerPreferences
) {
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _sleepTimerState = MutableStateFlow(SleepTimerState())
    val sleepTimerState: StateFlow<SleepTimerState> = _sleepTimerState.asStateFlow()

    private var sleepCountDownTimer: CountDownTimer? = null
    private var progressJob: Job? = null

    private var currentPlaylistSongs: List<Song> = emptyList()

    init {
        initializeController()
        observePreferences()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupPlayerListener()
                syncStateFromPlayer()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun setupPlayerListener() {
        val controller = mediaController ?: return
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackState()
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(playbackStateInt: Int) {
                updatePlaybackState()
                when (playbackStateInt) {
                    Player.STATE_READY -> {
                        _playbackState.update { it.copy(isLoading = false) }
                    }
                    Player.STATE_BUFFERING -> {
                        _playbackState.update { it.copy(isLoading = true) }
                    }
                    Player.STATE_ENDED -> {
                        if (_sleepTimerState.value.finishEndOfSong) {
                            pause()
                            cancelSleepTimer()
                        }
                    }
                    Player.STATE_IDLE -> {}
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updatePlaybackState()
                val currentSong = _playbackState.value.currentSong
                if (currentSong != null) {
                    serviceScope.launch {
                        val fullSong = musicRepository.getSongById(currentSong.id)
                        if (fullSong != null) {
                            _playbackState.update { state ->
                                if (state.currentSong?.id == fullSong.id) {
                                    state.copy(currentSong = fullSong)
                                } else state
                            }
                        }
                        musicRepository.recordSongPlayed(currentSong.id)
                        preferences.setLastPlayedSong(currentSong.id, 0L)
                    }
                    fetchNotificationArtworkForCurrentSong(currentSong)
                }
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && _sleepTimerState.value.finishEndOfSong) {
                    pause()
                    cancelSleepTimer()
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _playbackState.update { it.copy(isShuffleEnabled = shuffleModeEnabled) }
                serviceScope.launch { preferences.setShuffleEnabled(shuffleModeEnabled) }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                val mode = when (repeatMode) {
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    else -> RepeatMode.OFF
                }
                _playbackState.update { it.copy(repeatMode = mode) }
                serviceScope.launch { preferences.setRepeatMode(mode) }
            }
        })
    }

    private fun observePreferences() {
        serviceScope.launch {
            preferences.shuffleEnabledFlow.collect { enabled ->
                mediaController?.shuffleModeEnabled = enabled
            }
        }
        serviceScope.launch {
            preferences.repeatModeFlow.collect { mode ->
                val playerRepeat = when (mode) {
                    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                }
                mediaController?.repeatMode = playerRepeat
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (isActive) {
                mediaController?.let { controller ->
                    if (controller.isPlaying) {
                        _playbackState.update {
                            it.copy(
                                currentPosition = controller.currentPosition.coerceAtLeast(0L),
                                totalDuration = controller.duration.coerceAtLeast(0L),
                                bufferedPosition = controller.bufferedPosition.coerceAtLeast(0L)
                            )
                        }
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun updatePlaybackState() {
        val controller = mediaController ?: return
        val currentMediaItem = controller.currentMediaItem
        val currentSong = currentMediaItem?.let { item ->
            val songId = item.mediaId.toLongOrNull() ?: 0L
            currentPlaylistSongs.find { it.id == songId } ?: songFromMediaItem(item)
        }

        val currentIndex = controller.currentMediaItemIndex
        val repeatMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            else -> RepeatMode.OFF
        }

        val effectiveQueue = if (currentPlaylistSongs.isNotEmpty()) {
            currentPlaylistSongs
        } else {
            (0 until controller.mediaItemCount).map { index ->
                songFromMediaItem(controller.getMediaItemAt(index))
            }
        }

        _playbackState.update {
            it.copy(
                currentSong = currentSong,
                isPlaying = controller.isPlaying,
                currentPosition = controller.currentPosition.coerceAtLeast(0L),
                totalDuration = controller.duration.coerceAtLeast(0L),
                bufferedPosition = controller.bufferedPosition.coerceAtLeast(0L),
                isShuffleEnabled = controller.shuffleModeEnabled,
                repeatMode = repeatMode,
                queue = effectiveQueue,
                currentQueueIndex = currentIndex
            )
        }
    }

    private fun syncStateFromPlayer() {
        updatePlaybackState()
        if (mediaController?.isPlaying == true) {
            startProgressTracker()
        }
    }

    private var artworkFetchJob: Job? = null

    private fun fetchNotificationArtworkForCurrentSong(song: Song) {
        artworkFetchJob?.cancel()
        artworkFetchJob = serviceScope.launch {
            val controller = mediaController ?: return@launch
            val currentMediaItem = controller.currentMediaItem ?: return@launch

            if (currentMediaItem.mediaMetadata.artworkData != null) return@launch

            val bytes = loadArtworkBytes(song)
            if (bytes != null && isActive) {
                val currentController = mediaController ?: return@launch
                val currentItem = currentController.currentMediaItem ?: return@launch

                if (currentItem.mediaId == song.id.toString()) {
                    val updatedMetadata = currentItem.mediaMetadata.buildUpon()
                        .setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                        .build()

                    val updatedItem = currentItem.buildUpon()
                        .setMediaMetadata(updatedMetadata)
                        .build()

                    val currentIndex = currentController.currentMediaItemIndex
                    if (currentIndex >= 0 && currentIndex < currentController.mediaItemCount) {
                        currentController.replaceMediaItem(currentIndex, updatedItem)
                    }
                }
            }
        }
    }

    private suspend fun loadArtworkBytes(song: Song): ByteArray? = withContext(Dispatchers.IO) {
        AlbumArtContentProvider.loadArtworkBytes(
            context,
            song.albumArtUriString,
            song.contentUriString,
            song.path
        )
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val controller = mediaController ?: return

        currentPlaylistSongs = songs
        val mediaItems = songs.map { it.toMediaItem() }

        controller.setMediaItems(mediaItems, startIndex, 0L)
        controller.prepare()
        controller.play()
        updatePlaybackState()

        val startSong = songs.getOrNull(startIndex)
        if (startSong != null) {
            fetchNotificationArtworkForCurrentSong(startSong)
        }
    }

    fun playSong(song: Song) {
        playSongs(listOf(song), 0)
    }

    fun playPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_IDLE || controller.mediaItemCount == 0) {
                if (currentPlaylistSongs.isNotEmpty()) {
                    playSongs(currentPlaylistSongs, 0)
                }
            } else {
                controller.play()
            }
        }
    }

    fun play() {
        mediaController?.play()
    }

    fun pause() {
        mediaController?.pause()
    }

    fun skipToNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        } else if (controller.repeatMode == Player.REPEAT_MODE_ALL && controller.mediaItemCount > 0) {
            controller.seekTo(0, 0L)
        }
    }

    fun skipToPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3000) {
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        } else {
            controller.seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _playbackState.update { it.copy(currentPosition = positionMs) }
    }

    fun skipToQueueItem(index: Int) {
        val controller = mediaController ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.seekToDefaultPosition(index)
            controller.play()
        }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val newState = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = newState
        _playbackState.update { it.copy(isShuffleEnabled = newState) }
    }

    fun toggleRepeatMode() {
        val controller = mediaController ?: return
        val current = _playbackState.value.repeatMode
        val next = current.next()
        val playerMode = when (next) {
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = playerMode
        _playbackState.update { it.copy(repeatMode = next) }
    }

    fun toggleFavorite(songId: Long, targetFavoriteStatus: Boolean? = null) {
        val currentSong = _playbackState.value.currentSong
        val isFav = targetFavoriteStatus ?: if (currentSong?.id == songId) !currentSong.isFavorite else true

        updateSongFavoriteStatus(songId, isFav)

        serviceScope.launch {
            musicRepository.toggleFavorite(songId, isFav)
        }
    }

    fun updateSongFavoriteStatus(songId: Long, isFavorite: Boolean) {
        currentPlaylistSongs = currentPlaylistSongs.map { song ->
            if (song.id == songId) song.copy(isFavorite = isFavorite) else song
        }
        _playbackState.update { state ->
            val updatedCurrent = if (state.currentSong?.id == songId) {
                state.currentSong.copy(isFavorite = isFavorite)
            } else {
                state.currentSong
            }
            val updatedQueue = state.queue.map { song ->
                if (song.id == songId) song.copy(isFavorite = isFavorite) else song
            }
            state.copy(
                currentSong = updatedCurrent,
                queue = updatedQueue
            )
        }
    }

    fun addToQueue(song: Song) {
        val controller = mediaController ?: return
        currentPlaylistSongs = currentPlaylistSongs + song
        controller.addMediaItem(song.toMediaItem())
        updatePlaybackState()
    }

    fun playNext(song: Song) {
        val controller = mediaController ?: return
        val nextIndex = (controller.currentMediaItemIndex + 1).coerceAtMost(controller.mediaItemCount)
        val mutable = currentPlaylistSongs.toMutableList()
        if (nextIndex <= mutable.size) {
            mutable.add(nextIndex, song)
            currentPlaylistSongs = mutable
        }
        controller.addMediaItem(nextIndex, song.toMediaItem())
        updatePlaybackState()
    }

    fun removeFromQueue(index: Int) {
        val controller = mediaController ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.removeMediaItem(index)
            val mutable = currentPlaylistSongs.toMutableList()
            if (index < mutable.size) {
                mutable.removeAt(index)
                currentPlaylistSongs = mutable
            }
            updatePlaybackState()
        }
    }

    fun clearQueue() {
        val controller = mediaController ?: return
        controller.clearMediaItems()
        currentPlaylistSongs = emptyList()
        updatePlaybackState()
    }

    // Sleep Timer
    fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        val totalMillis = minutes * 60 * 1000L
        _sleepTimerState.value = SleepTimerState(
            isActive = true,
            remainingMillis = totalMillis,
            finishEndOfSong = false
        )

        sleepCountDownTimer = object : CountDownTimer(totalMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                _sleepTimerState.update {
                    it.copy(remainingMillis = millisUntilFinished)
                }
            }

            override fun onFinish() {
                pause()
                cancelSleepTimer()
            }
        }.start()
    }

    fun startSleepTimerEndCurrentSong() {
        cancelSleepTimer()
        val controller = mediaController
        val remaining = if (controller != null && controller.duration > 0) {
            (controller.duration - controller.currentPosition).coerceAtLeast(0L)
        } else 0L

        _sleepTimerState.value = SleepTimerState(
            isActive = true,
            remainingMillis = remaining,
            finishEndOfSong = true
        )
    }

    fun cancelSleepTimer() {
        sleepCountDownTimer?.cancel()
        sleepCountDownTimer = null
        _sleepTimerState.value = SleepTimerState(isActive = false, remainingMillis = 0L, finishEndOfSong = false)
    }

    private fun songFromMediaItem(item: MediaItem): Song {
        val metadata = item.mediaMetadata
        return Song(
            id = item.mediaId.toLongOrNull() ?: 0L,
            mediaStoreId = item.mediaId.toLongOrNull() ?: 0L,
            title = metadata.title?.toString() ?: "Unknown Title",
            artist = metadata.artist?.toString() ?: "Unknown Artist",
            album = metadata.albumTitle?.toString() ?: "Unknown Album",
            duration = 0L,
            path = "",
            contentUriString = item.requestMetadata.mediaUri?.toString() ?: "",
            albumArtUriString = metadata.artworkUri?.toString()
        )
    }
}

fun Song.toMediaItem(): MediaItem {
    val artworkUri = AlbumArtContentProvider.getSongArtUri(id)
    val songExtras = Bundle().apply {
        putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", 1)
    }
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setArtworkUri(artworkUri)
        .setIsPlayable(true)
        .setIsBrowsable(false)
        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
        .setExtras(songExtras)
        .build()

    val parsedUri = runCatching { contentUri }.getOrNull()

    val builder = MediaItem.Builder()
        .setMediaId(id.toString())
        .setMediaMetadata(metadata)

    if (parsedUri != null) {
        builder.setUri(parsedUri)
        builder.setRequestMetadata(
            MediaItem.RequestMetadata.Builder()
                .setMediaUri(parsedUri)
                .build()
        )
    }

    return builder.build()
}
