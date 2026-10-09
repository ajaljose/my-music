package com.example.musicplayer.ui.songs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.domain.model.PlaybackState
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.ui.components.*
import com.example.musicplayer.ui.theme.AccentGold
import com.example.musicplayer.ui.theme.BackgroundDark

@Composable
fun SongsScreen(
    viewModel: SongsViewModel,
    playbackState: PlaybackState,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var showSortSheet by remember { mutableStateOf(false) }
    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var songForPlaylist by remember { mutableStateOf<Song?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.isSearching) {
            SearchTopBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                onCloseSearch = { viewModel.toggleSearching(false) },
                placeholderText = "Search in songs..."
            )
        } else {
            CurvedHeader(
                title = "All Songs",
                subtitle = "${uiState.songs.size} tracks available",
                headerHeight = 140.dp,
                actions = {
                    IconButton(onClick = { viewModel.toggleSearching(true) }) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { showSortSheet = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Sort,
                            contentDescription = "Sort",
                            tint = Color.White
                        )
                    }
                }
            )
        }

        if (uiState.songs.isEmpty()) {
            if (uiState.searchQuery.isNotBlank()) {
                EmptyState(
                    title = "No results found",
                    message = "No songs match '${uiState.searchQuery}'. Try another query.",
                    icon = Icons.Rounded.SearchOff
                )
            } else {
                EmptyState(
                    title = "No Songs Found",
                    message = "Your audio library is empty.",
                    icon = Icons.Rounded.MusicOff
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // Header with Play All & Shuffle Buttons
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.playAll(shuffle = false) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGold,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play All", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.playAll(shuffle = true) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(imageVector = Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                }

                items(uiState.songs, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        isPlaying = playbackState.currentSong?.id == song.id,
                        onClick = { viewModel.playSong(song) },
                        onFavoriteClick = { viewModel.toggleFavorite(song) },
                        onOptionsClick = { selectedSongForOptions = song }
                    )
                }
            }
        }
    }

    if (showSortSheet) {
        SortBottomSheet(
            currentSort = uiState.sortOption,
            onSortSelected = { viewModel.setSortOption(it) },
            onDismiss = { showSortSheet = false }
        )
    }

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
