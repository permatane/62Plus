package com.Kazefuri

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Kazefuri : MainAPI() {
    override var mainUrl = "https://sv3.kazefuri.cloud"
    override var name = "Kazefuri"
    override val supportedTypes = setOf(TvType.Anime)
    
    // Simplifikasi dulu, fokus bisa build
    override suspend fun search(query: String): List<SearchResponse> {
        return listOf(
            newAnimeSearchResponse("Kazefuri Test", "$mainUrl/test")
        )
    }
    
    override suspend fun load(url: String): LoadResponse {
        return newAnimeLoadResponse("Kazefuri", url, TvType.Anime) {
            addEpisodes(DubStatus.Subbed, listOf(
                // ✅ Gunakan newEpisode()
                newEpisode(url) {
                    this.name = "Episode 1"
                    this.episode = 1
                }
            ))
        }
    }
    
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        // ✅ Gunakan newExtractorLink()
        callback(
            newExtractorLink(
                name,
                "Test",
                data,
                mainUrl,
                Qualities.Unknown.value
            )
        )
        return true
    }
}
