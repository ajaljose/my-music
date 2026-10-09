package com.example.musicplayer.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun SearchScreen(
    viewModel: SearchViewModel,
    playbackState: PlaybackState,
    onAlbumClick: (Long) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var songForPlaylist by remember { mutableStateOf<Song?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Curved Dome with Search Bar
        CurvedHeader(
            title = "Search",
            subtitle = "Find songs, artists, albums",
            headerHeight = 160.dp,
            bottomContent = {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    SearchTopBar(
                        query = uiState.query,
                        onQueryChange = { viewModel.onQueryChange(it) },
                        onCloseSearch = { viewModel.onQueryChange("") },
                        placeholderText = "Search library..."
                    )
                }
            }
        )

        // Filter chips row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SearchFilter.entries.toTypedArray()) { filter ->
                val isSelected = uiState.filter == filter
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { viewModel.onFilterChange(filter) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) AccentGold else MaterialTheme.colorScheme.surface,
                    contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        text = filter.label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Search Results or Empty Prompt
        if (!uiState.isSearching) {
            EmptyState(
                title = "Search Your Music",
                message = "Type the name of a song, artist, album, or playlist to search instantly.",
                icon = Icons.Rounded.Search
            )
        } else if (uiState.songs.isEmpty() && uiState.albums.isEmpty() && uiState.artists.isEmpty() && uiState.playlists.isEmpty()) {
            EmptyState(
                title = "No Results Found",
                message = "We couldn't find any matches for '${uiState.query}'",
                icon = Icons.Rounded.SearchOff
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // Songs Section
                if ((uiState.filter == SearchFilter.ALL || uiState.filter == SearchFilter.SONGS) && uiState.songs.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Songs (${uiState.songs.size})")
                    }
                    items(uiState.songs, key = { it.id }) { song ->
                        SongRow(
                            song = song,
                            isPlaying = playbackState.currentSong?.id == song.id,
                            onClick = { viewModel.playSong(song, uiState.songs) },
                            onFavoriteClick = { viewModel.toggleFavorite(song) },
                            onOptionsClick = { selectedSongForOptions = song }
                        )
                    }
                }

                // Albums Section
                if ((uiState.filter == SearchFilter.ALL || uiState.filter == SearchFilter.ALBUMS) && uiState.albums.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        SectionHeader(title = "Albums (${uiState.albums.size})")
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.albums, key = { it.id }) { album ->
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

                // Artists Section
                if ((uiState.filter == SearchFilter.ALL || uiState.filter == SearchFilter.ARTISTS) && uiState.artists.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        SectionHeader(title = "Artists (${uiState.artists.size})")
                    }
                    items(uiState.artists, key = { it.name }) { artist ->
                        ArtistRow(
                            artist = artist,
                            onClick = { onArtistClick(artist.name) }
                        )
                    }
                }

                // Playlists Section
                if ((uiState.filter == SearchFilter.ALL || uiState.filter == SearchFilter.PLAYLISTS) && uiState.playlists.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        SectionHeader(title = "Playlists (${uiState.playlists.size})")
                    }
                    items(uiState.playlists, key = { it.id }) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist.id) }
                        )
                    }
                }
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
