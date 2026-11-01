package com.auralis.app.data.download

/**
 * Stable key for one remote episode download.
 *
 * A null source is reserved for an explicitly legacy/ambiguous remoteId-only
 * identity; it must never be inferred to belong to a known source.
 */
data class DownloadIdentity(
    val sourceId: String?,
    val remoteId: String,
) {
    init {
        require(remoteId.isNotBlank()) { "remoteId must not be blank" }
        require(sourceId == null || sourceId.isNotBlank()) { "sourceId must be null or non-blank" }
    }

    val isSourceAware: Boolean
        get() = sourceId != null
}
