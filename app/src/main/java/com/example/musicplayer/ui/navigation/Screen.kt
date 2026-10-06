package com.example.musicplayer.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "Home", Icons.Rounded.Home)
    object Songs : Screen("songs", "Songs", Icons.Rounded.MusicNote)
    object Albums : Screen("albums", "Albums", Icons.Rounded.Album)
    object Artists : Screen("artists", "Artists", Icons.Rounded.Person)
    object Playlists : Screen("playlists", "Playlists", Icons.Rounded.QueueMusic)
    object Folders : Screen("folders", "Folders", Icons.Rounded.Folder)
    object Settings : Screen("settings", "Settings", Icons.Rounded.Settings)

    object AlbumDetail : Screen("album_detail/{albumId}", "Album Detail") {
        fun createRoute(albumId: Long) = "album_detail/$albumId"
    }

    object ArtistDetail : Screen("artist_detail/{artistName}", "Artist Detail") {
        fun createRoute(artistName: String) = "artist_detail/$artistName"
    }

    object PlaylistDetail : Screen("playlist_detail/{playlistId}?isFavorites={isFavorites}", "Playlist Detail") {
        fun createRoute(playlistId: Long, isFavorites: Boolean = false) = "playlist_detail/$playlistId?isFavorites=$isFavorites"
    }
}
