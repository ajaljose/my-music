package com.example.musicplayer.domain.util

import com.example.musicplayer.domain.model.Song
import java.io.File

object MetadataCleaner {

    /**
     * Cleans metadata string by removing hidden site names.
     * Matching is CASE-INSENSITIVE (e.g., "site.com" removes "SITE.COM", "Site.Com", etc.)
     */
    fun cleanText(text: String, hiddenSites: Set<String>): String {
        if (text.isBlank() || hiddenSites.isEmpty()) return text

        var result = text

        for (site in hiddenSites) {
            val trimmedSite = site.trim()
            if (trimmedSite.isBlank()) continue

            // Strip optional www. or http:// if present in site target
            val baseSite = trimmedSite
                .replace(Regex("""(?i)^https?://"""), "")
                .replace(Regex("""(?i)^www\."""), "")

            val escapedBase = Regex.escape(baseSite)

            // 1. Bracketed pattern: [ (www.)site.com ], ( Downloaded from site.com ), etc.
            val bracketRegex = Regex(
                """(?i)[\(\[\{]\s*(?:downloaded\s+(?:from|by)|from|by|via|@)?\s*(?:https?://)?(?:www\.)?""" + escapedBase + """\s*[\)\]\}]"""
            )
            result = result.replace(bracketRegex, "")

            // 2. Prefix pattern: downloaded from site.com, via site.com, @ site.com
            val prefixRegex = Regex(
                """(?i)\b(?:downloaded\s+(?:from|by)|from|by|via|@)\s*(?:https?://)?(?:www\.)?""" + escapedBase + """\b"""
            )
            result = result.replace(prefixRegex, "")

            // 3. Raw site pattern case-insensitively with optional www. / http(s)://
            val rawRegex = Regex(
                """(?i)(?:https?://)?(?:www\.)?""" + escapedBase
            )
            result = result.replace(rawRegex, "")
        }

        return cleanupPunctuationAndSpaces(result)
    }

    /**
     * Cleans up leftover punctuation, empty brackets, and whitespace after site name removal.
     */
    private fun cleanupPunctuationAndSpaces(text: String): String {
        var s = text

        // Remove leftover empty brackets
        s = s.replace(Regex("""[\(\[\{]\s*[\)\]\}]"""), "")

        // Normalize spaces
        s = s.replace(Regex("""\s+"""), " ")

        // Trim leading and trailing punctuation delimiters
        val delimiters = charArrayOf('-', '_', '|', ':', '/', '\\', '@', '~', ',', ';')

        var changed = true
        while (changed) {
            val prev = s
            s = s.trimStart(*delimiters).trimEnd(*delimiters).trim()
            changed = (s != prev)
        }

        // Collapse double delimiters in the middle (e.g., "Artist - - Title" -> "Artist - Title")
        s = s.replace(Regex("""\s*[\-_|:/\\@~,;]+\s*[\-_|:/\\@~,;]+\s*"""), " - ")

        return s.trim()
    }

    /**
     * Cleans a Song's title, artist, album, and composer fields.
     */
    fun cleanSong(song: Song, hiddenSites: Set<String>): Song {
        if (hiddenSites.isEmpty()) return song

        val cleanedTitle = cleanText(song.title, hiddenSites).ifBlank {
            val fallback = File(song.path).nameWithoutExtension
            cleanText(fallback, hiddenSites).ifBlank { "Unknown Title" }
        }

        val cleanedArtist = cleanText(song.artist, hiddenSites).ifBlank { "Unknown Artist" }
        val cleanedAlbum = cleanText(song.album, hiddenSites).ifBlank { "Unknown Album" }
        val cleanedComposer = if (song.composer.isNotBlank()) cleanText(song.composer, hiddenSites) else ""

        return song.copy(
            title = cleanedTitle,
            artist = cleanedArtist,
            album = cleanedAlbum,
            composer = cleanedComposer
        )
    }
}
