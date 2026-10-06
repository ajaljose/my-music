package com.example.musicplayer.domain.model

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val totalDuration: Long = 0L,
    val bufferedPosition: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<Song> = emptyList(),
    val currentQueueIndex: Int = -1,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

enum class RepeatMode {
    OFF,
    ALL,
    ONE;

    fun next(): RepeatMode = when (this) {
        OFF -> ALL
        ALL -> ONE
        ONE -> OFF
    }
}

enum class SongSortOption {
    TITLE_ASC,
    TITLE_DESC,
    ARTIST_ASC,
    ALBUM_ASC,
    DATE_ADDED_DESC,
    DURATION_DESC
}

data class SleepTimerState(
    val isActive: Boolean = false,
    val remainingMillis: Long = 0L,
    val finishEndOfSong: Boolean = false
)
