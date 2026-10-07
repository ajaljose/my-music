package com.example.musicplayer.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.ui.components.CurvedHeader
import com.example.musicplayer.ui.theme.AccentCyan
import com.example.musicplayer.ui.theme.AccentGold
import com.example.musicplayer.ui.theme.AccentPink
import com.example.musicplayer.ui.theme.BackgroundDark

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onNavigateToSongs: () -> Unit,
    onNavigateToAlbums: () -> Unit,
    onNavigateToArtists: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        CurvedHeader(
            title = "Your Library",
            subtitle = "${uiState.songs.size} tracks • ${uiState.albums.size} albums • ${uiState.artists.size} artists",
            headerHeight = 140.dp
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Favourites Highlight Card
            item {
                LibraryHeroCard(
                    title = "Favourites",
                    subtitle = "${uiState.favoritesCount} favorite songs",
                    icon = Icons.Rounded.Favorite,
                    iconColor = AccentPink,
                    onClick = onNavigateToFavorites
                )
            }

            // Category Items
            item {
                LibraryNavCard(
                    title = "All Songs",
                    count = "${uiState.songs.size} tracks",
                    icon = Icons.Rounded.MusicNote,
                    iconTint = AccentCyan,
                    onClick = onNavigateToSongs
                )
            }

            item {
                LibraryNavCard(
                    title = "Albums",
                    count = "${uiState.albums.size} albums",
                    icon = Icons.Rounded.Album,
                    iconTint = AccentGold,
                    onClick = onNavigateToAlbums
                )
            }

            item {
                LibraryNavCard(
                    title = "Artists",
                    count = "${uiState.artists.size} artists",
                    icon = Icons.Rounded.Person,
                    iconTint = AccentCyan,
                    onClick = onNavigateToArtists
                )
            }

            item {
                LibraryNavCard(
                    title = "Playlists",
                    count = "${uiState.playlists.size} playlists",
                    icon = Icons.Rounded.QueueMusic,
                    iconTint = AccentGold,
                    onClick = onNavigateToPlaylists
                )
            }

            item {
                LibraryNavCard(
                    title = "Folders",
                    count = "Device directories",
                    icon = Icons.Rounded.Folder,
                    iconTint = AccentCyan,
                    onClick = onNavigateToFolders
                )
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }
}

@Composable
private fun LibraryHeroCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, iconColor.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = Color(0xFF22242B),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = Color(0xFF2C313B)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF8E929E)
                )
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF8E929E),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun LibraryNavCard(
    title: String,
    count: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = Color(0xFF202228),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = Color(0xFF2B313C)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = count,
                    fontSize = 12.sp,
                    color = Color(0xFF8E929E)
                )
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF8E929E).copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
