package com.example.musicplayer.ui.home

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.domain.model.Album
import com.example.musicplayer.domain.model.PlaybackState
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.ui.components.*
import com.example.musicplayer.ui.theme.AccentCyan
import com.example.musicplayer.ui.theme.AccentGold
import com.example.musicplayer.ui.theme.AccentPink
import com.example.musicplayer.ui.theme.BackgroundDark

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Curved Dome Hero Header matching signature style
        CurvedHeader(
            title = "My Music",
            subtitle =null,
            headerHeight = 140.dp,
            actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        )

        if (!uiState.hasPermission) {
            EmptyState(
                title = "Access Required",
                message = "My Music needs storage permission to read and play audio files from your device.",
                icon = Icons.Rounded.FolderOpen,
                actionButtonText = "Grant Permission",
                onActionClick = onGrantPermissionClick
            )
        } else if (uiState.isScanning && uiState.recentlyAdded.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentGold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Scanning music library...",
                        fontSize = 15.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else if (uiState.recentlyAdded.isEmpty() && uiState.recentlyPlayed.isEmpty()) {
            EmptyState(
                title = "No Music Found",
                message = "No supported audio files found on your device. Add MP3/audio files to your storage and tap rescan.",
                icon = Icons.Rounded.MusicOff,
                actionButtonText = "Rescan Music",
                onActionClick = { viewModel.rescanMusic() }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // Quick Navigation Category Row
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
                        Spacer(modifier = Modifier.height(8.dp))
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
                        Spacer(modifier = Modifier.height(12.dp))
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

                // Frequently Played Songs
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

    // Dialogs & Bottom Sheets
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
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickNavChip(icon = Icons.Rounded.MusicNote, label = "Songs", tint = AccentCyan, onClick = onNavigateToSongs, modifier = Modifier.weight(1f))
        QuickNavChip(icon = Icons.Rounded.Album, label = "Albums", tint = AccentGold, onClick = onNavigateToAlbums, modifier = Modifier.weight(1f))
        QuickNavChip(icon = Icons.Rounded.Person, label = "Artists", tint = AccentCyan, onClick = onNavigateToArtists, modifier = Modifier.weight(1f))
        QuickNavChip(icon = Icons.Rounded.QueueMusic, label = "Playlists", tint = AccentGold, onClick = onNavigateToPlaylists, modifier = Modifier.weight(1f))
        QuickNavChip(icon = Icons.Rounded.Folder, label = "Folders", tint = AccentCyan, onClick = onNavigateToFolders, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun QuickNavChip(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
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
            .border(1.dp, if (isPlaying) AccentGold else MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box {
                ArtworkImage(
                    artworkUri = song.albumArtUri,
                    songUri = song.contentUri,
                    path = song.path,
                    size = 114.dp,
                    shape = RoundedCornerShape(10.dp),
                    contentDescription = song.title,
                    seed = song.album
                )
                if (isPlaying) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(24.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AnimatedEqualizer(
                                isPlaying = isPlaying,
                                barColor = AccentGold,
                                barWidth = 2.dp,
                                barSpacing = 1.5.dp,
                                maxBarHeight = 12.dp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = song.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPlaying) AccentGold else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
