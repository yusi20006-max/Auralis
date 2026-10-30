package com.auralis.app.data

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.auralis.app.data.db.EpisodeEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class EpisodeModelTest {

    private val fullEpisode = Episode(
        id = "ep-1",
        title = "اپیزود نمونه",
        podcastName = "Auralis",
        description = "توضیح",
        durationMs = 120_000L,
        artworkUri = Uri.parse("content://art/1"),
        localUri = Uri.parse("content://com.android.providers.media.documents/document/audio%3A123"),
        remoteId = "remote-9",
        audioUrl = "https://example.com/a.mp3",
        isAiGenerated = false,
        lastPositionMs = 30_000L,
        lastPlayedAt = 1_700_000_000_000L,
    )

    @Test
    fun entityRoundTripPreservesEveryField() {
        val restored = EpisodeEntity.from(fullEpisode).toEpisode()
        assertEquals(fullEpisode, restored)
    }

    @Test
    fun entityRoundTripKeepsNullableFieldsNull() {
        val minimal = Episode(id = "ep-2", title = "فقط عنوان")
        val restored = EpisodeEntity.from(minimal).toEpisode()
        assertEquals(minimal, restored)
        assertNull(restored.localUri)
        assertNull(restored.artworkUri)
        assertNull(restored.remoteId)
        assertNull(restored.lastPlayedAt)
    }

    @Test
    fun uriIsStoredAsTheExactOriginalString() {
        val raw = "content://com.android.providers.media.documents/document/audio%3A123"
        val entity = EpisodeEntity.from(Episode(id = "x", title = "t", localUri = Uri.parse(raw)))
        assertEquals(raw, entity.localUri)
    }

    @Test
    fun hasLocalSourceFollowsLocalUri() {
        assertTrue(fullEpisode.hasLocalSource)
        assertFalse(fullEpisode.copy(localUri = null).hasLocalSource)
    }

    @Test
    fun progressFractionIsZeroWithoutKnownDuration() {
        assertEquals(0f, Episode(id = "a", title = "t", durationMs = 0L, lastPositionMs = 5_000L).progressFraction, 0.0001f)
    }

    @Test
    fun progressFractionIsProportionalAndClamped() {
        fun fraction(duration: Long, position: Long) =
            Episode(id = "a", title = "t", durationMs = duration, lastPositionMs = position).progressFraction

        assertEquals(0.5f, fraction(100_000L, 50_000L), 0.0001f)
        assertEquals(1f, fraction(100_000L, 250_000L), 0.0001f)
        assertEquals(0f, fraction(100_000L, -10L), 0.0001f)
    }
}
