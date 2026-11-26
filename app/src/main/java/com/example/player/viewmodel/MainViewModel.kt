package com.example.player.viewmodel

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.player.data.MediaRepository
import com.example.player.data.SettingsRepository
import com.example.player.data.Track
import com.example.player.service.EqualizerEngine
import com.example.player.service.PlaybackService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlaybackUi(
    val title: String = "Ничего не играет",
    val artist: String = "",
    val artworkUri: Uri? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val hasMedia: Boolean = false
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val mediaRepo = MediaRepository(app)
    private val settingsRepo = SettingsRepository(app)

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _trackCount = MutableStateFlow(0)
    val trackCount: StateFlow<Int> = _trackCount.asStateFlow()

    private val _playback = MutableStateFlow(PlaybackUi())
    val playback: StateFlow<PlaybackUi> = _playback.asStateFlow()

    val wallpaperPath: StateFlow<String?> =
        settingsRepo.wallpaperPath.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val eqEnabled: StateFlow<Boolean> = EqualizerEngine.enabled
    val eqBands: StateFlow<List<Float>> = EqualizerEngine.bands
    val eqPreset: StateFlow<String?> = EqualizerEngine.preset
    val eqDeviceBands: StateFlow<Int> = EqualizerEngine.deviceBands

    private var controller: MediaController? = null
    private var loadStarted = false

    init {
        connectController()
        observeAudioSession()
    }

    // ---------- Сканирование треков и заставка ----------

    fun loadTracks() {
        if (loadStarted) return
        loadStarted = true

        viewModelScope.launch {
            val started = System.currentTimeMillis()
            _isLoading.value = true

            val list = mediaRepo.scanTracks { count -> _trackCount.value = count }
            _tracks.value = list
            _trackCount.value = list.size

            // Минимальное время показа заставки, чтобы анимация не мелькала
            val elapsed = System.currentTimeMillis() - started
            if (elapsed < MIN_SPLASH_MS) delay(MIN_SPLASH_MS - elapsed)

            _isLoading.value = false
        }
    }

    fun onPermissionDenied() {
        _isLoading.value = false
    }

    // ---------- Воспроизведение ----------

    fun playTrack(index: Int) {
        val c = controller ?: return
        val list = _tracks.value
        if (list.isEmpty()) return
        c.setMediaItems(list.map { it.toMediaItem() }, index, 0L)
        c.prepare()
        c.play()
    }

    fun togglePlay() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() {
        controller?.seekToNext()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    private fun connectController() {
        val app = getApplication<Application>()
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()

        future.addListener({
            val c = future.get()
            controller = c
            c.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    syncPlayback(player)
                }
            })
            syncPlayback(c)
            startPositionLoop()
        }, ContextCompat.getMainExecutor(app))
    }

    private fun syncPlayback(p: Player) {
        val md = p.mediaMetadata
        _playback.value = _playback.value.copy(
            title = md.title?.toString() ?: "Ничего не играет",
            artist = md.artist?.toString() ?: "",
            artworkUri = md.artworkUri,
            isPlaying = p.isPlaying,
            duration = p.duration.coerceAtLeast(0L),
            hasMedia = p.currentMediaItem != null
        )
    }

    private fun startPositionLoop() {
        viewModelScope.launch {
            while (true) {
                controller?.let { c ->
                    _playback.value = _playback.value.copy(
                        position = c.currentPosition.coerceAtLeast(0L),
                        duration = c.duration.coerceAtLeast(0L),
                        isPlaying = c.isPlaying
                    )
                }
                delay(500)
            }
        }
    }

    // ---------- Эквалайзер ----------

    private fun observeAudioSession() {
        viewModelScope.launch {
            PlaybackService.audioSessionId.collect { id ->
                if (id != 0) EqualizerEngine.attach(id)
            }
        }
    }

    fun setEqEnabled(on: Boolean) = EqualizerEngine.setEnabledState(on)
    fun setEqBand(index: Int, value: Float) = EqualizerEngine.setBandValue(index, value)
    fun selectEqPreset(name: String) = EqualizerEngine.selectPreset(name)

    // ---------- Обои ----------

    fun setWallpaper(uri: Uri) {
        viewModelScope.launch {
            runCatching { settingsRepo.saveWallpaper(uri) }
        }
    }

    fun resetWallpaper() {
        viewModelScope.launch {
            runCatching { settingsRepo.resetWallpaper() }
        }
    }

    private fun Track.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setArtworkUri(artworkUri)
                    .build()
            )
            .build()

    override fun onCleared() {
        controller?.release()
        controller = null
        super.onCleared()
    }

    companion object {
        private const val MIN_SPLASH_MS = 1200L
    }
}
