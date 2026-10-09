package com.example.musicplayer.ui.artists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.domain.model.PlaybackState
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.ui.components.*
import com.example.musicplayer.ui.theme.AccentCyan
import com.example.musicplayer.ui.theme.AccentGold
import com.example.musicplayer.ui.theme.BackgroundDark

@Composable
fun ArtistDetailScreen(
    artistName: String,
    viewModel: ArtistsViewModel,
    playbackState: PlaybackState,
    onBackClick: () -> Unit
) {
    LaunchedEffect(artistName) {
        viewModel.selectArtist(artistName)
    }

    val uiState by viewModel.uiState.collectAsState()
    val songs = uiState.artistSongs

    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var songForPlaylist by remember { mutableStateOf<Song?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Curved Dome Header with Overlapping Artist Icon
        CurvedHeader(
            title = "",
            navigationIcon = Icons.Rounded.ArrowBack,
            onNavigationClick = onBackClick,
            headerHeight = 200.dp,
            bottomContent = {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .size(130.dp)
                            .shadow(20.dp, CircleShape)
                            .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(68.dp)
                            )
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(36.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = artistName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${songs.size} tracks",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.playArtistSongs(songs, shuffle = false) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGold,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.playArtistSongs(songs, shuffle = true) },
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

                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                SongRow(
                    song = song,
                    isPlaying = playbackState.currentSong?.id == song.id,
                    onClick = { viewModel.playSong(song, songs) },
                    onFavoriteClick = { viewModel.toggleFavorite(song) },
                    onOptionsClick = { selectedSongForOptions = song }
                )
            }
        }
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
