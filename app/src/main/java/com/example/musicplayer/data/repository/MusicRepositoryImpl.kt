package com.example.musicplayer.data.repository

import com.example.musicplayer.data.local.dao.PlaybackHistoryDao
import com.example.musicplayer.data.local.dao.PlaylistDao
import com.example.musicplayer.data.local.dao.SongDao
import com.example.musicplayer.data.local.entities.PlaybackHistoryEntity
import com.example.musicplayer.data.local.entities.PlaylistEntity
import com.example.musicplayer.data.local.entities.SongEntity
import com.example.musicplayer.data.media.MediaStoreScanner
import com.example.musicplayer.data.preferences.PlayerPreferences
import com.example.musicplayer.domain.model.*
import com.example.musicplayer.domain.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val playbackHistoryDao: PlaybackHistoryDao,
    private val mediaStoreScanner: MediaStoreScanner,
    private val playerPreferences: PlayerPreferences
) : MusicRepository {

    private fun isPathExcluded(path: String, excludedSet: Set<String>): Boolean {
        if (excludedSet.isEmpty() || path.isBlank()) return false
        val parent = File(path).parent ?: ""
        return excludedSet.contains(parent) || excludedSet.any { parent.startsWith(it) }
    }

    override fun getAllSongsFlow(): Flow<List<Song>> =
        combine(songDao.getAllSongsFlow(), playerPreferences.excludedFoldersFlow) { entities, excluded ->
            entities.map { it.toDomain() }.filterNot { isPathExcluded(it.path, excluded) }
        }

    override suspend fun getAllSongs(): List<Song> =
        songDao.getAllSongs().map { it.toDomain() }

    override suspend fun getSongById(id: Long): Song? =
        songDao.getSongById(id)?.toDomain()

    override fun getFavoriteSongsFlow(): Flow<List<Song>> =
        combine(songDao.getFavoriteSongsFlow(), playerPreferences.excludedFoldersFlow) { entities, excluded ->
            entities.map { it.toDomain() }.filterNot { isPathExcluded(it.path, excluded) }
        }

    override fun getRecentlyPlayedSongsFlow(limit: Int): Flow<List<Song>> =
        combine(songDao.getRecentlyPlayedSongsFlow(limit), playerPreferences.excludedFoldersFlow) { entities, excluded ->
            entities.map { it.toDomain() }.filterNot { isPathExcluded(it.path, excluded) }
        }

    override fun getRecentlyAddedSongsFlow(limit: Int): Flow<List<Song>> =
        combine(songDao.getRecentlyAddedSongsFlow(limit), playerPreferences.excludedFoldersFlow) { entities, excluded ->
            entities.map { it.toDomain() }.filterNot { isPathExcluded(it.path, excluded) }
        }

    override fun getMostPlayedSongsFlow(limit: Int): Flow<List<Song>> =
        combine(songDao.getMostPlayedSongsFlow(limit), playerPreferences.excludedFoldersFlow) { entities, excluded ->
            entities.map { it.toDomain() }.filterNot { isPathExcluded(it.path, excluded) }
        }

    override fun getSongsByAlbumFlow(albumId: Long): Flow<List<Song>> =
        combine(songDao.getSongsByAlbumFlow(albumId), playerPreferences.excludedFoldersFlow) { entities, excluded ->
            entities.map { it.toDomain() }.filterNot { isPathExcluded(it.path, excluded) }
        }

    override fun getSongsByArtistFlow(artistId: Long, artistName: String): Flow<List<Song>> =
        combine(songDao.getSongsByArtistFlow(artistId, artistName), playerPreferences.excludedFoldersFlow) { entities, excluded ->
            entities.map { it.toDomain() }.filterNot { isPathExcluded(it.path, excluded) }
        }

    override fun searchSongsFlow(query: String): Flow<List<Song>> =
        combine(songDao.searchSongsFlow(query), playerPreferences.excludedFoldersFlow) { entities, excluded ->
            entities.map { it.toDomain() }.filterNot { isPathExcluded(it.path, excluded) }
        }

    override fun getAllAlbumsFlow(): Flow<List<Album>> =
        getAllSongsFlow().map { songs ->
            songs.groupBy { it.albumId.takeIf { id -> id != 0L } ?: it.album.hashCode().toLong() }
                .map { (_, songGroup) ->
                    val first = songGroup.first()
                    Album(
                        id = first.albumId.takeIf { it != 0L } ?: first.album.hashCode().toLong(),
                        name = first.album,
                        artist = first.artist,
                        artistId = first.artistId,
                        songCount = songGroup.size,
                        year = songGroup.map { it.year }.filter { it > 0 }.maxOrNull() ?: 0,
                        albumArtUriString = songGroup.firstOrNull { !it.albumArtUriString.isNullOrBlank() }?.albumArtUriString,
                        firstSongContentUriString = first.contentUriString,
                        firstSongPath = first.path
                    )
                }
                .sortedBy { it.name.lowercase() }
        }

    override fun getAllArtistsFlow(): Flow<List<Artist>> =
        getAllSongsFlow().map { songs ->
            songs.groupBy { it.artist.lowercase() }
                .map { (_, songGroup) ->
                    val first = songGroup.first()
                    val uniqueAlbums = songGroup.map { it.album }.distinct().size
                    Artist(
                        id = first.artistId.takeIf { it != 0L } ?: first.artist.hashCode().toLong(),
                        name = first.artist,
                        songCount = songGroup.size,
                        albumCount = uniqueAlbums
                    )
                }
                .sortedBy { it.name.lowercase() }
        }

    override fun getFolderStructureFlow(): Flow<List<Folder>> =
        combine(songDao.getAllSongsFlow(), playerPreferences.excludedFoldersFlow) { entities, excluded ->
            val songs = entities.map { it.toDomain() }
            val folderMap = mutableMapOf<String, MutableList<Song>>()

            songs.forEach { song ->
                if (song.path.isNotBlank()) {
                    val parent = File(song.path).parent ?: "Root"
                    folderMap.getOrPut(parent) { mutableListOf() }.add(song)
                }
            }

            folderMap.map { (folderPath, songList) ->
                val folderName = File(folderPath).name.ifBlank { folderPath }
                val isExcluded = excluded.contains(folderPath) || excluded.any { folderPath.startsWith(it) }
                Folder(
                    path = folderPath,
                    name = folderName,
                    songCount = songList.size,
                    songs = songList.sortedBy { it.title.lowercase() },
                    isExcluded = isExcluded
                )
            }.sortedBy { it.name.lowercase() }
        }

    override fun getAllPlaylistsFlow(): Flow<List<Playlist>> =
        playlistDao.getAllPlaylistsWithCountFlow().map { list ->
            list.map { it.playlist.toDomain(it.songCount) }
        }

    override fun getSongsForPlaylistFlow(playlistId: Long): Flow<List<Song>> =
        playlistDao.getSongsForPlaylistFlow(playlistId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    override suspend fun renamePlaylist(playlistId: Long, newName: String) = withContext(Dispatchers.IO) {
        val existing = playlistDao.getPlaylistById(playlistId)
        if (existing != null) {
            playlistDao.updatePlaylist(existing.copy(name = newName))
        }
    }

    override suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylistById(playlistId)
    }

    override suspend fun addSongToPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        playlistDao.addSongToPlaylist(playlistId, songId)
    }

    override suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) = withContext(Dispatchers.IO) {
        playlistDao.addSongsToPlaylist(playlistId, songIds)
    }

    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    override suspend fun toggleFavorite(songId: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        songDao.updateFavoriteStatus(songId, isFavorite)
    }

    override suspend fun recordSongPlayed(songId: Long) {
        withContext(Dispatchers.IO) {
            songDao.incrementPlayCount(songId)
            playbackHistoryDao.insertHistory(PlaybackHistoryEntity(songId = songId))
        }
    }

    override suspend fun rescanLibrary(): Int = withContext(Dispatchers.IO) {
        val scannedSongs = mediaStoreScanner.scanAudioFiles()
        if (scannedSongs.isNotEmpty()) {
            val existingSongs = songDao.getAllSongs().associateBy { it.mediaStoreId }

            val songsToUpdate = mutableListOf<SongEntity>()
            val songsToInsert = mutableListOf<SongEntity>()

            scannedSongs.forEach { scanned ->
                val existing = existingSongs[scanned.mediaStoreId]
                if (existing != null) {
                    songsToUpdate.add(
                        scanned.copy(
                            id = existing.id,
                            isFavorite = existing.isFavorite,
                            playCount = existing.playCount,
                            lastPlayedTimestamp = existing.lastPlayedTimestamp
                        )
                    )
                } else {
                    songsToInsert.add(scanned)
                }
            }

            if (songsToUpdate.isNotEmpty()) {
                songDao.updateSongs(songsToUpdate)
            }
            if (songsToInsert.isNotEmpty()) {
                songDao.insertSongs(songsToInsert)
            }

            // Remove songs deleted from device
            val activeIds = scannedSongs.map { it.mediaStoreId }
            songDao.deleteOldSongs(activeIds)
        }
        scannedSongs.size
    }

    override fun getExcludedFoldersFlow(): Flow<Set<String>> =
        playerPreferences.excludedFoldersFlow

    override suspend fun toggleExcludedFolder(folderPath: String) =
        playerPreferences.toggleExcludedFolder(folderPath)
}
