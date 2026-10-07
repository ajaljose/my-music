package com.example.musicplayer.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.model.Album
import com.example.musicplayer.domain.model.Artist
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import com.example.musicplayer.playback.manager.MusicPlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val favoritesCount: Int = 0
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    val playbackManager: MusicPlaybackManager
) : ViewModel() {

    val uiState: StateFlow<LibraryUiState> = combine(
        musicRepository.getAllSongsFlow(),
        musicRepository.getAllAlbumsFlow(),
        musicRepository.getAllArtistsFlow(),
        musicRepository.getAllPlaylistsFlow()
    ) { songs, albums, artists, playlists ->
        LibraryUiState(
            songs = songs,
            albums = albums,
            artists = artists,
            playlists = playlists,
            favoritesCount = songs.count { it.isFavorite }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState()
    )

    fun playAll(shuffle: Boolean = false) {
        val songs = uiState.value.songs
        if (songs.isNotEmpty()) {
            val list = if (shuffle) songs.shuffled() else songs
            playbackManager.playSongs(list, 0)
        }
    }
}
