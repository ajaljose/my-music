package com.example.musicplayer.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.model.Album
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import com.example.musicplayer.playback.manager.MusicPlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumsUiState(
    val albums: List<Album> = emptyList(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val selectedAlbum: Album? = null,
    val albumSongs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playbackManager: MusicPlaybackManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isSearching = MutableStateFlow(false)
    private val _selectedAlbumId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<AlbumsUiState> = combine(
        musicRepository.getAllAlbumsFlow(),
        _searchQuery,
        _isSearching,
        _selectedAlbumId.flatMapLatest { id ->
            if (id != null) musicRepository.getSongsByAlbumFlow(id)
            else flowOf(emptyList())
        },
        musicRepository.getAllPlaylistsFlow()
    ) { allAlbums, query, isSearching, songs, playlists ->
        val filtered = if (query.isNotBlank()) {
            allAlbums.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true)
            }
        } else {
            allAlbums
        }

        val selected = allAlbums.find { it.id == _selectedAlbumId.value }

        AlbumsUiState(
            albums = filtered,
            searchQuery = query,
            isSearching = isSearching,
            selectedAlbum = selected,
            albumSongs = songs,
            playlists = playlists,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlbumsUiState())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearching(isSearching: Boolean) {
        _isSearching.value = isSearching
        if (!isSearching) {
            _searchQuery.value = ""
        }
    }

    fun selectAlbum(albumId: Long) {
        _selectedAlbumId.value = albumId
    }

    fun playSong(song: Song, queue: List<Song>) {
        val index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playbackManager.playSongs(queue, index)
    }

    fun playAlbum(albumSongs: List<Song>, shuffle: Boolean = false) {
        if (albumSongs.isNotEmpty()) {
            if (shuffle) {
                playbackManager.playSongs(albumSongs.shuffled(), 0)
            } else {
                playbackManager.playSongs(albumSongs, 0)
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
