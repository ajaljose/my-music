package com.example.musicplayer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.musicplayer.domain.model.PlaybackState
import com.example.musicplayer.ui.albums.AlbumDetailScreen
import com.example.musicplayer.ui.albums.AlbumsScreen
import com.example.musicplayer.ui.albums.AlbumsViewModel
import com.example.musicplayer.ui.artists.ArtistDetailScreen
import com.example.musicplayer.ui.artists.ArtistsScreen
import com.example.musicplayer.ui.artists.ArtistsViewModel
import com.example.musicplayer.ui.folders.FoldersScreen
import com.example.musicplayer.ui.folders.FoldersViewModel
import com.example.musicplayer.ui.home.HomeScreen
import com.example.musicplayer.ui.home.HomeViewModel
import com.example.musicplayer.ui.library.LibraryScreen
import com.example.musicplayer.ui.library.LibraryViewModel
import com.example.musicplayer.ui.playlists.PlaylistDetailScreen
import com.example.musicplayer.ui.playlists.PlaylistsScreen
import com.example.musicplayer.ui.playlists.PlaylistsViewModel
import com.example.musicplayer.ui.search.SearchScreen
import com.example.musicplayer.ui.search.SearchViewModel
import com.example.musicplayer.ui.settings.SettingsScreen
import com.example.musicplayer.ui.settings.SettingsViewModel
import com.example.musicplayer.ui.songs.SongsScreen
import com.example.musicplayer.ui.songs.SongsViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    playbackState: PlaybackState,
    onGrantPermissionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        // Tab 1: Home
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = viewModel,
                playbackState = playbackState,
                onNavigateToSongs = { navController.navigate(Screen.Songs.route) },
                onNavigateToAlbums = { navController.navigate(Screen.Albums.route) },
                onNavigateToArtists = { navController.navigate(Screen.Artists.route) },
                onNavigateToPlaylists = { navController.navigate(Screen.Playlists.route) },
                onNavigateToFolders = { navController.navigate(Screen.Folders.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onAlbumClick = { albumId -> navController.navigate(Screen.AlbumDetail.createRoute(albumId)) },
                onArtistClick = { artistName -> navController.navigate(Screen.ArtistDetail.createRoute(artistName)) },
                onPlaylistClick = { playlistId -> navController.navigate(Screen.PlaylistDetail.createRoute(playlistId)) },
                onGrantPermissionClick = onGrantPermissionClick
            )
        }

        // Tab 2: Search
        composable(Screen.Search.route) {
            val viewModel: SearchViewModel = hiltViewModel()
            SearchScreen(
                viewModel = viewModel,
                playbackState = playbackState,
                onAlbumClick = { albumId -> navController.navigate(Screen.AlbumDetail.createRoute(albumId)) },
                onArtistClick = { artistName -> navController.navigate(Screen.ArtistDetail.createRoute(artistName)) },
                onPlaylistClick = { playlistId -> navController.navigate(Screen.PlaylistDetail.createRoute(playlistId)) }
            )
        }

        // Tab 3: Library
        composable(Screen.Library.route) {
            val viewModel: LibraryViewModel = hiltViewModel()
            LibraryScreen(
                viewModel = viewModel,
                onNavigateToSongs = { navController.navigate(Screen.Songs.route) },
                onNavigateToAlbums = { navController.navigate(Screen.Albums.route) },
                onNavigateToArtists = { navController.navigate(Screen.Artists.route) },
                onNavigateToPlaylists = { navController.navigate(Screen.Playlists.route) },
                onNavigateToFolders = { navController.navigate(Screen.Folders.route) },
                onNavigateToFavorites = { navController.navigate(Screen.Favourites.route) }
            )
        }

        // Tab 4: Favourites
        composable(Screen.Favourites.route) {
            val viewModel: PlaylistsViewModel = hiltViewModel()
            PlaylistDetailScreen(
                playlistId = -1L,
                isFavorites = true,
                viewModel = viewModel,
                playbackState = playbackState,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Sub-routes
        composable(Screen.Songs.route) {
            val viewModel: SongsViewModel = hiltViewModel()
            SongsScreen(
                viewModel = viewModel,
                playbackState = playbackState
            )
        }

        composable(Screen.Albums.route) {
            val viewModel: AlbumsViewModel = hiltViewModel()
            AlbumsScreen(
                viewModel = viewModel,
                onAlbumClick = { albumId ->
                    navController.navigate(Screen.AlbumDetail.createRoute(albumId))
                }
            )
        }

        composable(
            route = Screen.AlbumDetail.route,
            arguments = listOf(navArgument("albumId") { type = NavType.LongType })
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getLong("albumId") ?: 0L
            val viewModel: AlbumsViewModel = hiltViewModel()
            AlbumDetailScreen(
                albumId = albumId,
                viewModel = viewModel,
                playbackState = playbackState,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Artists.route) {
            val viewModel: ArtistsViewModel = hiltViewModel()
            ArtistsScreen(
                viewModel = viewModel,
                onArtistClick = { artistName ->
                    navController.navigate(Screen.ArtistDetail.createRoute(artistName))
                }
            )
        }

        composable(
            route = Screen.ArtistDetail.route,
            arguments = listOf(navArgument("artistName") { type = NavType.StringType })
        ) { backStackEntry ->
            val artistName = backStackEntry.arguments?.getString("artistName") ?: ""
            val viewModel: ArtistsViewModel = hiltViewModel()
            ArtistDetailScreen(
                artistName = artistName,
                viewModel = viewModel,
                playbackState = playbackState,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Playlists.route) {
            val viewModel: PlaylistsViewModel = hiltViewModel()
            PlaylistsScreen(
                viewModel = viewModel,
                onPlaylistClick = { playlistId ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(playlistId, isFavorites = false))
                },
                onFavoritesClick = {
                    navController.navigate(Screen.Favourites.route)
                }
            )
        }

        composable(
            route = Screen.PlaylistDetail.route,
            arguments = listOf(
                navArgument("playlistId") { type = NavType.LongType },
                navArgument("isFavorites") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
            val isFavorites = backStackEntry.arguments?.getBoolean("isFavorites") ?: false
            val viewModel: PlaylistsViewModel = hiltViewModel()
            PlaylistDetailScreen(
                playlistId = playlistId,
                isFavorites = isFavorites,
                viewModel = viewModel,
                playbackState = playbackState,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Folders.route) {
            val viewModel: FoldersViewModel = hiltViewModel()
            FoldersScreen(
                viewModel = viewModel,
                playbackState = playbackState
            )
        }

        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
