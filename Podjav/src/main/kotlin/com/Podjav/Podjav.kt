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

    override val mainPage = mainPageOf(
        "/movies" to "Terbaru",
        "/genre/big-tits" to "Tobrut",
        "/genre/orgasm" to "Orgame"
    )

    // ================= MAIN PAGE =================

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val pageUrl = if (page == 1) {
            "$mainUrl${request.data}"
        } else {
            "$mainUrl${request.data}/page/$page"
        }

        val document = app.get(pageUrl).document

        val items = document.select("article")
            .mapNotNull { it.toSearchResult() }
            .filter { it.posterUrl != null && it.name.isNotBlank() }

        return newHomePageResponse(
            HomePageList(
                name = request.name,
                list = items,
                isHorizontalImages = false
            ),
            hasNext = items.isNotEmpty()
        )
    }

    // ================= SEARCH ITEM =================

    private fun Element.toSearchResult(): SearchResponse? {

        val anchor = selectFirst("a[href]") ?: return null

        // JUDUL: ambil dari heading, BUKAN title attr
        val title =
            selectFirst("h1, h2, h3")?.text()?.trim()
                ?: anchor.text().trim()

        val href = fixUrl(anchor.attr("href"))

        val img = selectFirst("img")

        val poster = fixUrlNull(
            img?.attr("data-src")?.ifBlank { null }
                ?: img?.attr("data-lazy-src")?.ifBlank { null }
                ?: img?.attr("srcset")
                    ?.split(",")
                    ?.firstOrNull()
                    ?.substringBefore(" ")
                ?: img?.attr("src")
        )

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            posterUrl = poster
        }
    }

    // ================= SEARCH =================

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document

        return document.select("article")
            .mapNotNull { it.toSearchResult() }
    }

    // ================= LOAD DETAIL =================

    override suspend fun load(url: String): LoadResponse {

        val document = app.get(url).document

        val title = document.selectFirst("h1.entry-title, h1")
            ?.text()?.trim()
            ?: document.selectFirst("meta[property=og:title]")
                ?.attr("content")
            ?: "Podjav Video"

        val poster = fixUrlNull(
            document.selectFirst("meta[property=og:image]")
                ?.attr("content")
        )

        val description = document.selectFirst("meta[property=og:description]")
            ?.attr("content")

        return newMovieLoadResponse(
            name = title,
            url = url,
            type = TvType.NSFW,
            data = url
        ) {
            this.posterUrl = poster
            this.plot = description
        }
    }

    // ================= LOAD LINKS =================

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val document = app.get(data).document

        // 1️⃣ iframe langsung
        document.select("iframe[src]").forEach {
            fixUrlNull(it.attr("src"))?.let { iframe ->
                loadExtractor(iframe, subtitleCallback, callback)
            }
        }

        // 2️⃣ cari URL video di script (fallback)
        val scriptData = document.select("script").joinToString("\n") { it.data() }

        Regex("""https?:\/\/[^\s'"]+""")
            .findAll(scriptData)
            .map { it.value }
            .filter { it.contains("embed") || it.contains("player") }
            .forEach {
                loadExtractor(it, subtitleCallback, callback)
            }

        return true
    }
}