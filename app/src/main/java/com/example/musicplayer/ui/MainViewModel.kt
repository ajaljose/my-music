package com.example.musicplayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.preferences.PlayerPreferences
import com.example.musicplayer.domain.model.PlaybackState
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import com.example.musicplayer.playback.manager.MusicPlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val playbackManager: MusicPlaybackManager,
    private val musicRepository: MusicRepository,
    private val preferences: PlayerPreferences
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playbackManager.playbackState

    val playlists: StateFlow<List<Playlist>> = musicRepository.getAllPlaylistsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val themeMode: StateFlow<String> = preferences.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val dynamicColor: StateFlow<Boolean> = preferences.dynamicColorFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun playPause() {
        playbackManager.playPause()
    }

    fun skipToNext() {
        playbackManager.skipToNext()
    }

    fun skipToPrevious() {
        playbackManager.skipToPrevious()
    }

    fun toggleFavoriteCurrentSong() {
        val currentSong = playbackState.value.currentSong ?: return
        playbackManager.toggleFavorite(currentSong.id)
    }

    fun toggleFavorite(song: Song) {
        playbackManager.toggleFavorite(song.id, !song.isFavorite)
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            musicRepository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun createPlaylist(name: String, songIdToAdd: Long? = null) {
        viewModelScope.launch {
            val playlistId = musicRepository.createPlaylist(name)
            if (songIdToAdd != null) {
                musicRepository.addSongToPlaylist(playlistId, songIdToAdd)
            }
        }
    }
}
