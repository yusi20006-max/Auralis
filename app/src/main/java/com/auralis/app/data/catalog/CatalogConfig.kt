package com.auralis.app.data.catalog

/**
 * When you have a host, set [baseUrl] (e.g. https://cdn.auralis.example)
 * and point catalog episode [CatalogEpisode.audioUrl] to full paths or
 * relative paths resolved with [resolveAudioUrl].
 *
 * Until then, sample HTTPS URLs in [CatalogRepository] are used as-is.
 */
object CatalogConfig {
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
