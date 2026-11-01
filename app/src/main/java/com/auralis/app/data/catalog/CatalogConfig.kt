package com.auralis.app.data.catalog

/**
 * Hosted catalog configuration.
 *
 * Default [catalogUrl] points at the JSON file in this repo so the app can
 * load a real HTTP catalog without a separate CDN.
 *
 * Preferred production URL after GitHub Pages is enabled on `/docs`:
 * `https://yusi20006-max.github.io/Auralis/catalog.json`
 *
 * [SOURCE_ID] is the stable remote-source identity for the hosted Auralis
 * catalog. It forms the composite Episode remote identity together with
 * [CatalogEpisode.id] as remoteId.
 */
object CatalogConfig {
    /** Stable source identity for the hosted Auralis catalog. */
    const val SOURCE_ID: String = "catalog:auralis"

    /**
     * HTTP endpoint for the catalog JSON.
     * Override at runtime if needed; empty disables remote fetch.
     */
    @Volatile
    var catalogUrl: String =
        "https://raw.githubusercontent.com/yusi20006-max/Auralis/main/docs/catalog.json"

    /** Optional CDN base for relative audio paths inside the catalog JSON. */
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
