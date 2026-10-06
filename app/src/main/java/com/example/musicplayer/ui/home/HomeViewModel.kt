package com.example.musicplayer.ui.home

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

data class HomeUiState(
    val recentlyPlayed: List<Song> = emptyList(),
    val recentlyAdded: List<Song> = emptyList(),
    val mostPlayed: List<Song> = emptyList(),
    val featuredAlbums: List<Album> = emptyList(),
    val featuredArtists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val totalSongCount: Int = 0,
    val isScanning: Boolean = false,
    val hasPermission: Boolean = false
)

private data class HomeSection1(
    val recentlyPlayed: List<Song>,
    val recentlyAdded: List<Song>,
    val mostPlayed: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>
)

private data class HomeSection2(
    val playlists: List<Playlist>,
    val isScanning: Boolean,
    val hasPermission: Boolean
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playbackManager: MusicPlaybackManager
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _hasPermission = MutableStateFlow(true)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        combine(
            musicRepository.getRecentlyPlayedSongsFlow(10),
            musicRepository.getRecentlyAddedSongsFlow(10),
            musicRepository.getMostPlayedSongsFlow(10),
            musicRepository.getAllAlbumsFlow(),
            musicRepository.getAllArtistsFlow()
        ) { recentlyPlayed, recentlyAdded, mostPlayed, albums, artists ->
            HomeSection1(recentlyPlayed, recentlyAdded, mostPlayed, albums, artists)
        },
        combine(
            musicRepository.getAllPlaylistsFlow(),
            _isScanning,
            _hasPermission
        ) { playlists, scanning, perm ->
            HomeSection2(playlists, scanning, perm)
        }
    ) { s1, s2 ->
        HomeUiState(
            recentlyPlayed = s1.recentlyPlayed,
            recentlyAdded = s1.recentlyAdded,
            mostPlayed = s1.mostPlayed,
            featuredAlbums = s1.albums.take(8),
            featuredArtists = s1.artists.take(8),
            playlists = s2.playlists,
            totalSongCount = s1.recentlyAdded.size,
            isScanning = s2.isScanning,
            hasPermission = s2.hasPermission
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun setPermissionGranted(granted: Boolean) {
        _hasPermission.value = granted
        if (granted) {
            rescanMusic()
        }
    }

    fun rescanMusic() {
        viewModelScope.launch {
            _isScanning.value = true
            musicRepository.rescanLibrary()
            _isScanning.value = false
        }
    }

    fun playSong(song: Song, queue: List<Song>) {
        val index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playbackManager.playSongs(queue, index)
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
