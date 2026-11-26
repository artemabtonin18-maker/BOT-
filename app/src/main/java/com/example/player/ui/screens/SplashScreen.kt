package com.example.player.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Заставка поверх главного экрана. Появляется плавно (fade + scale),
 * исчезает (fade out), когда isLoading становится false.
 */
@Composable
fun SplashOverlay(isLoading: Boolean, trackCount: Int) {
    AnimatedVisibility(
        visible = isLoading,
        modifier = Modifier.fillMaxSize(),
        exit = fadeOut(animationSpec = tween(durationMillis = 600))
    ) {
        val entered = remember { MutableTransitionState(false).apply { targetState = true } }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visibleState = entered,
                enter = fadeIn(animationSpec = tween(700)) +
                        scaleIn(animationSpec = tween(700), initialScale = 0.7f)
            ) {
                SplashContent(trackCount)
            }
        }
    }
}

@Composable
private fun SplashContent(trackCount: Int) {
    val transition = rememberInfiniteTransition(label = "splash")

    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val bars = List(5) { i ->
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 480 + i * 110, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar$i"
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                bars.forEachIndexed { i, bar ->
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(72.dp * bar.value)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                if (i % 2 == 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondary
                            )
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Загрузка треков...",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Найдено: $trackCount",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
