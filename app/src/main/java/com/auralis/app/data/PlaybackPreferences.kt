package com.auralis.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lightweight in-memory + SharedPreferences store for playback settings.
 * Keeps the MVP free of DataStore boilerplate while surviving process death.
 */
class PlaybackPreferences(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _speed = MutableStateFlow(prefs.getFloat(KEY_SPEED, 1.0f).coerceIn(0.5f, 2.0f))
    val speed: StateFlow<Float> = _speed.asStateFlow()

    fun setSpeed(value: Float) {
        val clamped = value.coerceIn(0.5f, 2.0f)
        _speed.value = clamped
        prefs.edit().putFloat(KEY_SPEED, clamped).apply()
    }

    companion object {
        private const val PREFS_NAME = "auralis_playback"
        private const val KEY_SPEED = "playback_speed"
    }
}
