package com.example.musicplayer.ui.search

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

enum class SearchFilter(val label: String) {
    ALL("All"),
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists")
}

data class SearchUiState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.ALL,
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isSearching: Boolean = false
)

private data class SearchData(
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val playlists: List<Playlist>
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    val playbackManager: MusicPlaybackManager
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _filter = MutableStateFlow(SearchFilter.ALL)

    val uiState: StateFlow<SearchUiState> = combine(
        _query,
        _filter,
        combine(
            musicRepository.getAllSongsFlow(),
            musicRepository.getAllAlbumsFlow(),
            musicRepository.getAllArtistsFlow(),
            musicRepository.getAllPlaylistsFlow()
        ) { songs, albums, artists, playlists ->
            SearchData(songs, albums, artists, playlists)
        }
    ) { query, filter, data ->
        val trimmed = query.trim().lowercase()
        val isSearching = trimmed.isNotEmpty()

        val filteredSongs = if (isSearching) {
            data.songs.filter {
                it.title.lowercase().contains(trimmed) ||
                        it.artist.lowercase().contains(trimmed) ||
                        it.album.lowercase().contains(trimmed)
            }
        } else emptyList()

        val filteredAlbums = if (isSearching) {
            data.albums.filter {
                it.name.lowercase().contains(trimmed) ||
                        it.artist.lowercase().contains(trimmed)
            }
        } else emptyList()

        val filteredArtists = if (isSearching) {
            data.artists.filter {
                it.name.lowercase().contains(trimmed)
            }
        } else emptyList()

        val filteredPlaylists = if (isSearching) {
            data.playlists.filter {
                it.name.lowercase().contains(trimmed)
            }
        } else emptyList()

        SearchUiState(
            query = query,
            filter = filter,
            songs = filteredSongs,
            albums = filteredAlbums,
            artists = filteredArtists,
            playlists = filteredPlaylists,
            isSearching = isSearching
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState()
    )

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun onFilterChange(newFilter: SearchFilter) {
        _filter.value = newFilter
    }

    fun playSong(song: Song, songList: List<Song>) {
        val index = songList.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playbackManager.playSongs(songList, index)
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            musicRepository.toggleFavorite(song.id, !song.isFavorite)
        }
    }

    fun playNext(song: Song) {
        playbackManager.playNext(song)
    }

    fun addToQueue(song: Song) {
        playbackManager.addToQueue(song)
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            musicRepository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun createPlaylist(name: String, initialSongId: Long? = null) {
        viewModelScope.launch {
            val playlistId = musicRepository.createPlaylist(name)
            initialSongId?.let { songId ->
                musicRepository.addSongToPlaylist(playlistId, songId)
            }
        }
    }
}
