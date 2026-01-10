package com.Javstory

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class Javstory : MainAPI() {
    override var mainUrl = "https://javstory1.com"
    override var name = "JavStory"
    override val supportedTypes = setOf(TvType.NSFW)
    override var lang = "id"
    override val hasMainPage = true
    
    private val BROWSER_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    override val mainPage = mainPageOf(
        "/" to "Terbaru",
        "/category/indosub/" to "Sub Indonesia",
        "/category/engsub/" to "Sub English"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) "$mainUrl${request.data}" else "$mainUrl${request.data}page/$page/"
        
        // Menggunakan app.get dengan timeout dan headers yang lebih lengkap
        val response = app.get(url, timeout = 15).document
        
        // Selector 'article' biasanya digunakan di tema WordPress JavStory
        // Jika masih kosong, coba ganti ke ".post-item" atau "div.bs"
        val items = response.select("article, .post-item, .bs").mapNotNull {
            it.toSearchResult()
        }
        
        return newHomePageResponse(request.name, items, hasNext = true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        // Mencari link dan judul dengan selector yang lebih luas
        val linkElement = this.selectFirst(".entry-title a, a.tip") ?: return null
        val title = linkElement.text().trim()
        val href = fixUrl(linkElement.attr("href"))
        
        // Mencari gambar (poster)
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src") 
            ?: this.selectFirst("img")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        return document.select("article, .post-item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        
        // Mencari data detail
        val title = document.selectFirst("h1.entry-title, .entry-title")?.text() ?: return null
        val poster = fixUrlNull(document.selectFirst(".content-thumb img, .thumb img")?.attr("src"))
        val description = document.selectFirst(".entry-content p, .description")?.text()

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document

        // 1. Selector Iframe Player
        document.select("iframe.player-iframe, .player-iframe iframe").forEach { iframe ->
            val src = fixUrl(iframe.attr("src"))
            loadExtractor(src, data, subtitleCallback, callback)
        }

        // 2. Logic server-button onclick (loadStream)
        document.select("button.server-button").forEach { button ->
            val onClick = button.attr("onclick")
            if (onClick.contains("loadStream")) {
                val matches = """'([^']*)'""".toRegex().findAll(onClick).map { it.groupValues[1] }.toList()
                if (matches.size >= 2) {
                    val baseUrl = matches[0]
                    val id = matches[1]
                    val fullUrl = if (baseUrl.endsWith("/")) "$baseUrl$id" else "$baseUrl/$id"
                    loadExtractor(fullUrl, data, subtitleCallback, callback)
                }
            }
        }

        // 3. Logic rndmzr (Reverse string ID)
        document.select("script").forEach { script ->
            val scriptData = script.data()
            if (scriptData.contains("rndmzr")) {
                val regex = """"(.*?)".rndmzr\(\)""".toRegex()
                regex.findAll(scriptData).forEach { match ->
                    val realId = match.groupValues[1].reversed()
                    when {
                        scriptData.contains("streamtape") -> 
                            loadExtractor("https://streamtape.com/e/$realId", data, subtitleCallback, callback)
                        scriptData.contains("sbembed") || scriptData.contains("sbvideo") || scriptData.contains("sbplay") -> 
                            loadExtractor("https://sbembed2.com/e/$realId.html", data, subtitleCallback, callback)
                    }
                }
            }
        }
        return true
    }
}
