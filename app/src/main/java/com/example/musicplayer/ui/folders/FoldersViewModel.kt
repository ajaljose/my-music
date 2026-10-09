package com.example.musicplayer.ui.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.model.Folder
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import com.example.musicplayer.playback.manager.MusicPlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FoldersUiState(
    val folders: List<Folder> = emptyList(),
    val selectedFolder: Folder? = null,
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playbackManager: MusicPlaybackManager
) : ViewModel() {

    private val _selectedFolderPath = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FoldersUiState> = combine(
        musicRepository.getFolderStructureFlow(),
        _selectedFolderPath,
        musicRepository.getAllPlaylistsFlow()
    ) { folders, selectedPath, playlists ->
        val selected = if (selectedPath != null) folders.find { it.path == selectedPath } else null
        FoldersUiState(
            folders = folders,
            selectedFolder = selected,
            playlists = playlists,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FoldersUiState())

    fun selectFolder(folderPath: String?) {
        _selectedFolderPath.value = folderPath
    }

    fun toggleExcludedFolder(folderPath: String) {
        viewModelScope.launch {
            musicRepository.toggleExcludedFolder(folderPath)
        }
    }

    fun playSong(song: Song, queue: List<Song>) {
        val index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playbackManager.playSongs(queue, index)
    }

    fun playFolder(folder: Folder, shuffle: Boolean = false) {
        if (folder.songs.isNotEmpty()) {
            if (shuffle) {
                playbackManager.playSongs(folder.songs.shuffled(), 0)
            } else {
                playbackManager.playSongs(folder.songs, 0)
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
