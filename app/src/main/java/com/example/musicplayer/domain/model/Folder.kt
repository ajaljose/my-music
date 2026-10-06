package com.example.musicplayer.domain.model

data class Folder(
    val path: String,
    val name: String,
    val songCount: Int,
    val songs: List<Song> = emptyList(),
    val subFolders: List<Folder> = emptyList()
)
