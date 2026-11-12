package com.example.musicplayer.domain.util

import com.example.musicplayer.domain.model.Song
import org.junit.Assert.*
import org.junit.Test

class MetadataCleanerTest {

    @Test
    fun testCaseInsensitiveWebsiteHiding() {
        val hiddenSites = setOf("pagalworld.com", "mp3juice.cc")

        // Capital letters in input, lowercase in hiddenSites
        val input1 = "Shape of You - PAGALWORLD.COM"
        val cleaned1 = MetadataCleaner.cleanText(input1, hiddenSites)
        assertEquals("Shape of You", cleaned1)

        // Mixed case in input
        val input2 = "Blinding Lights [Mp3Juice.CC]"
        val cleaned2 = MetadataCleaner.cleanText(input2, hiddenSites)
        assertEquals("Blinding Lights", cleaned2)

        // Lowercase in input
        val input3 = "Despacito (pagalworld.com)"
        val cleaned3 = MetadataCleaner.cleanText(input3, hiddenSites)
        assertEquals("Despacito", cleaned3)
    }

    @Test
    fun testBracketsAndDelimitersRemoval() {
        val hiddenSites = setOf("y2mate.is", "songs.pk")

        val input1 = "y2mate.is - Artist Name - Song Title"
        val cleaned1 = MetadataCleaner.cleanText(input1, hiddenSites)
        assertEquals("Artist Name - Song Title", cleaned1)

        val input2 = "Song Title (Downloaded from www.songs.pk)"
        val cleaned2 = MetadataCleaner.cleanText(input2, hiddenSites)
        assertEquals("Song Title", cleaned2)

        val input3 = "[www.y2mate.is] Song Title | Official Audio"
        val cleaned3 = MetadataCleaner.cleanText(input3, hiddenSites)
        assertEquals("Song Title | Official Audio", cleaned3)
    }

    @Test
    fun testCleanSongObject() {
        val hiddenSites = setOf("site.com")
        val song = Song(
            mediaStoreId = 10,
            title = "Awesome Track [site.com]",
            artist = "Artist @ SITE.COM",
            album = "Album Name (site.com)",
            composer = "Composer site.com",
            duration = 180000,
            path = "/music/Awesome Track.mp3",
            contentUriString = "content://media/10"
        )

        val cleanedSong = MetadataCleaner.cleanSong(song, hiddenSites)

        assertEquals("Awesome Track", cleanedSong.title)
        assertEquals("Artist", cleanedSong.artist)
        assertEquals("Album Name", cleanedSong.album)
        assertEquals("Composer", cleanedSong.composer)
    }
}
