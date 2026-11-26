package com.example.player.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.player.service.EqualizerEngine
import com.example.player.ui.components.WallpaperBackground
import kotlin.math.roundToInt

@Composable
fun EqualizerScreen(
    enabled: Boolean,
    bands: List<Float>,
    preset: String?,
    deviceBands: Int,
    wallpaperPath: String?,
    onEnabledChange: (Boolean) -> Unit,
    onBandChange: (Int, Float) -> Unit,
    onPresetSelect: (String) -> Unit
) {
    WallpaperBackground(wallpaperPath) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Эквалайзер",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }

            Spacer(Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(EqualizerEngine.PRESETS.keys.toList()) { name ->
                    FilterChip(
                        selected = preset == name,
                        onClick = { onPresetSelect(name) },
                        label = { Text(name) },
                        enabled = enabled
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    bands.forEachIndexed { index, value ->
                        BandSlider(
                            label = EqualizerEngine.UI_LABELS[index],
                            value = value,
                            enabled = enabled,
                            onChange = { onBandChange(index, it) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Аппаратных полос на устройстве: $deviceBands. Ползунки распределяют сигнал по ним.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BandSlider(label: String, value: Float, enabled: Boolean, onChange: (Float) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(34.dp)
    ) {
        Text(
            text = "${value.roundToInt()}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(Modifier.height(220.dp), contentAlignment = Alignment.Center) {
            Slider(
                value = value,
                onValueChange = onChange,
                valueRange = -12f..12f,
                enabled = enabled,
                modifier = Modifier
                    .requiredWidth(220.dp)
                    .graphicsLayer { rotationZ = -90f }
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
