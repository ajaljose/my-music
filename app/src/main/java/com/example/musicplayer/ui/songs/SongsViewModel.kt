package com.example.musicplayer.ui.songs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.preferences.PlayerPreferences
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.model.SongSortOption
import com.example.musicplayer.domain.repository.MusicRepository
import com.example.musicplayer.playback.manager.MusicPlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SongsUiState(
    val songs: List<Song> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val sortOption: SongSortOption = SongSortOption.TITLE_ASC,
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class SongsViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playbackManager: MusicPlaybackManager,
    private val preferences: PlayerPreferences
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isSearching = MutableStateFlow(false)

    val uiState: StateFlow<SongsUiState> = combine(
        musicRepository.getAllSongsFlow(),
        preferences.songSortOptionFlow,
        _searchQuery,
        _isSearching,
        musicRepository.getAllPlaylistsFlow()
    ) { allSongs, sortOption, query, isSearching, playlists ->
        val filtered = if (query.isNotBlank()) {
            allSongs.filter { song ->
                song.title.contains(query, ignoreCase = true) ||
                song.artist.contains(query, ignoreCase = true) ||
                song.album.contains(query, ignoreCase = true)
            }
        } else {
            allSongs
        }

        val sorted = when (sortOption) {
            SongSortOption.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            SongSortOption.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            SongSortOption.ARTIST_ASC -> filtered.sortedBy { it.artist.lowercase() }
            SongSortOption.ALBUM_ASC -> filtered.sortedBy { it.album.lowercase() }
            SongSortOption.DATE_ADDED_DESC -> filtered.sortedByDescending { it.dateAdded }
            SongSortOption.DURATION_DESC -> filtered.sortedByDescending { it.duration }
        }

        SongsUiState(
            songs = sorted,
            searchQuery = query,
            isSearching = isSearching,
            sortOption = sortOption,
            playlists = playlists,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SongsUiState(isLoading = true))

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearching(isSearching: Boolean) {
        _isSearching.value = isSearching
        if (!isSearching) {
            _searchQuery.value = ""
        }
    }

    fun setSortOption(option: SongSortOption) {
        viewModelScope.launch {
            preferences.setSongSortOption(option)
        }
    }

    fun playSong(song: Song) {
        val currentList = uiState.value.songs
        val index = currentList.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playbackManager.playSongs(currentList, index)
    }

    fun playAll(shuffle: Boolean = false) {
        val list = uiState.value.songs
        if (list.isNotEmpty()) {
            if (shuffle) {
                playbackManager.playSongs(list.shuffled(), 0)
            } else {
                playbackManager.playSongs(list, 0)
            }
        }
    }

    fun toggleFavorite(song: Song) {
        playbackManager.toggleFavorite(song.id, !song.isFavorite)
    }

    fun addToQueue(song: Song) {
        playbackManager.addToQueue(song)
    }

    fun playNext(song: Song) {
        playbackManager.playNext(song)
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
