package com.example.musicplayer.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import com.example.musicplayer.playback.manager.MusicPlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistsUiState(
    val playlists: List<Playlist> = emptyList(),
    val favoriteCount: Int = 0,
    val selectedPlaylist: Playlist? = null,
    val playlistSongs: List<Song> = emptyList(),
    val isFavoritesSelected: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playbackManager: MusicPlaybackManager
) : ViewModel() {

    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    private val _isFavoritesSelected = MutableStateFlow(false)

    val uiState: StateFlow<PlaylistsUiState> = combine(
        musicRepository.getAllPlaylistsFlow(),
        musicRepository.getFavoriteSongsFlow(),
        _selectedPlaylistId.flatMapLatest { id ->
            if (id != null) musicRepository.getSongsForPlaylistFlow(id)
            else flowOf(emptyList())
        },
        _isFavoritesSelected
    ) { playlists, favorites, playlistSongs, isFavSelected ->
        val selected = playlists.find { it.id == _selectedPlaylistId.value }
        val displayedSongs = if (isFavSelected) favorites else playlistSongs

        PlaylistsUiState(
            playlists = playlists,
            favoriteCount = favorites.size,
            selectedPlaylist = selected,
            playlistSongs = displayedSongs,
            isFavoritesSelected = isFavSelected,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlaylistsUiState())

    fun selectPlaylist(playlistId: Long) {
        _isFavoritesSelected.value = false
        _selectedPlaylistId.value = playlistId
    }

    fun selectFavorites() {
        _isFavoritesSelected.value = true
        _selectedPlaylistId.value = null
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            musicRepository.createPlaylist(name)
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            musicRepository.renamePlaylist(playlistId, newName)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            musicRepository.deletePlaylist(playlistId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            musicRepository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun playSong(song: Song, queue: List<Song>) {
        val index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playbackManager.playSongs(queue, index)
    }

    fun playAll(songs: List<Song>, shuffle: Boolean = false) {
        if (songs.isNotEmpty()) {
            if (shuffle) {
                playbackManager.playSongs(songs.shuffled(), 0)
            } else {
                playbackManager.playSongs(songs, 0)
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
}
