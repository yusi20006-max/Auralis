package com.auralis.app.playback

import androidx.media3.common.PlaybackException

/**
 * Issue #12 — map Media3 [PlaybackException] codes to short Persian UI messages.
 * Pure / JVM-testable (no Android Context).
 */
object PlaybackErrorMapper {

    fun userMessage(error: PlaybackException): String =
        userMessage(error.errorCode, error.message)

    fun userMessage(errorCode: Int, technicalMessage: String? = null): String {
        return when (errorCode) {
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
            -> "فایل در دسترس نیست. ممکن است حذف شده یا دسترسی آن لغو شده باشد."

            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            -> "اتصال شبکه برقرار نشد. اینترنت را بررسی کنید."

            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            -> "سرور فایل را درست تحویل نداد. بعداً دوباره تلاش کنید."

            PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED,
            -> "پخش از آدرس ناامن (http) مجاز نیست."

            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
            -> "فرمت فایل پشتیبانی نمی‌شود یا فایل آسیب دیده است."

            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            -> "رمزگشایی صدا ممکن نشد. فرمت دیگری را امتحان کنید."

            PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED,
            -> "خروجی صدا در دسترس نیست."

            PlaybackException.ERROR_CODE_TIMEOUT,
            -> "پخش زمان‌بر شد و متوقف گردید."

            PlaybackException.ERROR_CODE_UNSPECIFIED,
            -> technicalMessage?.takeIf { it.isNotBlank() }?.let { "پخش متوقف شد: $it" }
                ?: "پخش به‌خاطر خطای ناشناخته متوقف شد."

            else -> technicalMessage?.takeIf { it.isNotBlank() }?.let { "پخش متوقف شد: $it" }
                ?: "پخش متوقف شد."
        }
    }
}
