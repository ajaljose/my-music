package com.example.musicplayer.ui.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.musicplayer.ui.components.AlbumCard
import com.example.musicplayer.ui.components.CurvedHeader
import com.example.musicplayer.ui.components.EmptyState
import com.example.musicplayer.ui.components.SearchTopBar
import com.example.musicplayer.ui.theme.BackgroundDark

@Composable
fun AlbumsScreen(
    viewModel: AlbumsViewModel,
    onAlbumClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        if (uiState.isSearching) {
            SearchTopBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                onCloseSearch = { viewModel.toggleSearching(false) },
                placeholderText = "Search albums..."
            )
        } else {
            CurvedHeader(
                title = "Albums",
                subtitle = "${uiState.albums.size} albums in library",
                headerHeight = 140.dp,
                actions = {
                    IconButton(onClick = { viewModel.toggleSearching(true) }) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = Color.White
                        )
                    }
                }
            )
        }

        if (uiState.albums.isEmpty()) {
            if (uiState.searchQuery.isNotBlank()) {
                EmptyState(
                    title = "No Albums Found",
                    message = "No albums match '${uiState.searchQuery}'",
                    icon = Icons.Rounded.SearchOff
                )
            } else {
                EmptyState(
                    title = "No Albums",
                    message = "No albums discovered in your library.",
                    icon = Icons.Rounded.Album
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.albums, key = { it.id }) { album ->
                    AlbumCard(
                        album = album,
                        onClick = {
                            viewModel.selectAlbum(album.id)
                            onAlbumClick(album.id)
                        }
                    )
                }
            }
        }
    }
}
