package com.example.musicplayer.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.ui.components.CreatePlaylistDialog
import com.example.musicplayer.ui.components.CurvedHeader
import com.example.musicplayer.ui.components.EmptyState
import com.example.musicplayer.ui.components.PlaylistCard
import com.example.musicplayer.ui.theme.AccentGold
import com.example.musicplayer.ui.theme.BackgroundDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    viewModel: PlaylistsViewModel,
    onPlaylistClick: (Long) -> Unit,
    onFavoritesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var selectedPlaylistForOptions by remember { mutableStateOf<Playlist?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CurvedHeader(
            title = "Playlists",
            subtitle = "${uiState.playlists.size + 1} playlists available",
            headerHeight = 140.dp,
            actions = {
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Create Playlist",
                        tint = Color.White
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Default System Playlist: Favourites
            item {
                val favoritesPlaylist = Playlist(
                    id = -1L,
                    name = "Favorites",
                    songCount = uiState.favoriteCount,
                    isSystem = true
                )
                PlaylistCard(
                    playlist = favoritesPlaylist,
                    onClick = {
                        viewModel.selectFavorites()
                        onFavoritesClick()
                    }
                )
            }

            if (uiState.playlists.isEmpty()) {
                item {
                    EmptyState(
                        title = "No Custom Playlists",
                        message = "Create playlists to organize your favorite music.",
                        icon = Icons.Rounded.QueueMusic,
                        actionButtonText = "Create Playlist",
                        onActionClick = { showCreateDialog = true }
                    )
                }
            } else {
                items(uiState.playlists, key = { it.id }) { playlist ->
                    PlaylistCard(
                        playlist = playlist,
                        onClick = {
                            viewModel.selectPlaylist(playlist.id)
                            onPlaylistClick(playlist.id)
                        },
                        onOptionsClick = { selectedPlaylistForOptions = playlist }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            title = "Create Playlist",
            confirmButtonText = "Create",
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    playlistToRename?.let { playlist ->
        CreatePlaylistDialog(
            initialName = playlist.name,
            title = "Rename Playlist",
            confirmButtonText = "Save",
            onConfirm = { newName ->
                viewModel.renamePlaylist(playlist.id, newName)
                playlistToRename = null
            },
            onDismiss = { playlistToRename = null }
        )
    }

    playlistToDelete?.let { playlist ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = { Text("Delete Playlist", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${playlist.name}'? The audio files will not be deleted.", color = Color(0xFF8E929E)) },
            containerColor = Color(0xFF22242B),
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlaylist(playlist.id)
                        playlistToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Cancel", color = Color(0xFF8E929E))
                }
            }
        )
    }

    selectedPlaylistForOptions?.let { playlist ->
        ModalBottomSheet(
            onDismissRequest = { selectedPlaylistForOptions = null },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = Color(0xFF22242B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = playlist.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            selectedPlaylistForOptions = null
                            playlistToRename = playlist
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Rounded.Edit, contentDescription = null, tint = AccentGold)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Rename", fontSize = 15.sp, color = Color.White)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            selectedPlaylistForOptions = null
                            playlistToDelete = playlist
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Delete", fontSize = 15.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
