package com.example.player.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaRepository(private val context: Context) {

    /**
     * Сканирует аудиофайлы через MediaStore.
     * onProgress вызывается по мере чтения (каждые 25 треков), чтобы UI показывал счётчик.
     */
    suspend fun scanTracks(onProgress: (Int) -> Unit = {}): List<Track> =
        withContext(Dispatchers.IO) {
            val result = mutableListOf<Track>()

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    result += Track(
                        id = id,
                        title = cursor.getString(titleCol) ?: "Без названия",
                        artist = cursor.getString(artistCol)
                            ?.takeIf { it != "<unknown>" }
                            ?: "Неизвестный исполнитель",
                        durationMs = cursor.getLong(durationCol),
                        albumId = cursor.getLong(albumCol),
                        uri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                        )
                    )
                    if (result.size % 25 == 0) onProgress(result.size)
                }
            }

            onProgress(result.size)
            result
        }
}
