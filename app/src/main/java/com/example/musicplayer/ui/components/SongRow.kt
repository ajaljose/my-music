package com.example.musicplayer.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.ui.theme.AccentGold
import com.example.musicplayer.ui.theme.AccentPink

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: Song,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    showDuration: Boolean = true,
    showFavorite: Boolean = true
) {
    val titleColor = if (isPlaying) AccentGold else Color.White
    val fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onOptionsClick
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            ArtworkImage(
                artworkUri = song.albumArtUri,
                songUri = song.contentUri,
                path = song.path,
                size = 52.dp,
                shape = RoundedCornerShape(12.dp),
                contentDescription = song.title,
                seed = song.album.ifBlank { song.artist }
            )
            if (isPlaying) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Equalizer,
                            contentDescription = "Playing",
                            tint = AccentGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                fontSize = 15.sp,
                color = titleColor,
                fontWeight = fontWeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = if (song.album.isNotBlank()) "${song.artist} • ${song.album}" else song.artist,
                fontSize = 13.sp,
                color = Color(0xFF8E929E),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (showDuration) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = song.formattedDuration,
                fontSize = 12.sp,
                color = Color(0xFF8E929E),
                fontWeight = FontWeight.Medium
            )
        }

        if (showFavorite) {
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (song.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (song.isFavorite) AccentPink else Color(0xFF8E929E).copy(alpha = 0.7f),
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        IconButton(
            onClick = onOptionsClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Song options",
                tint = Color(0xFF8E929E),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
