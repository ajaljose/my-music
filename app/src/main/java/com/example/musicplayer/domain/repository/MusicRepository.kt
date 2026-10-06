package com.example.musicplayer.domain.repository

import com.example.musicplayer.domain.model.*
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun getAllSongsFlow(): Flow<List<Song>>
    suspend fun getAllSongs(): List<Song>
    suspend fun getSongById(id: Long): Song?
    fun getFavoriteSongsFlow(): Flow<List<Song>>
    fun getRecentlyPlayedSongsFlow(limit: Int = 20): Flow<List<Song>>
    fun getRecentlyAddedSongsFlow(limit: Int = 20): Flow<List<Song>>
    fun getMostPlayedSongsFlow(limit: Int = 20): Flow<List<Song>>
    fun getSongsByAlbumFlow(albumId: Long): Flow<List<Song>>
    fun getSongsByArtistFlow(artistId: Long, artistName: String): Flow<List<Song>>
    fun searchSongsFlow(query: String): Flow<List<Song>>
    fun getAllAlbumsFlow(): Flow<List<Album>>
    fun getAllArtistsFlow(): Flow<List<Artist>>
    fun getFolderStructureFlow(): Flow<List<Folder>>

    // Playlists
    fun getAllPlaylistsFlow(): Flow<List<Playlist>>
    fun getSongsForPlaylistFlow(playlistId: Long): Flow<List<Song>>
    suspend fun createPlaylist(name: String): Long
    suspend fun renamePlaylist(playlistId: Long, newName: String)
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long)
    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>)
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    // Mutation & Stats
    suspend fun toggleFavorite(songId: Long, isFavorite: Boolean)
    suspend fun recordSongPlayed(songId: Long)
    suspend fun rescanLibrary(): Int
}
