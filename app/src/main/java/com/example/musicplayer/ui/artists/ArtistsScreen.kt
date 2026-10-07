package com.example.musicplayer.ui.artists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.musicplayer.ui.components.ArtistRow
import com.example.musicplayer.ui.components.CurvedHeader
import com.example.musicplayer.ui.components.EmptyState
import com.example.musicplayer.ui.components.SearchTopBar
import com.example.musicplayer.ui.theme.BackgroundDark

@Composable
fun ArtistsScreen(
    viewModel: ArtistsViewModel,
    onArtistClick: (String) -> Unit,
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
                placeholderText = "Search artists..."
            )
        } else {
            CurvedHeader(
                title = "Artists",
                subtitle = "${uiState.artists.size} artists in library",
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

        if (uiState.artists.isEmpty()) {
            if (uiState.searchQuery.isNotBlank()) {
                EmptyState(
                    title = "No Artists Found",
                    message = "No artists match '${uiState.searchQuery}'",
                    icon = Icons.Rounded.SearchOff
                )
            } else {
                EmptyState(
                    title = "No Artists",
                    message = "No artists discovered in your audio library.",
                    icon = Icons.Rounded.Person
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(uiState.artists, key = { it.name }) { artist ->
                    ArtistRow(
                        artist = artist,
                        onClick = {
                            viewModel.selectArtist(artist.name)
                            onArtistClick(artist.name)
                        }
                    )
                }
            }
        }
    }
}
