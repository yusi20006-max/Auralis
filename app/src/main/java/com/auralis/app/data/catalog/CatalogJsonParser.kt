package com.auralis.app.data.catalog

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Strictly parses the public catalog document into domain models.
 *
 * Malformed individual entries are skipped so one bad row does not discard
 * otherwise usable catalog data. Invalid document structure still fails the
 * parse and is surfaced by CatalogRepository as a load error.
 */
object CatalogJsonParser {

    data class ParsedCatalog(
        val podcasts: List<CatalogPodcast>,
        val episodes: List<CatalogEpisode>,
    )

    fun parse(jsonText: String): ParsedCatalog {
        val root = JSONObject(jsonText)
        val podcasts = readArray(root, "podcasts")?.let(::parsePodcasts).orEmpty()
        val episodes = readArray(root, "episodes")?.let(::parseEpisodes).orEmpty()
        return ParsedCatalog(podcasts = podcasts, episodes = episodes)
    }

    private fun readArray(root: JSONObject, key: String): JSONArray? {
        if (!root.has(key) || root.isNull(key)) return null
        return root.optJSONArray(key)
            ?: throw JSONException("Catalog field '$key' must be an array")
    }

    private fun parsePodcasts(array: JSONArray): List<CatalogPodcast> {
        val out = ArrayList<CatalogPodcast>(array.length())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val parsed = runCatching {
                val id = item.stringValue("id").trim()
                val title = item.stringValue("title").trim()
                if (id.isEmpty() || title.isEmpty()) return@runCatching null

                val rawCover = item.optionalString("coverUrl")
                CatalogPodcast(
                    id = id,
                    title = title,
                    description = item.optionalString("description") ?: "",
                    coverUrl = CatalogConfig.resolveArtworkUrl(rawCover),
                    isAiGenerated = item.optionalBoolean("isAiGenerated") ?: true,
                    episodeCount = item.optionalNonNegativeInt("episodeCount") ?: 0,
                    category = item.optionalString("category")?.ifBlank { "همه" } ?: "همه",
                )
            }.getOrNull()
            if (parsed != null) out.add(parsed)
        }
        return out
    }

    private fun parseEpisodes(array: JSONArray): List<CatalogEpisode> {
        val out = ArrayList<CatalogEpisode>(array.length())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val parsed = runCatching {
                val id = item.stringValue("id").trim()
                val podcastId = item.stringValue("podcastId").trim()
                val title = item.stringValue("title").trim()
                val audioUrl = CatalogConfig.resolveAudioUrl(item.stringValue("audioUrl"))
                if (id.isEmpty() || podcastId.isEmpty() || title.isEmpty() || audioUrl.isEmpty()) {
                    return@runCatching null
                }

                CatalogEpisode(
                    id = id,
                    podcastId = podcastId,
                    title = title,
                    description = item.optionalString("description") ?: "",
                    audioUrl = audioUrl,
                    durationMs = item.optionalNonNegativeLong("durationMs") ?: 0L,
                    publishedAtEpochMs = item.optionalNonNegativeLong("publishedAtEpochMs") ?: 0L,
                    isAiGenerated = item.optionalBoolean("isAiGenerated") ?: true,
                )
            }.getOrNull()
            if (parsed != null) out.add(parsed)
        }
        return out
    }

    private fun JSONObject.stringValue(key: String): String {
        val value = opt(key)
        if (value !is String) throw JSONException("Catalog field '$key' must be a string")
        return value
    }

    private fun JSONObject.optionalString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val value = opt(key)
        if (value !is String) throw JSONException("Catalog field '$key' must be a string or null")
        return value
    }

    private fun JSONObject.optionalBoolean(key: String): Boolean? {
        if (!has(key) || isNull(key)) return null
        val value = opt(key)
        if (value !is Boolean) throw JSONException("Catalog field '$key' must be a boolean or null")
        return value
    }

    private fun JSONObject.optionalNonNegativeInt(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        val value = opt(key)
        if (value !is Number) throw JSONException("Catalog field '$key' must be a number or null")
        val number = value.toDouble()
        if (!number.isFinite() || number < 0 || number % 1.0 != 0.0 || number > Int.MAX_VALUE) {
            throw JSONException("Catalog field '$key' must be a non-negative integer")
        }
        return number.toInt()
    }

    private fun JSONObject.optionalNonNegativeLong(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        val value = opt(key)
        if (value !is Number) throw JSONException("Catalog field '$key' must be a number or null")
        val number = value.toDouble()
        if (!number.isFinite() || number < 0 || number % 1.0 != 0.0 || number > Long.MAX_VALUE.toDouble()) {
            throw JSONException("Catalog field '$key' must be a non-negative integer")
        }
        return number.toLong()
    }
}
