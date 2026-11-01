package com.auralis.app.data.catalog

import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses the hosted catalog JSON into domain models.
 * Keeps a zero-dependency JSON path (org.json is on the Android platform).
 */
object CatalogJsonParser {

    data class ParsedCatalog(
        val podcasts: List<CatalogPodcast>,
        val episodes: List<CatalogEpisode>,
    )

    fun parse(jsonText: String): ParsedCatalog {
        val root = JSONObject(jsonText)
        val podcasts = parsePodcasts(root.optJSONArray("podcasts"))
        val episodes = parseEpisodes(root.optJSONArray("episodes"))
        return ParsedCatalog(podcasts = podcasts, episodes = episodes)
    }

    private fun parsePodcasts(array: JSONArray?): List<CatalogPodcast> {
        if (array == null) return emptyList()
        val out = ArrayList<CatalogPodcast>(array.length())
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val id = o.optString("id").trim()
            val title = o.optString("title").trim()
            if (id.isEmpty() || title.isEmpty()) continue
            out.add(
                CatalogPodcast(
                    id = id,
                    title = title,
                    description = o.optString("description", ""),
                    coverUrl = o.optString("coverUrl").takeIf { it.isNotBlank() && it != "null" },
                    isAiGenerated = o.optBoolean("isAiGenerated", true),
                    episodeCount = o.optInt("episodeCount", 0),
                    category = o.optString("category", "همه").ifBlank { "همه" },
                ),
            )
        }
        return out
    }

    private fun parseEpisodes(array: JSONArray?): List<CatalogEpisode> {
        if (array == null) return emptyList()
        val out = ArrayList<CatalogEpisode>(array.length())
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val id = o.optString("id").trim()
            val podcastId = o.optString("podcastId").trim()
            val title = o.optString("title").trim()
            val audioUrl = CatalogConfig.resolveAudioUrl(o.optString("audioUrl").trim())
            if (id.isEmpty() || podcastId.isEmpty() || title.isEmpty() || audioUrl.isEmpty()) continue
            out.add(
                CatalogEpisode(
                    id = id,
                    podcastId = podcastId,
                    title = title,
                    description = o.optString("description", ""),
                    audioUrl = audioUrl,
                    durationMs = o.optLong("durationMs", 0L),
                    publishedAtEpochMs = o.optLong("publishedAtEpochMs", 0L),
                    isAiGenerated = o.optBoolean("isAiGenerated", true),
                ),
            )
        }
        return out
    }
}
