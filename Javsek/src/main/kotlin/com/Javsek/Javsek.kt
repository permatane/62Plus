package com.Javsek

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class Javsek : MainAPI() {
    override var mainUrl = "https://javsek.net"
    override var name = "JavSek"
    override val hasMainPage = true
    override var lang = "id"
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "page/%d/" to "Terbaru",
        "category/indo-sub/page/%d/" to "Sub Indo",
        "category/english-sub/page/%d/" to "Sub English",
        "category/jav-reducing-mosaic-decensored-streaming-and-download/page/%d/" to "Reducing Mosaic",
        "category/amateur/page/%d/" to "Amateur",
        "category/chinese-porn-streaming/page/%d/" to "China",
        
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) {
            "$mainUrl/${request.data.replace("page/%d/", "")}"
        } else {
            "$mainUrl/${request.data.format(page)}"
        }

        val document = app.get(url).document
        // Menggunakan selektor 'article' yang lebih umum sesuai struktur index.html
        val home = document.select("article").mapNotNull { it.toSearchResult() }
        
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val titleElement = this.selectFirst("h2.entry-title a") ?: return null
        val title = titleElement.text().trim()
        val href = fixUrl(titleElement.attr("href"))
        
        // Perbaikan Poster: Menggunakan getImageAttr untuk menangani Lazy Load
        val posterUrl = this.selectFirst("img")?.getImageAttr()

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val document = app.get(url).document
        return document.select("article").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val content = document.selectFirst("div.entry-content")

        val title = document.selectFirst("h1.entry-title")?.text()?.trim() ?: ""
        // Ambil gambar utama dari dalam konten post
        val poster = content?.selectFirst("img")?.getImageAttr()
        val plot = content?.select("p")?.firstOrNull { it.text().isNotBlank() }?.text()
        
        val tags = document.select("span.tags-links a").eachText()

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = plot
            this.tags = tags
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        
        // 1. Ekstraksi dari semua iframe (Embed Player)
        document.select("iframe").forEach { iframe ->
            val src = iframe.attr("src")
            if (src.isNotEmpty() && !src.contains("about:blank")) {
                loadExtractor(fixUrl(src), data, subtitleCallback, callback)
            }
        }

        // 2. Ekstraksi dari link teks/tombol (Alternative Link)
        // Mencari link yang mengandung provider video populer
        val videoProviders = listOf("dood", "streamwish", "filelions", "vidguard", "voe", "mixdrop")
        document.select("div.entry-content a").forEach { link ->
            val href = link.attr("href")
            if (videoProviders.any { href.contains(it) }) {
                loadExtractor(href, data, subtitleCallback, callback)
            }
        }

        return true
    }

    // Fungsi utilitas untuk menangani Lazy Loading Gambar (diambil dari pola DutaMovie.kt)
    private fun Element.getImageAttr(): String? {
        val url = when {
            this.hasAttr("data-src") -> this.attr("abs:data-src")
            this.hasAttr("data-lazy-src") -> this.attr("abs:data-lazy-src")
            this.hasAttr("srcset") -> this.attr("abs:srcset").substringBefore(" ")
            else -> this.attr("abs:src")
        }
        return url?.ifBlank { null }
    }
}
