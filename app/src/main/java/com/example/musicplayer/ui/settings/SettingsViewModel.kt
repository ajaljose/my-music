package com.example.musicplayer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.preferences.PlayerPreferences
import com.example.musicplayer.domain.model.SleepTimerState
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import com.example.musicplayer.playback.manager.MusicPlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: String = "SYSTEM",
    val dynamicColor: Boolean = true,
    val pauseOnHeadphoneDisconnect: Boolean = true,
    val totalSongs: Int = 0,
    val isRescanning: Boolean = false,
    val sleepTimerState: SleepTimerState = SleepTimerState()
)

private data class SettingsPrefData(
    val themeMode: String,
    val dynamicColor: Boolean,
    val pauseOnHeadphoneDisconnect: Boolean
)

private data class SettingsStateData(
    val songs: List<Song>,
    val isRescanning: Boolean,
    val sleepTimerState: SleepTimerState
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: PlayerPreferences,
    private val musicRepository: MusicRepository,
    private val playbackManager: MusicPlaybackManager
) : ViewModel() {

    private val _isRescanning = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            preferences.themeModeFlow,
            preferences.dynamicColorFlow,
            preferences.pauseOnHeadphoneDisconnectFlow
        ) { theme, dynamic, pauseNoisy ->
            SettingsPrefData(theme, dynamic, pauseNoisy)
        },
        combine(
            musicRepository.getAllSongsFlow(),
            _isRescanning,
            playbackManager.sleepTimerState
        ) { songs, rescanning, timerState ->
            SettingsStateData(songs, rescanning, timerState)
        }
    ) { prefs, state ->
        SettingsUiState(
            themeMode = prefs.themeMode,
            dynamicColor = prefs.dynamicColor,
            pauseOnHeadphoneDisconnect = prefs.pauseOnHeadphoneDisconnect,
            totalSongs = state.songs.size,
            isRescanning = state.isRescanning,
            sleepTimerState = state.sleepTimerState
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setDynamicColor(enabled)
        }
    }

    fun setPauseOnHeadphoneDisconnect(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setPauseOnHeadphoneDisconnect(enabled)
        }
    }

    fun rescanLibrary() {
        viewModelScope.launch {
            _isRescanning.value = true
            musicRepository.rescanLibrary()
            _isRescanning.value = false
        }
    }

    fun startSleepTimer(minutes: Int) {
        playbackManager.startSleepTimer(minutes)
    }

    fun startSleepTimerEndOfSong() {
        playbackManager.startSleepTimerEndCurrentSong()
    }

    fun cancelSleepTimer() {
        playbackManager.cancelSleepTimer()
    }
}
