package com.example.player.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val wallpaperKey = stringPreferencesKey("wallpaper_path")

    /** Путь к локальной копии обоев или null, если обои стандартные. */
    val wallpaperPath: Flow<String?> = context.dataStore.data.map { it[wallpaperKey] }

    /**
     * Копирует выбранное изображение в внутреннее хранилище приложения.
     * URI из Photo Picker не сохраняют доступ надолго, поэтому копия надёжнее.
     */
    suspend fun saveWallpaper(source: Uri) {
        withContext(Dispatchers.IO) {
            val target = File(context.filesDir, "wallpaper_${System.currentTimeMillis()}.jpg")
            val input = context.contentResolver.openInputStream(source)
                ?: throw IllegalStateException("Не удалось открыть изображение")
            input.use { src ->
                target.outputStream().use { dst -> src.copyTo(dst) }
            }
            val oldPath = context.dataStore.data.first()[wallpaperKey]
            context.dataStore.edit { it[wallpaperKey] = target.absolutePath }
            oldPath?.let { File(it).delete() }
        }
    }

    suspend fun resetWallpaper() {
        withContext(Dispatchers.IO) {
            val oldPath = context.dataStore.data.first()[wallpaperKey]
            context.dataStore.edit { it.remove(wallpaperKey) }
            oldPath?.let { File(it).delete() }
        }
    }
}
