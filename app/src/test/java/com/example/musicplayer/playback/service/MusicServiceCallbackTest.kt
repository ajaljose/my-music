package com.example.musicplayer.playback.service

import com.example.musicplayer.domain.model.Album
import com.example.musicplayer.domain.model.Artist
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.playback.manager.toMediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicServiceCallbackTest {

    @Test
    fun testSongToMediaItemHasPlayableFlag() {
        val song = Song(
            id = 42L,
            mediaStoreId = 42L,
            title = "Test Song",
            artist = "Test Artist",
            album = "Test Album",
            duration = 180000L,
            path = "/storage/emulated/0/Music/test.mp3",
            contentUriString = "content://media/external/audio/media/42"
        )

        val mediaItem = song.toMediaItem()
        assertEquals("42", mediaItem.mediaId)
        assertEquals("Test Song", mediaItem.mediaMetadata.title.toString())
        assertEquals("Test Artist", mediaItem.mediaMetadata.artist.toString())
        assertEquals("Test Album", mediaItem.mediaMetadata.albumTitle.toString())
        assertTrue("Song must be playable for Android Auto", mediaItem.mediaMetadata.isPlayable == true)
        assertFalse("Song must not be browsable", mediaItem.mediaMetadata.isBrowsable == true)
        if (mediaItem.mediaMetadata.artworkUri != null) {
            assertTrue(
                "Artwork URI must use AlbumArtContentProvider authority",
                mediaItem.mediaMetadata.artworkUri.toString().contains("com.example.musicplayer.albumart")
            )
        }
    }

    @Test
    fun testAlbumToMediaItemHasBrowsableFlag() {
        val album = Album(
            id = 100L,
            name = "Test Album",
            artist = "Test Artist",
            songCount = 10
        )

        val mediaItem = album.toMediaItem()
        assertEquals("album_100", mediaItem.mediaId)
        assertEquals("Test Album", mediaItem.mediaMetadata.title.toString())
        assertTrue("Album must be browsable for Android Auto", mediaItem.mediaMetadata.isBrowsable == true)
        assertFalse("Album must not be directly playable", mediaItem.mediaMetadata.isPlayable == true)
        if (mediaItem.mediaMetadata.artworkUri != null) {
            assertTrue(
                "Artwork URI must use AlbumArtContentProvider authority",
                mediaItem.mediaMetadata.artworkUri.toString().contains("com.example.musicplayer.albumart")
            )
        }
    }

    @Test
    fun testArtistToMediaItemHasBrowsableFlag() {
        val artist = Artist(
            id = 200L,
            name = "Test Artist",
            songCount = 5
        )

        val mediaItem = artist.toMediaItem()
        assertEquals("artist_200_Test Artist", mediaItem.mediaId)
        assertEquals("Test Artist", mediaItem.mediaMetadata.title.toString())
        assertTrue("Artist must be browsable for Android Auto", mediaItem.mediaMetadata.isBrowsable == true)
        assertFalse("Artist must not be directly playable", mediaItem.mediaMetadata.isPlayable == true)
    }

    @Test
    fun testPlaylistToMediaItemHasBrowsableFlag() {
        val playlist = Playlist(
            id = 300L,
            name = "My Chill Playlist",
            songCount = 12
        )

        val mediaItem = playlist.toMediaItem()
        assertEquals("playlist_300", mediaItem.mediaId)
        assertEquals("My Chill Playlist", mediaItem.mediaMetadata.title.toString())
        assertTrue("Playlist must be browsable for Android Auto", mediaItem.mediaMetadata.isBrowsable == true)
        assertFalse("Playlist must not be directly playable", mediaItem.mediaMetadata.isPlayable == true)
    }
}
