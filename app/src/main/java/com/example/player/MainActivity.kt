package com.example.player

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.player.ui.screens.EqualizerScreen
import com.example.player.ui.screens.PlayerScreen
import com.example.player.ui.screens.SettingsScreen
import com.example.player.ui.screens.SplashOverlay
import com.example.player.ui.screens.TracksScreen
import com.example.player.ui.theme.MyPlayerTheme
import com.example.player.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyPlayerTheme {
                App(viewModel)
            }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    TRACKS("Треки", Icons.Filled.LibraryMusic),
    PLAYER("Плеер", Icons.Filled.MusicNote),
    EQUALIZER("Эквалайзер", Icons.Filled.Equalizer),
    SETTINGS("Настройки", Icons.Filled.Settings)
}

private fun hasAudioPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

@Composable
private fun App(vm: MainViewModel) {
    val context = LocalContext.current

    val tracks by vm.tracks.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val trackCount by vm.trackCount.collectAsState()
    val playback by vm.playback.collectAsState()
    val wallpaper by vm.wallpaperPath.collectAsState()
    val eqEnabled by vm.eqEnabled.collectAsState()
    val eqBands by vm.eqBands.collectAsState()
    val eqPreset by vm.eqPreset.collectAsState()
    val eqDeviceBands by vm.eqDeviceBands.collectAsState()

    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tab = Tab.entries[tabIndex]

    val audioPermission =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    val permissions: Array<String> =
        if (Build.VERSION.SDK_INT >= 33) arrayOf(audioPermission, Manifest.permission.POST_NOTIFICATIONS)
        else arrayOf(audioPermission)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasAudioPermission(context, audioPermission)) vm.loadTracks()
        else vm.onPermissionDenied()
    }

    val pickWallpaper = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) vm.setWallpaper(uri)
    }

    LaunchedEffect(Unit) {
        if (hasAudioPermission(context, audioPermission)) vm.loadTracks()
        else permissionLauncher.launch(permissions)
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tabIndex = index },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding).fillMaxSize()) {
                when (tab) {
                    Tab.TRACKS -> TracksScreen(
                        tracks = tracks,
                        wallpaperPath = wallpaper,
                        onTrackClick = { index ->
                            vm.playTrack(index)
                            tabIndex = Tab.PLAYER.ordinal
                        },
                        onRequestPermission = { permissionLauncher.launch(permissions) }
                    )
                    Tab.PLAYER -> PlayerScreen(
                        state = playback,
                        wallpaperPath = wallpaper,
                        onToggle = vm::togglePlay,
                        onNext = vm::next,
                        onPrevious = vm::previous,
                        onSeek = vm::seekTo
                    )
                    Tab.EQUALIZER -> EqualizerScreen(
                        enabled = eqEnabled,
                        bands = eqBands,
                        preset = eqPreset,
                        deviceBands = eqDeviceBands,
                        wallpaperPath = wallpaper,
                        onEnabledChange = vm::setEqEnabled,
                        onBandChange = vm::setEqBand,
                        onPresetSelect = vm::selectEqPreset
                    )
                    Tab.SETTINGS -> SettingsScreen(
                        wallpaperPath = wallpaper,
                        onPickWallpaper = {
                            pickWallpaper.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onResetWallpaper = vm::resetWallpaper
                    )
                }
            }
        }

        // Заставка поверх всего интерфейса, пока идёт сканирование
        SplashOverlay(isLoading = isLoading, trackCount = trackCount)
    }
}
