package com.example.musicplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.musicplayer.data.local.entities.PlaybackHistoryEntity
import com.example.musicplayer.data.local.entities.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PlaybackHistoryEntity): Long

    @Query("""
        SELECT s.* FROM songs s 
        INNER JOIN playback_history h ON s.id = h.songId 
        ORDER BY h.playedAt DESC 
        LIMIT :limit
    """)
    fun getRecentHistorySongsFlow(limit: Int = 50): Flow<List<SongEntity>>

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()
}
