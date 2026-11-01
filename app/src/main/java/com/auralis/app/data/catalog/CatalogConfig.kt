package com.auralis.app.data.catalog

/**
 * When you have a host, set [baseUrl] (e.g. https://cdn.auralis.example)
 * and point catalog episode [CatalogEpisode.audioUrl] to full paths or
 * relative paths resolved with [resolveAudioUrl].
 *
 * Until then, sample HTTPS URLs in [CatalogRepository] are used as-is.
 *
 * [SOURCE_ID] is the stable remote-source identity for the hosted Auralis
 * catalog. It forms the composite Episode remote identity together with
 * [CatalogEpisode.id] as remoteId. Do not reuse this value for RSS or
 * other independent sources.
 */
object CatalogConfig {
    /** Stable source identity for the hosted Auralis catalog. */
    const val SOURCE_ID: String = "catalog:auralis"

    /** Empty = use absolute URLs from the catalog sample / API. */
    @Volatile
    var baseUrl: String = ""

    fun resolveAudioUrl(pathOrUrl: String): String {
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            return pathOrUrl
        }
        val base = baseUrl.trimEnd('/')
        if (base.isEmpty()) return pathOrUrl
        val path = pathOrUrl.trimStart('/')
        return "$base/$path"
    }
}
