package com.auralis.app.data.catalog

import java.net.URI

/**
 * Hosted catalog configuration.
 *
 * [SOURCE_ID] is the stable remote-source identity for the hosted Auralis
 * catalog. It forms the composite Episode remote identity together with
 * [CatalogEpisode.id] as remoteId.
 */
object CatalogConfig {
    /** Stable source identity for the hosted Auralis catalog. */
    const val SOURCE_ID: String = "catalog:auralis"

    /** HTTP endpoint for the catalog JSON. */
    @Volatile
    var catalogUrl: String =
        "https://raw.githubusercontent.com/yusi20006-max/Auralis/main/docs/catalog.json"

    /** Optional HTTP(S) base for relative audio and artwork paths. */
    @Volatile
    var baseUrl: String = ""

    /**
     * Resolves an audio URL according to the catalog contract:
     * absolute HTTP(S) URLs pass through; relative paths require a valid HTTP(S)
     * [baseUrl]. Unsupported schemes, malformed URLs and unresolved paths fail
     * closed by returning an empty string.
     */
    fun resolveAudioUrl(pathOrUrl: String): String =
        resolveHttpUrl(pathOrUrl) ?: ""

    /** Resolves optional artwork URLs; invalid or unresolved values become null. */
    fun resolveArtworkUrl(pathOrUrl: String?): String? =
        pathOrUrl?.takeIf { it.isNotBlank() }?.let(::resolveHttpUrl)

    private fun resolveHttpUrl(pathOrUrl: String): String? {
        val value = pathOrUrl.trim()
        if (value.isEmpty()) return null

        return try {
            val candidate = URI(value)
            val resolved = if (candidate.isAbsolute) {
                candidate
            } else {
                val base = baseUrl.trim()
                if (base.isEmpty()) return null
                val baseUri = URI(if (base.endsWith("/")) base else "$base/")
                if (!baseUri.isAbsolute || baseUri.scheme !in HTTP_SCHEMES || baseUri.host.isNullOrBlank()) {
                    return null
                }
                baseUri.resolve(value.trimStart('/'))
            }

            if (resolved.scheme !in HTTP_SCHEMES || resolved.host.isNullOrBlank()) {
                null
            } else {
                resolved.toASCIIString()
            }
        } catch (_: Exception) {
            null
        }
    }

    private val HTTP_SCHEMES = setOf("http", "https")
}
