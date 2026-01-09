package com.Podjav

import android.util.Base64
import com.lagradost.api.Log
//import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Podjav : MainAPI() {
    override var mainUrl              = "https://podjav.tv"
    override var name                 = "PodJAV"
    override val hasMainPage          = true
    override var lang                 = "id"
    override val hasDownloadSupport   = true
    override val supportedTypes       = setOf(TvType.NSFW)

    /* =========================
       USER AGENT (MANUAL)
       ========================= */
    private val BROWSER_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"


    override val mainPage = mainPageOf(
        "/movies/" to "Update Terbaru",
        "/genre/orgasm/" to "Orgasme",
        "/genre/big-tits/" to "Tobrut",
        "/genre/creampie/" to "Krim Pejuh",
        "/genre/abuse/" to "Pemaksaan",
        "/genre/model/" to "Model Cantik",
        "/genre/mature-woman/" to "Wanita Dewasa",
        "/genre/step-mother/" to "Ibu Angkat",
        "/genre/nurse/" to "Perawat",
        "/genre/secretary/" to "Sekretaris",
        "/genre/female-teacher/" to "Guru",
        "/genre/swingers/" to "Tukar Pasangan",
        "/genre/solowork/" to "Solowork",
        "/genre/cuckold/" to "Istri Menyimpang"

    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page == 1) "$mainUrl${request.data}" else "$mainUrl${request.data}page/$page/"
        val document = app.get(url).document
        val responseList = document.select("article, div.item").mapNotNull { it.toSearchResult() }
        val hasNext = document.select(".pagination .next").isNotEmpty()
        return newHomePageResponse(HomePageList(request.name, responseList, isHorizontalImages = false), hasNext)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val titleEl = selectFirst("h2.entry-title a, h3 a") ?: return null
        val title = titleEl.text().trim()
        if (title.isEmpty()) return null
        val href = fixUrlNull(titleEl.attr("href")) ?: return null
        val posterUrl = selectFirst("img")?.attr("src")?.let { fixUrlNull(it) }
        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList? {
        val url = if (page == 1) "$mainUrl/?s=$query" else "$mainUrl/page/$page/?s=$query"
        val document = app.get(url).document
        val results = document.select("article, div.item").mapNotNull { it.toSearchResult() }
        val hasNext = document.select(".pagination .next").isNotEmpty()
        return if (results.isEmpty()) null else newSearchResponseList(results, hasNext)
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("meta[property=og:title]")
            ?.attr("content")
            ?.trim()
            ?: "Podjav Video"
        val poster = document.selectFirst("meta[property=og:image]")
            ?.attr("content")
        val description = document.selectFirst("meta[property=og:description]")
            ?.attr("content")
        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.tags = document.select("a[rel=tag]").eachText()
        }
    }

override suspend fun loadLinks(
    data: String,
    isCasting: Boolean,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit
): Boolean {
    val javCodeMatch = Regex("/movies/([a-zA-Z0-9-]+)(-sub-indo-.*?)?/?$").find(data)
        ?: return false
    val javCode = javCodeMatch.groupValues[1].uppercase()
    val mp4Url = "https://vod.podjav.tv/$javCode/$javCode.mp4"

    callback(
        newExtractorLink(
            source = this.name,
            name = "Direct MP4 • 1080p",
            url = mp4Url,
            referer = data,
            quality = Qualities.P1080.value,
            isM3u8 = false,
            headers = mapOf(
                "Origin" to "https://podjav.tv",
                "Referer" to data,
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"
            )
        )
    )
// 3. Extra fallback untuk kasus seperti START-440-id.mp4 (Sub Indo version)
    val indoUrl = "https://vod.podjav.tv/$javCode/$javCode-id.mp4"
    if (indoUrl != generatedUrl) {
        callback(
            ExtractorLink(
                source = this.name,
                name = "Direct MP4 • 1080p (Sub Indo Version)",
                url = indoUrl,
                referer = data,
                quality = Qualities.P1080.value,
                isM3u8 = false,
                headers = mapOf(
                    "Origin" to "https://podjav.tv",
                    "Referer" to data,
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                )
            )
        )
    return true
}

}
