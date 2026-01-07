package com.Podjav

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Podjav : MainAPI() {

    override var mainUrl = "https://podjav.tv"
    override var name = "Podjav"
    override val hasMainPage = true
    override var lang = "id"
    override val hasQuickSearch = false
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded

    private val BROWSER_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    override val mainPage = mainPageOf(
        "" to "Latest",
        "category/japanese" to "Japanese",
        "category/subtitle" to "Sub Indo"
    )

    private suspend fun getDocument(url: String) =
        app.get(
            url,
            headers = mapOf(
                "User-Agent" to BROWSER_UA,
                "Referer" to mainUrl
            )
        ).document

    /* =========================
       MAIN PAGE
       ========================= */
    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val url = if (page == 1)
            "$mainUrl/${request.data}"
        else
            "$mainUrl/${request.data}/page/$page"

        val document = getDocument(url)

        val items = document
            .select("article")
            .mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            HomePageList(
                request.name,
                items,
                isHorizontalImages = false
            ),
            hasNext = items.isNotEmpty()
        )
    }

    /* =========================
       SEARCH RESULT
       ========================= */
    private fun Element.toSearchResult(): SearchResponse? {
        val title = selectFirst("h2.entry-title a")
            ?: selectFirst("h2 a")
            ?: return null

        val img = selectFirst("img")

        return newMovieSearchResponse(
            title.text().trim(),
            fixUrl(title.attr("href")),
            TvType.NSFW
        ) {
            posterUrl = fixUrlNull(
                img?.attr("data-src")
                    ?.ifBlank { img.attr("src") }
            )
        }
    }

    /* =========================
       SEARCH
       ========================= */
    override suspend fun search(query: String): List<SearchResponse> {
        val document = getDocument("$mainUrl/?s=$query")
        return document
            .select("article")
            .mapNotNull { it.toSearchResult() }
    }

    /* =========================
       LOAD DETAIL (FIXED)
       ========================= */
    override suspend fun load(url: String): LoadResponse {
        val document = getDocument(url)

        // 🔑 JUDUL ASLI PODJAV
        val title = document
            .selectFirst("h1.entry-title")
            ?.text()
            ?: document.selectFirst("title")?.text()
            ?: "Podjav Video"

        // 🔑 POSTER ASLI PODJAV
        val poster = document
            .selectFirst("div.post-thumbnail img")
            ?.attr("src")
            ?: document.selectFirst("meta[property=og:image]")?.attr("content")

        val desc = document
            .selectFirst("div.entry-content p")
            ?.text()
            ?: document.selectFirst("meta[property=og:description]")?.attr("content")

        // Ambil iframe player
        val iframeLinks = document
            .select("iframe[src]")
            .map { fixUrl(it.attr("src")) }
            .distinct()

        return newMovieLoadResponse(
            name = title.trim(),
            url = url,
            type = TvType.NSFW,
            data = iframeLinks.joinToString("||")
        ) {
            posterUrl = fixUrlNull(poster)
            plot = desc
        }
    }

    /* =========================
       LOAD LINKS (EXTRACTOR)
       ========================= */
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val iframeLinks = data.split("||").distinct()
        var found = false

        iframeLinks.forEach { iframe ->
            try {
                loadExtractor(
                    iframe,
                    referer = mainUrl,
                    subtitleCallback = subtitleCallback
                ) { link ->
                    found = true
                    callback(link)
                }
            } catch (_: Exception) {
            }
        }

        return found
    }
}