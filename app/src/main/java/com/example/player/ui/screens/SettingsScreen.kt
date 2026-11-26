package com.example.player.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.player.ui.components.WallpaperBackground
import com.example.player.ui.components.rememberBitmapFromPath

@Composable
fun SettingsScreen(
    wallpaperPath: String?,
    onPickWallpaper: () -> Unit,
    onResetWallpaper: () -> Unit
) {
    WallpaperBackground(wallpaperPath) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Text(
                text = "Настройки",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Обои",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Изображение станет фоном главного экрана и экрана плеера.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    WallpaperPreview(wallpaperPath)

                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onPickWallpaper) {
                            Icon(Icons.Filled.Image, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Выбрать из галереи")
                        }
                        OutlinedButton(
                            onClick = onResetWallpaper,
                            enabled = wallpaperPath != null
                        ) {
                            Text("Сбросить")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperPreview(path: String?) {
    val bitmap = rememberBitmapFromPath(path)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Предпросмотр обоев",
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Text(
            text = "Стандартный фон",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(top = 20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
