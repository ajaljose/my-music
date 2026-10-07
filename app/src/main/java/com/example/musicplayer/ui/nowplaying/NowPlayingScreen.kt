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
import com.example.musicplayer.domain.model.RepeatMode
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.playback.manager.MusicPlaybackManager
import com.example.musicplayer.ui.components.ArtworkImage
import com.example.musicplayer.ui.components.SleepTimerDialog
import com.example.musicplayer.ui.components.SongOptionsBottomSheet
import com.example.musicplayer.ui.components.WavyProgressBar

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

    var prominentColorTarget by remember { mutableStateOf(Color(0xFF008B9B)) }
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
            prominentColorTarget = color
        }
    }

    var dragOffset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1B1B1D))
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
                // Top Curved / Dome Background with Prominent Color
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
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
                            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Row {
                            IconButton(onClick = { showSleepTimerDialog = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.Timer,
                                    contentDescription = "Sleep Timer",
                                    tint = if (sleepTimerState.isActive) Color(0xFFFFD54F) else Color.White
                                )
                            }
                            IconButton(onClick = { showQueueSheet = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.QueueMusic,
                                    contentDescription = "Queue",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Large Album Art Box centered on the curve boundary
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
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
                            size = 250.dp,
                            shape = RoundedCornerShape(22.dp),
                            contentDescription = currentSong.title,
                            seed = currentSong.album,
                            modifier = Modifier
                                .size(250.dp)
                                .shadow(24.dp, RoundedCornerShape(22.dp))
                                .border(1.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(22.dp))
                        )
                    }
                }

                // Song Title, Artist & Options Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
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
                            tint = if (currentSong.isFavorite) Color(0xFFFF4081) else Color.Gray,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = currentSong.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${currentSong.artist} • ${currentSong.album}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { showOptionsSheet = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Options",
                            tint = Color.Gray,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Wavy Progress Bar
                WavyProgressBar(
                    currentPositionMs = playbackState.currentPosition,
                    totalDurationMs = playbackState.totalDuration,
                    onSeek = { playbackManager.seekTo(it) },
                    activeColor = Color(0xFFFFD54F),
                    inactiveColor = Color(0xFF3E3E42),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                // Playback Controls Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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
                            tint = if (playbackState.isShuffleEnabled) Color(0xFFFFD54F) else Color.Gray
                        )
                    }

                    // Skip Previous
                    IconButton(
                        onClick = { playbackManager.skipToPrevious() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Play/Pause Center Button (Vibrant Yellow/Gold)
                    Surface(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .shadow(12.dp, CircleShape, ambientColor = Color(0xFFFFD54F), spotColor = Color(0xFFFFD54F)),
                        color = Color(0xFFFFD54F),
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
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }
                    }

                    // Skip Next
                    IconButton(
                        onClick = { playbackManager.skipToNext() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
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
                        val tint = if (playbackState.repeatMode != RepeatMode.OFF) Color(0xFFFFD54F) else Color.Gray
                        Icon(
                            imageVector = icon,
                            contentDescription = "Repeat",
                            tint = tint
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Track Information Card (Matching screenshot)
                TrackInformationCard(
                    song = currentSong,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))
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
        color = Color(0xFF232427),
        shape = RoundedCornerShape(20.dp)
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
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Track Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(14.dp))

            // TITLE
            TrackInfoRow(
                icon = Icons.Rounded.MusicNote,
                label = "TITLE",
                value = song.title
            )

            Spacer(modifier = Modifier.height(14.dp))

            // AUTHORS / ARTISTS
            TrackInfoRow(
                icon = Icons.Rounded.Person,
                label = "AUTHORS / ARTISTS",
                value = song.artist
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ALBUM
            TrackInfoRow(
                icon = Icons.Rounded.Album,
                label = "ALBUM",
                value = song.album
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
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = Color(0xFF2C3036)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF00B4D8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value.ifBlank { "Unknown" },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
