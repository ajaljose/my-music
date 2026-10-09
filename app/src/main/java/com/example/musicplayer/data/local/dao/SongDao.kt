package com.example.musicplayer.data.local.dao

import androidx.room.*
import com.example.musicplayer.data.local.entities.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun getAllSongsFlow(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    suspend fun getAllSongs(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE mediaStoreId = :mediaStoreId")
    suspend fun getSongByMediaStoreId(mediaStoreId: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun getFavoriteSongsFlow(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE albumId = :albumId ORDER BY trackNumber ASC, title COLLATE NOCASE ASC")
    fun getSongsByAlbumFlow(albumId: Long): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE album = :albumName ORDER BY trackNumber ASC, title COLLATE NOCASE ASC")
    suspend fun getSongsByAlbum(albumName: String): List<SongEntity>

    @Query("SELECT * FROM songs WHERE artistId = :artistId OR artist = :artistName ORDER BY title COLLATE NOCASE ASC")
    fun getSongsByArtistFlow(artistId: Long, artistName: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC LIMIT :limit")
    fun getRecentlyPlayedSongsFlow(limit: Int = 20): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY dateAdded DESC LIMIT :limit")
    fun getRecentlyAddedSongsFlow(limit: Int = 20): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE playCount > 0 ORDER BY playCount DESC LIMIT :limit")
    fun getMostPlayedSongsFlow(limit: Int = 20): Flow<List<SongEntity>>

    @Query("""
        SELECT * FROM songs 
        WHERE title LIKE '%' || :query || '%' 
           OR artist LIKE '%' || :query || '%' 
           OR album LIKE '%' || :query || '%' 
        ORDER BY title COLLATE NOCASE ASC
    """)
    fun searchSongsFlow(query: String): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity): Long

    @Update
    suspend fun updateSong(song: SongEntity)

    @Update
    suspend fun updateSongs(songs: List<SongEntity>)

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    suspend fun updateFavoriteStatus(songId: Long, isFavorite: Boolean)

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayedTimestamp = :timestamp WHERE id = :songId")
    suspend fun incrementPlayCount(songId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM songs WHERE mediaStoreId NOT IN (:activeMediaStoreIds)")
    suspend fun deleteOldSongs(activeMediaStoreIds: List<Long>)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSongById(id: Long)

    @Query("SELECT COUNT(*) FROM songs")
    fun getSongCountFlow(): Flow<Int>
}
