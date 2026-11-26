package com.example.player.service

import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Эквалайзер на основе android.media.audiofx.Equalizer.
 * UI показывает 10 полос. Большинство телефонов имеют 5 аппаратных полос,
 * поэтому значения 10 полос усредняются и распределяются по аппаратным.
 */
object EqualizerEngine {

    val UI_FREQUENCIES = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
    val UI_LABELS = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

    val PRESETS: Map<String, List<Float>> = linkedMapOf(
        "Flat" to listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        "Rock" to listOf(5f, 4f, 3f, 1f, -1f, -1f, 1f, 3f, 4f, 4f),
        "Pop" to listOf(-1f, 2f, 4f, 4f, 2f, 0f, -1f, -1f, 1f, 2f),
        "Jazz" to listOf(3f, 2f, 1f, 2f, -2f, -2f, 0f, 1f, 2f, 3f),
        "Classical" to listOf(4f, 3f, 2f, 1f, 0f, 0f, 0f, 1f, 2f, 3f),
        "Bass Boost" to listOf(6f, 5f, 4f, 2f, 1f, 0f, 0f, 0f, 0f, 0f),
        "Vocal" to listOf(-2f, -1f, 0f, 2f, 4f, 4f, 3f, 1f, 0f, -1f)
    )

    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _bands = MutableStateFlow(PRESETS.getValue("Flat"))
    val bands: StateFlow<List<Float>> = _bands.asStateFlow()

    private val _preset = MutableStateFlow<String?>("Flat")
    val preset: StateFlow<String?> = _preset.asStateFlow()

    private val _deviceBands = MutableStateFlow(0)
    val deviceBands: StateFlow<Int> = _deviceBands.asStateFlow()

    private var equalizer: Equalizer? = null
    private var attachedSession = -1

    @Synchronized
    fun attach(sessionId: Int) {
        if (sessionId == attachedSession && equalizer != null) return
        release()
        attachedSession = sessionId
        equalizer = try {
            val eq = Equalizer(0, sessionId)
            eq.enabled = _enabled.value
            eq
        } catch (e: Exception) {
            null
        }
        _deviceBands.value = equalizer?.numberOfBands?.toInt() ?: 0
        applyToDevice()
    }

    @Synchronized
    fun setBandValue(index: Int, value: Float) {
        _bands.value = _bands.value.toMutableList().also { it[index] = value }
        _preset.value = null
        applyToDevice()
    }

    @Synchronized
    fun selectPreset(name: String) {
        val values = PRESETS[name] ?: return
        _bands.value = values
        _preset.value = name
        applyToDevice()
    }

    @Synchronized
    fun setEnabledState(on: Boolean) {
        _enabled.value = on
        equalizer?.enabled = on
    }

    private fun applyToDevice() {
        val eq = equalizer ?: return
        val count = eq.numberOfBands.toInt()
        val centers = FloatArray(count) { eq.getCenterFreq(it.toShort()) / 1000f }
        val range = eq.bandLevelRange
        val minMb = range[0].toInt()
        val maxMb = range[1].toInt()
        val ui = _bands.value

        val sums = FloatArray(count)
        val counts = IntArray(count)
        UI_FREQUENCIES.forEachIndexed { i, freq ->
            val b = nearestIndex(centers, freq)
            sums[b] += ui[i]
            counts[b]++
        }

        for (b in 0 until count) {
            val db = if (counts[b] > 0) {
                sums[b] / counts[b]
            } else {
                ui[nearestIndex(UI_FREQUENCIES, centers[b])]
            }
            val millibels = (db * 100f).roundToInt().coerceIn(minMb, maxMb)
            eq.setBandLevel(b.toShort(), millibels.toShort())
        }
    }

    private fun nearestIndex(source: FloatArray, target: Float): Int {
        var best = 0
        for (i in source.indices) {
            if (abs(source[i] - target) < abs(source[best] - target)) best = i
        }
        return best
    }

    @Synchronized
    private fun release() {
        equalizer?.release()
        equalizer = null
        attachedSession = -1
    }
}
