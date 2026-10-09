package com.example.musicplayer.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.musicplayer.domain.model.RepeatMode
import com.example.musicplayer.domain.model.SongSortOption
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "harmonia_preferences")

@Singleton
class PlayerPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode") // "SYSTEM", "LIGHT", "DARK"
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val REPEAT_MODE = stringPreferencesKey("repeat_mode") // "OFF", "ALL", "ONE"
        val SONG_SORT_OPTION = stringPreferencesKey("song_sort_option")
        val LAST_PLAYED_SONG_ID = longPreferencesKey("last_played_song_id")
        val LAST_PLAYED_POSITION = longPreferencesKey("last_played_position")
        val PAUSE_ON_HEADPHONE_DISCONNECT = booleanPreferencesKey("pause_on_headphone_disconnect")
        val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val EXCLUDED_FOLDERS = stringSetPreferencesKey("excluded_folders")
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.THEME_MODE] ?: "SYSTEM"
    }

    val dynamicColorFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.DYNAMIC_COLOR] ?: true
    }

    val excludedFoldersFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[Keys.EXCLUDED_FOLDERS] ?: emptySet()
    }

    val shuffleEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.SHUFFLE_ENABLED] ?: false
    }

    val repeatModeFlow: Flow<RepeatMode> = context.dataStore.data.map { preferences ->
        when (preferences[Keys.REPEAT_MODE]) {
            "ALL" -> RepeatMode.ALL
            "ONE" -> RepeatMode.ONE
            else -> RepeatMode.OFF
        }
    }

    val songSortOptionFlow: Flow<SongSortOption> = context.dataStore.data.map { preferences ->
        val raw = preferences[Keys.SONG_SORT_OPTION]
        try {
            if (raw != null) SongSortOption.valueOf(raw) else SongSortOption.TITLE_ASC
        } catch (e: Exception) {
            SongSortOption.TITLE_ASC
        }
    }

    val lastPlayedSongIdFlow: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[Keys.LAST_PLAYED_SONG_ID]
    }

    val pauseOnHeadphoneDisconnectFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.PAUSE_ON_HEADPHONE_DISCONNECT] ?: true
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = mode
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setShuffleEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SHUFFLE_ENABLED] = enabled
        }
    }

    suspend fun setRepeatMode(mode: RepeatMode) {
        context.dataStore.edit { preferences ->
            preferences[Keys.REPEAT_MODE] = mode.name
        }
    }

    suspend fun setSongSortOption(option: SongSortOption) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SONG_SORT_OPTION] = option.name
        }
    }

    suspend fun setLastPlayedSong(songId: Long, position: Long) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LAST_PLAYED_SONG_ID] = songId
            preferences[Keys.LAST_PLAYED_POSITION] = position
        }
    }

    suspend fun setPauseOnHeadphoneDisconnect(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.PAUSE_ON_HEADPHONE_DISCONNECT] = enabled
        }
    }

    suspend fun toggleExcludedFolder(folderPath: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[Keys.EXCLUDED_FOLDERS] ?: emptySet()
            if (current.contains(folderPath)) {
                preferences[Keys.EXCLUDED_FOLDERS] = current - folderPath
            } else {
                preferences[Keys.EXCLUDED_FOLDERS] = current + folderPath
            }
        }
    }
}
