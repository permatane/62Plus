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
        "/category/engsub/" to "Sub English",
    )

   override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) "$mainUrl${request.data}" else "$mainUrl${request.data}page/$page/"
        val response = app.get(url, timeout = 15).document
        val homePageLists = mutableListOf<HomePageList>()

        // --- BARIS 1: REKOMENDASI UTAMA (SLIDER) ---
        if (request.data == "/" && page <= 1) {
            // Selector ini menargetkan slider di bagian atas (biasanya class 'featured' atau 'slider')
            val featured = response.select(".featured-item, .slider-item, .hero-content").mapNotNull {
                it.toSearchResult()
            }
            if (featured.isNotEmpty()) {
                homePageLists.add(
                    HomePageList("Rekomendasi Utama", featured, isHorizontalImages = true)
                )
            }
        }

        // --- BARIS 2: DAFTAR FILM STANDAR ---
        val items = response.select("article, .post-item, .bs").mapNotNull {
            it.toSearchResult()
        }
        
        homePageLists.add(
            HomePageList(request.name, items, isHorizontalImages = false)
        )

        return newHomePageResponse(homePageLists, hasNext = true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val linkElement = this.selectFirst(".entry-title a, a.tip, .title a") ?: return null
        val title = linkElement.text().trim()
        val href = fixUrl(linkElement.attr("href"))
        
        // Menangani Lazy Load Poster
        val img = this.selectFirst("img")
        val posterUrl = fixUrlNull(
            img?.attr("data-src") ?: 
            img?.attr("data-lazy-src") ?: 
            img?.attr("src")
        )

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
        val response = app.get(url, referer = mainUrl).document
        
        val title = response.selectFirst("h1.entry-title, .entry-title")?.text()?.trim() 
            ?: throw ErrorLoadingException("Gagal memuat judul")

        val img = response.selectFirst(".content-thumb img, .thumb img, .entry-content img")
        val poster = fixUrlNull(img?.attr("data-src") ?: img?.attr("src"))
        
        val description = response.selectFirst(".entry-content p, .description")?.text()?.trim()
        val tags = response.select(".genredown a, .entry-categories a").map { it.text() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val response = app.get(data).document

        // 1. Ambil Iframe Langsung (Biasanya Streamtape)
        response.select("iframe.player-iframe, .iframe-container iframe").forEach { iframe ->
            val src = fixUrl(iframe.attr("src"))
            loadExtractor(src, data, subtitleCallback, callback)
        }

        // 2. Dekripsi Fungsi rndmzr() di dalam Script (Reverse ID)
        response.select("script").forEach { script ->
            val text = script.data()
            if (text.contains("rndmzr")) {
                val regex = """"(.*?)".rndmzr\(\)""".toRegex()
                regex.findAll(text).forEach { match ->
                    val realId = match.groupValues[1].reversed()
                    val host = if (text.contains("streamtape")) "https://streamtape.com/e/" 
                              else if (text.contains("sbembed") || text.contains("sbvideo")) "https://sbplay2.com/e/"
                              else ""
                    
                    if (host.isNotEmpty()) {
                        loadExtractor("$host$realId", data, subtitleCallback, callback)
                    }
                }
            }
        }

        // 3. Tangkap Link dari Server Buttons
        response.select("button.server-button").forEach { btn ->
            val onclick = btn.attr("onclick")
            if (onclick.contains("loadStream")) {
                val matches = """'([^']*)'""".toRegex().findAll(onclick).map { it.groupValues[1] }.toList()
                if (matches.size >= 2) {
                    val finalUrl = if (matches[0].endsWith("/")) "${matches[0]}${matches[1]}" else "${matches[0]}/${matches[1]}"
                    loadExtractor(fixUrl(finalUrl), data, subtitleCallback, callback)
                }
            }
        }

        return true
    }
}
