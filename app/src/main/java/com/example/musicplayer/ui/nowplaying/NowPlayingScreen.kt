package com.example.musicplayer.ui.nowplaying

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.musicplayer.domain.model.RepeatMode
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.playback.manager.MusicPlaybackManager
import com.example.musicplayer.ui.components.ArtworkImage
import com.example.musicplayer.ui.components.SleepTimerDialog
import com.example.musicplayer.ui.components.SongOptionsBottomSheet
import com.example.musicplayer.ui.components.WavyProgressBar
import com.example.musicplayer.ui.theme.AccentCyan
import com.example.musicplayer.ui.theme.AccentGold
import com.example.musicplayer.ui.theme.AccentPink
import com.example.musicplayer.ui.theme.BackgroundDark

@Composable
fun NowPlayingScreen(
    playbackManager: MusicPlaybackManager,
    onClose: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playbackState by playbackManager.playbackState.collectAsState()
    val sleepTimerState by playbackManager.sleepTimerState.collectAsState()
    val currentSong = playbackState.currentSong

    var showQueueSheet by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showOptionsSheet by remember { mutableStateOf(false) }

    var prominentColorTarget by remember { mutableStateOf(Color(0xFF008B9E)) }
    val animatedProminentColor by animateColorAsState(
        targetValue = prominentColorTarget,
        animationSpec = tween(durationMillis = 600),
        label = "prominentColor"
    )

    LaunchedEffect(currentSong?.id) {
        if (currentSong != null) {
            val color = extractProminentColor(
                context = context,
                artworkUri = currentSong.albumArtUri,
                songUri = currentSong.contentUri,
                path = currentSong.path
            )
            prominentColorTarget = if (color != Color(0xFF1E1E24)) color else Color(0xFF008B9E)
        }
    }

    var dragOffset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        if (currentSong == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No track playing",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Curved / Dome Background with Teal / Prominent Extracted Color
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val path = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(w, 0f)
                            lineTo(w, h * 0.65f)
                            quadraticTo(
                                w / 2f, h * 1.15f,
                                0f, h * 0.65f
                            )
                            close()
                        }
                        drawPath(path = path, color = animatedProminentColor)
                    }

                    // Top Bar overlay
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Dismiss",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = "Now Playing",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showSleepTimerDialog = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.Timer,
                                    contentDescription = "Sleep Timer",
                                    tint = if (sleepTimerState.isActive) AccentGold else Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            IconButton(onClick = { showQueueSheet = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.QueueMusic,
                                    contentDescription = "Queue",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }

                    // Large Album Art Box centered on the curve boundary
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (dragOffset < -80) {
                                            playbackManager.skipToNext()
                                        } else if (dragOffset > 80) {
                                            playbackManager.skipToPrevious()
                                        }
                                        dragOffset = 0f
                                    },
                                    onDragCancel = { dragOffset = 0f },
                                    onHorizontalDrag = { _, dragAmount ->
                                        dragOffset += dragAmount
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        ArtworkImage(
                            artworkUri = currentSong.albumArtUri,
                            songUri = currentSong.contentUri,
                            path = currentSong.path,
                            size = 260.dp,
                            shape = RoundedCornerShape(22.dp),
                            contentDescription = currentSong.title,
                            seed = currentSong.album,
                            modifier = Modifier
                                .size(260.dp)
                                .shadow(28.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black.copy(alpha = 0.6f), spotColor = Color.Black)
                                .border(1.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(22.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Song Title, Artist, Favorite & 3-Dots Options Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (currentSong.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (currentSong.isFavorite) AccentPink else Color(0xFF8E929E),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                    ) {
                        Text(
                            text = currentSong.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (currentSong.album.isNotBlank()) "${currentSong.artist} - ${currentSong.album}" else currentSong.artist,
                            fontSize = 14.sp,
                            color = Color(0xFF8E929E),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { showOptionsSheet = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = "Options",
                            tint = Color(0xFF8E929E),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Squiggly Wavy Progress Bar with Timestamps
                WavyProgressBar(
                    currentPositionMs = playbackState.currentPosition,
                    totalDurationMs = playbackState.totalDuration,
                    onSeek = { playbackManager.seekTo(it) },
                    activeColor = AccentGold,
                    inactiveColor = Color(0xFF383A42),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Playback Controls Row (Matching Screenshot)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(
                        onClick = { playbackManager.toggleShuffle() },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.isShuffleEnabled) AccentGold else Color(0xFF8E929E),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Skip Previous
                    IconButton(
                        onClick = { playbackManager.skipToPrevious() },
                        modifier = Modifier.size(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Play/Pause Center Glowing Gold Button
                    Surface(
                        modifier = Modifier
                            .size(70.dp)
                            .shadow(16.dp, CircleShape, ambientColor = AccentGold, spotColor = AccentGold)
                            .clip(CircleShape),
                        color = AccentGold,
                        onClick = { playbackManager.playPause() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (playbackState.isLoading) {
                                CircularProgressIndicator(
                                    color = Color.Black,
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }

                    // Skip Next
                    IconButton(
                        onClick = { playbackManager.skipToNext() },
                        modifier = Modifier.size(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Repeat
                    IconButton(
                        onClick = { playbackManager.toggleRepeatMode() },
                        modifier = Modifier.size(44.dp)
                    ) {
                        val icon = when (playbackState.repeatMode) {
                            RepeatMode.ONE -> Icons.Rounded.RepeatOne
                            else -> Icons.Rounded.Repeat
                        }
                        val tint = if (playbackState.repeatMode != RepeatMode.OFF) AccentGold else Color(0xFF8E929E)
                        Icon(
                            imageVector = icon,
                            contentDescription = "Repeat",
                            tint = tint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Track Information Card (Matching Screenshot exactly)
                TrackInformationCard(
                    song = currentSong,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }

    if (showQueueSheet) {
        QueueBottomSheet(
            queue = playbackState.queue,
            currentIndex = playbackState.currentQueueIndex,
            onSongClick = { index ->
                playbackManager.skipToQueueItem(index)
            },
            onRemoveItem = { index ->
                playbackManager.removeFromQueue(index)
            },
            onClearQueue = {
                playbackManager.clearQueue()
            },
            onDismiss = { showQueueSheet = false }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            sleepTimerState = sleepTimerState,
            onSetTimerMinutes = { mins ->
                playbackManager.startSleepTimer(mins)
            },
            onSetTimerEndOfSong = {
                playbackManager.startSleepTimerEndCurrentSong()
            },
            onCancelTimer = {
                playbackManager.cancelSleepTimer()
            },
            onDismiss = { showSleepTimerDialog = false }
        )
    }

    if (showOptionsSheet && currentSong != null) {
        SongOptionsBottomSheet(
            song = currentSong,
            onPlayNext = { playbackManager.playNext(currentSong) },
            onAddToQueue = { playbackManager.addToQueue(currentSong) },
            onAddToPlaylist = {},
            onToggleFavorite = onToggleFavorite,
            onDismiss = { showOptionsSheet = false }
        )
    }
}

@Composable
private fun TrackInformationCard(
    song: Song,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF22242B),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Track Information",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // TITLE
            TrackInfoRow(
                icon = Icons.Rounded.MusicNote,
                label = "TITLE",
                value = song.title
            )

            Spacer(modifier = Modifier.height(16.dp))

            // AUTHORS / ARTISTS
            TrackInfoRow(
                icon = Icons.Rounded.Person,
                label = "AUTHORS / ARTISTS",
                value = song.artist
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ALBUM
            TrackInfoRow(
                icon = Icons.Rounded.Album,
                label = "ALBUM",
                value = song.album.ifBlank { "Unknown Album" }
            )
        }
    }
}

@Composable
private fun TrackInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = CircleShape,
            color = Color(0xFF2B313C)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFF8E929E),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value.ifBlank { "Unknown" },
                fontSize = 15.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
