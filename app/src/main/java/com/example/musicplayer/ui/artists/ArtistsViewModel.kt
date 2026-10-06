package com.example.musicplayer.ui.artists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.model.Artist
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import com.example.musicplayer.playback.manager.MusicPlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArtistsUiState(
    val artists: List<Artist> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val selectedArtistName: String? = null,
    val artistSongs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class ArtistsViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playbackManager: MusicPlaybackManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isSearching = MutableStateFlow(false)
    private val _selectedArtistName = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ArtistsUiState> = combine(
        musicRepository.getAllArtistsFlow(),
        _searchQuery,
        _isSearching,
        _selectedArtistName.flatMapLatest { name ->
            if (name != null) musicRepository.getSongsByArtistFlow(0L, name)
            else flowOf(emptyList())
        },
        musicRepository.getAllPlaylistsFlow()
    ) { allArtists, query, isSearching, songs, playlists ->
        val filtered = if (query.isNotBlank()) {
            allArtists.filter { it.name.contains(query, ignoreCase = true) }
        } else {
            allArtists
        }

        ArtistsUiState(
            artists = filtered,
            searchQuery = query,
            isSearching = isSearching,
            selectedArtistName = _selectedArtistName.value,
            artistSongs = songs,
            playlists = playlists,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ArtistsUiState())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearching(isSearching: Boolean) {
        _isSearching.value = isSearching
        if (!isSearching) {
            _searchQuery.value = ""
        }
    }

    fun selectArtist(artistName: String) {
        _selectedArtistName.value = artistName
    }

    fun playSong(song: Song, queue: List<Song>) {
        val index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playbackManager.playSongs(queue, index)
    }

    fun playArtistSongs(songs: List<Song>, shuffle: Boolean = false) {
        if (songs.isNotEmpty()) {
            if (shuffle) {
                playbackManager.playSongs(songs.shuffled(), 0)
            } else {
                playbackManager.playSongs(songs, 0)
            }
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            musicRepository.toggleFavorite(song.id, !song.isFavorite)
        }
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
