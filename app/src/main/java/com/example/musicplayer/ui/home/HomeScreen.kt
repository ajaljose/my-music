package com.example.musicplayer.ui.home

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.musicplayer.domain.model.Album
import com.example.musicplayer.domain.model.PlaybackState
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    playbackState: PlaybackState,
    onNavigateToSongs: () -> Unit,
    onNavigateToAlbums: () -> Unit,
    onNavigateToArtists: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onAlbumClick: (Long) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onGrantPermissionClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var songForPlaylist by remember { mutableStateOf<Song?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "My Music",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (!uiState.hasPermission) {
            EmptyState(
                title = "Access Required",
                message = "My Music needs storage permission to read and play audio files from your device.",
                icon = Icons.Rounded.FolderOpen,
                actionButtonText = "Grant Permission",
                onActionClick = onGrantPermissionClick,
                modifier = Modifier.padding(paddingValues)
            )
        } else if (uiState.isScanning && uiState.recentlyAdded.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Scanning music library...",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        } else if (uiState.recentlyAdded.isEmpty() && uiState.recentlyPlayed.isEmpty()) {
            EmptyState(
                title = "No Music Found",
                message = "No supported audio files found on your device. Add MP3/audio files to your device storage and rescan.",
                icon = Icons.Rounded.MusicOff,
                actionButtonText = "Rescan Music",
                onActionClick = { viewModel.rescanMusic() },
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // Quick Navigation Grid
                item {
                    QuickNavSection(
                        onNavigateToSongs = onNavigateToSongs,
                        onNavigateToAlbums = onNavigateToAlbums,
                        onNavigateToArtists = onNavigateToArtists,
                        onNavigateToPlaylists = onNavigateToPlaylists,
                        onNavigateToFolders = onNavigateToFolders
                    )
                }

                // Recently Played Section
                if (uiState.recentlyPlayed.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Recently Played")
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.recentlyPlayed) { song ->
                                RecentlyPlayedCard(
                                    song = song,
                                    isPlaying = playbackState.currentSong?.id == song.id && playbackState.isPlaying,
                                    onClick = { viewModel.playSong(song, uiState.recentlyPlayed) }
                                )
                            }
                        }
                    }
                }

                // Featured Albums
                if (uiState.featuredAlbums.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader(
                            title = "Albums",
                            actionText = "See all",
                            onActionClick = onNavigateToAlbums
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.featuredAlbums) { album ->
                                Box(modifier = Modifier.width(140.dp)) {
                                    AlbumCard(
                                        album = album,
                                        onClick = { onAlbumClick(album.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Most Played / Frequently Played
                if (uiState.mostPlayed.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        SectionHeader(title = "Frequently Played")
                    }
                    items(uiState.mostPlayed.take(5)) { song ->
                        SongRow(
                            song = song,
                            isPlaying = playbackState.currentSong?.id == song.id,
                            onClick = { viewModel.playSong(song, uiState.mostPlayed) },
                            onFavoriteClick = { viewModel.toggleFavorite(song) },
                            onOptionsClick = { selectedSongForOptions = song }
                        )
                    }
                }

                // Recently Added Songs
                if (uiState.recentlyAdded.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        SectionHeader(
                            title = "Recently Added",
                            actionText = "See all",
                            onActionClick = onNavigateToSongs
                        )
                    }
                    items(uiState.recentlyAdded.take(8)) { song ->
                        SongRow(
                            song = song,
                            isPlaying = playbackState.currentSong?.id == song.id,
                            onClick = { viewModel.playSong(song, uiState.recentlyAdded) },
                            onFavoriteClick = { viewModel.toggleFavorite(song) },
                            onOptionsClick = { selectedSongForOptions = song }
                        )
                    }
                }
            }
        }
    }

    // Dialogs & Sheets
    selectedSongForOptions?.let { song ->
        SongOptionsBottomSheet(
            song = song,
            onPlayNext = { viewModel.playNext(song) },
            onAddToQueue = { viewModel.addToQueue(song) },
            onAddToPlaylist = {
                songForPlaylist = song
                showAddToPlaylistDialog = true
            },
            onToggleFavorite = { viewModel.toggleFavorite(song) },
            onDismiss = { selectedSongForOptions = null }
        )
    }

    if (showAddToPlaylistDialog && songForPlaylist != null) {
        AddToPlaylistDialog(
            playlists = uiState.playlists,
            onPlaylistSelected = { playlist ->
                songForPlaylist?.let { viewModel.addSongToPlaylist(playlist.id, it.id) }
                showAddToPlaylistDialog = false
            },
            onCreateNewPlaylistClick = {
                showAddToPlaylistDialog = false
                showCreatePlaylistDialog = true
            },
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onConfirm = { name ->
                viewModel.createPlaylist(name, songForPlaylist?.id)
                showCreatePlaylistDialog = false
                songForPlaylist = null
            },
            onDismiss = {
                showCreatePlaylistDialog = false
                songForPlaylist = null
            }
        )
    }
}

@Composable
private fun QuickNavSection(
    onNavigateToSongs: () -> Unit,
    onNavigateToAlbums: () -> Unit,
    onNavigateToArtists: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToFolders: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickNavChip(icon = Icons.Rounded.MusicNote, label = "Songs", onClick = onNavigateToSongs, modifier = Modifier.weight(1f))
        QuickNavChip(icon = Icons.Rounded.Album, label = "Albums", onClick = onNavigateToAlbums, modifier = Modifier.weight(1f))
        QuickNavChip(icon = Icons.Rounded.Person, label = "Artists", onClick = onNavigateToArtists, modifier = Modifier.weight(1f))
        QuickNavChip(icon = Icons.Rounded.QueueMusic, label = "Playlists", onClick = onNavigateToPlaylists, modifier = Modifier.weight(1f))
        QuickNavChip(icon = Icons.Rounded.Folder, label = "Folders", onClick = onNavigateToFolders, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun QuickNavChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RecentlyPlayedCard(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(130.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            ArtworkImage(
                artworkUri = song.albumArtUri,
                songUri = song.contentUri,
                path = song.path,
                size = 114.dp,
                shape = RoundedCornerShape(10.dp),
                contentDescription = song.title,
                seed = song.album
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
