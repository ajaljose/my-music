package com.example.musicplayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.ui.theme.AccentCyan
import com.example.musicplayer.ui.theme.AccentGold
import com.example.musicplayer.ui.theme.AccentPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
    song: Song,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color(0xFF22242B)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header with song info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                ArtworkImage(
                    artworkUri = song.albumArtUri,
                    songUri = song.contentUri,
                    path = song.path,
                    size = 54.dp,
                    shape = RoundedCornerShape(12.dp),
                    seed = song.album
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = song.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${song.artist} • ${song.album}",
                        fontSize = 13.sp,
                        color = Color(0xFF8E929E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp), color = Color.White.copy(alpha = 0.08f))

            OptionItem(
                icon = Icons.Rounded.PlayArrow,
                title = "Play Next",
                iconTint = AccentGold,
                onClick = {
                    onPlayNext()
                    onDismiss()
                }
            )

            OptionItem(
                icon = Icons.Rounded.QueueMusic,
                title = "Add to Queue",
                iconTint = AccentCyan,
                onClick = {
                    onAddToQueue()
                    onDismiss()
                }
            )

            OptionItem(
                icon = Icons.Rounded.PlaylistAdd,
                title = "Add to Playlist",
                iconTint = AccentGold,
                onClick = {
                    onAddToPlaylist()
                    onDismiss()
                }
            )

            OptionItem(
                icon = if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                title = if (song.isFavorite) "Remove from Favorites" else "Add to Favorites",
                iconTint = if (song.isFavorite) AccentPink else Color(0xFF8E929E),
                onClick = {
                    onToggleFavorite()
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OptionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    iconTint: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = CircleShape,
            color = Color(0xFF2C313B)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}
