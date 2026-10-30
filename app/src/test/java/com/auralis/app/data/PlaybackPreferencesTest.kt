package com.auralis.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PlaybackPreferencesTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Same file name PlaybackPreferences uses; start every test from a clean store.
        context.getSharedPreferences("auralis_playback", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun defaultSpeedIsOne() {
        assertEquals(1.0f, PlaybackPreferences(context).speed.value, 0.0001f)
    }

    @Test
    fun speedSurvivesRecreation() {
        PlaybackPreferences(context).setSpeed(1.5f)
        // A brand-new instance simulates the app being reopened.
        assertEquals(1.5f, PlaybackPreferences(context).speed.value, 0.0001f)
    }

    @Test
    fun setSpeedClampsToSupportedRange() {
        val prefs = PlaybackPreferences(context)
        prefs.setSpeed(0.1f)
        assertEquals(0.5f, prefs.speed.value, 0.0001f)
        prefs.setSpeed(9f)
        assertEquals(2.0f, prefs.speed.value, 0.0001f)
        // The clamped value is what gets persisted, not the raw input.
        assertEquals(2.0f, PlaybackPreferences(context).speed.value, 0.0001f)
    }

    @Test
    fun outOfRangeStoredValueIsClampedOnRead() {
        context.getSharedPreferences("auralis_playback", Context.MODE_PRIVATE)
            .edit().putFloat("playback_speed", 5f).commit()
        assertEquals(2.0f, PlaybackPreferences(context).speed.value, 0.0001f)
    }
}
