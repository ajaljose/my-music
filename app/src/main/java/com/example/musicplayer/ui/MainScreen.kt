package com.example.musicplayer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.musicplayer.ui.components.MiniPlayer
import com.example.musicplayer.ui.navigation.BottomNavBar
import com.example.musicplayer.ui.navigation.NavGraph
import com.example.musicplayer.ui.navigation.Screen
import com.example.musicplayer.ui.nowplaying.NowPlayingScreen
import com.example.musicplayer.ui.theme.BackgroundDark
import com.example.musicplayer.ui.theme.HarmoniaTheme

@Composable
fun MainScreen(
    onGrantPermissionClick: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val playbackState by viewModel.playbackState.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()

    var isNowPlayingExpanded by remember { mutableStateOf(false) }

    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> systemDark
    }

    HarmoniaTheme(
        darkTheme = isDark,
        dynamicColor = dynamicColor
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    if (playbackState.currentSong != null && !isNowPlayingExpanded) {
                        MiniPlayer(
                            playbackState = playbackState,
                            onClick = { isNowPlayingExpanded = true },
                            onPlayPauseClick = { viewModel.playPause() },
                            onSkipNextClick = { viewModel.skipToNext() },
                            onFavoriteClick = { viewModel.toggleFavoriteCurrentSong() }
                        )
                    }

                    BottomNavBar(
                        navController = navController,
                        onItemClick = {
                            if (isNowPlayingExpanded) {
                                isNowPlayingExpanded = false
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            NavGraph(
                navController = navController,
                playbackState = playbackState,
                onGrantPermissionClick = onGrantPermissionClick,
                modifier = Modifier.padding(paddingValues)
            )

            AnimatedVisibility(
                visible = isNowPlayingExpanded,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                val playlists by viewModel.playlists.collectAsState()
                NowPlayingScreen(
                    playbackManager = viewModel.playbackManager,
                    playlists = playlists,
                    onClose = { isNowPlayingExpanded = false },
                    onToggleFavorite = { viewModel.toggleFavoriteCurrentSong() },
                    onAddSongToPlaylist = { playlistId, songId ->
                        viewModel.addSongToPlaylist(playlistId, songId)
                    },
                    onCreatePlaylist = { name, songId ->
                        viewModel.createPlaylist(name, songId)
                    }
                )
            }
        }
    }
}
