package com.example.musicplayer.data.local.dao

import androidx.room.*
import com.example.musicplayer.data.local.entities.PlaylistEntity
import com.example.musicplayer.data.local.entities.PlaylistSongCrossRef
import com.example.musicplayer.data.local.entities.SongEntity
import kotlinx.coroutines.flow.Flow

data class PlaylistWithSongCount(
    @Embedded val playlist: PlaylistEntity,
    val songCount: Int
)

@Dao
interface PlaylistDao {

    @Query("""
        SELECT p.*, COUNT(r.songId) as songCount 
        FROM playlists p 
        LEFT JOIN playlist_song_cross_ref r ON p.id = r.playlistId 
        GROUP BY p.id 
        ORDER BY p.name COLLATE NOCASE ASC
    """)
    fun getAllPlaylistsWithCountFlow(): Flow<List<PlaylistWithSongCount>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): PlaylistEntity?

    @Query("""
        SELECT s.* FROM songs s 
        INNER JOIN playlist_song_cross_ref r ON s.id = r.songId 
        WHERE r.playlistId = :playlistId 
        ORDER BY r.orderIndex ASC
    """)
    fun getSongsForPlaylistFlow(playlistId: Long): Flow<List<SongEntity>>

    @Query("""
        SELECT s.* FROM songs s 
        INNER JOIN playlist_song_cross_ref r ON s.id = r.songId 
        WHERE r.playlistId = :playlistId 
        ORDER BY r.orderIndex ASC
    """)
    suspend fun getSongsForPlaylist(playlistId: Long): List<SongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistSongCrossRef(crossRef: PlaylistSongCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistSongCrossRefs(crossRefs: List<PlaylistSongCrossRef>)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    @Query("SELECT MAX(orderIndex) FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    suspend fun getMaxOrderIndex(playlistId: Long): Int?

    @Transaction
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        val maxIndex = getMaxOrderIndex(playlistId) ?: -1
        insertPlaylistSongCrossRef(
            PlaylistSongCrossRef(
                playlistId = playlistId,
                songId = songId,
                orderIndex = maxIndex + 1
            )
        )
    }

    @Transaction
    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        var nextIndex = (getMaxOrderIndex(playlistId) ?: -1) + 1
        val refs = songIds.map { songId ->
            PlaylistSongCrossRef(
                playlistId = playlistId,
                songId = songId,
                orderIndex = nextIndex++
            )
        }
        insertPlaylistSongCrossRefs(refs)
    }
}
