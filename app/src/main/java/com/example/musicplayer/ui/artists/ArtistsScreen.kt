package com.example.musicplayer.ui.artists

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.musicplayer.ui.components.ArtistRow
import com.example.musicplayer.ui.components.EmptyState
import com.example.musicplayer.ui.components.SearchTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistsScreen(
    viewModel: ArtistsViewModel,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            if (uiState.isSearching) {
                SearchTopBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                    onCloseSearch = { viewModel.toggleSearching(false) },
                    placeholderText = "Search artists..."
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Artists",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.artists.size} artists",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleSearching(true) }) {
                            Icon(imageVector = Icons.Rounded.Search, contentDescription = "Search")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        }
    ) { paddingValues ->
        if (uiState.artists.isEmpty()) {
            if (uiState.searchQuery.isNotBlank()) {
                EmptyState(
                    title = "No Artists Found",
                    message = "No artists match '${uiState.searchQuery}'",
                    icon = Icons.Rounded.SearchOff,
                    modifier = Modifier.padding(paddingValues)
                )
            } else {
                EmptyState(
                    title = "No Artists",
                    message = "No artists discovered in your library.",
                    icon = Icons.Rounded.Person,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
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
