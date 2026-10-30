package com.auralis.app.playback

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackErrorMapperTest {

    @Test
    fun fileNotFoundMessageIsPersianAndSpecific() {
        val msg = PlaybackErrorMapper.userMessage(
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
        )
        assertTrue(msg.contains("فایل"))
        assertFalse(msg.contains("ERROR_CODE"))
    }

    @Test
    fun networkTimeoutMessageMentionsNetwork() {
        val msg = PlaybackErrorMapper.userMessage(
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        )
        assertTrue(msg.contains("شبکه") || msg.contains("اینترنت"))
    }

    @Test
    fun decoderFailureMessageMentionsDecode() {
        val msg = PlaybackErrorMapper.userMessage(
            PlaybackException.ERROR_CODE_DECODING_FAILED,
        )
        assertTrue(msg.contains("رمزگشایی") || msg.contains("فرمت"))
    }

    @Test
    fun unsupportedContainerMessage() {
        val msg = PlaybackErrorMapper.userMessage(
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        )
        assertTrue(msg.contains("فرمت") || msg.contains("پشتیبانی"))
    }

    @Test
    fun unspecifiedFallsBackToTechnicalMessage() {
        val msg = PlaybackErrorMapper.userMessage(
            PlaybackException.ERROR_CODE_UNSPECIFIED,
            "detail-xyz",
        )
        assertTrue(msg.contains("detail-xyz"))
    }

    @Test
    fun unknownCodeStillReturnsNonEmptyPersianSafeString() {
        val msg = PlaybackErrorMapper.userMessage(errorCode = -99999, technicalMessage = null)
        assertTrue(msg.isNotBlank())
        assertFalse(msg.contains("-99999"))
    }
}
