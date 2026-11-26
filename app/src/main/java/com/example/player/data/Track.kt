package com.example.player.data

import android.net.Uri

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val albumId: Long,
    val uri: Uri
) {
    val artworkUri: Uri
        get() = Uri.parse("content://media/external/audio/albumart/$albumId")
}
