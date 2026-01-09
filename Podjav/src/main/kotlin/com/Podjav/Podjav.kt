package com.Podjav
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SearchResponseList
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.VPNStatus
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.fixUrlNull
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newSearchResponseList
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element


class Podjav : MainAPI() {
override var mainUrl              = "https://podjav.tv"
override var name                 = "POD JAV"
override val hasMainPage          = true
override var lang                 = "id"  // Indonesian subs focus
override val hasDownloadSupport   = true
override val hasChromecastSupport = true
override val supportedTypes       = setOf(TvType.NSFW)
override val vpnStatus            = VPNStatus.MightBeNeeded
override val mainPage = mainPageOf(
"/movies/" to "All Movies",
"/genre/abuse/" to "Abuse",
"/genre/big-tits/" to "Big Tits",
"/genre/bride/" to "Bride",
"/genre/creampie/" to "Creampie",
"/genre/cuckold/" to "Cuckold",
"/genre/step-mother/" to "Step Mother"
// Add more genres as needed
)
override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
val url = if (page == 1) "$mainUrl${request.data}" else "$mainUrl${request.data}page/$page/"
val document = app.get(url).document
val responseList = document.select("a").filter { it.attr("href").startsWith("$mainUrl/movies/") }.mapNotNull { it.toSearchResult() }
return newHomePageResponse(HomePageList(request.name, responseList, isHorizontalImages = false), hasNext = document.select("a[href*=page/]").isNotEmpty())
}
private fun Element.toSearchResult(): SearchResponse? {
val title = this.text().trim()
if (title.isEmpty()) return null
val href = fixUrlNull(this.attr("href")) ?: return null
val posterUrl = null  // No posters found in scraping
return newMovieSearchResponse(title, href, TvType.NSFW) {
this.posterUrl = posterUrl
}
}
override suspend fun search(query: String, page: Int): SearchResponseList? {
val document = app.get("$mainUrl/?s=$query").document  // No pagination observed, but if exists, add page
val results = document.select("a").filter { it.attr("href").startsWith("$mainUrl/movies/") }.mapNotNull { it.toSearchResult() }
val hasNext = false  // No pagination in search results from scraping
return if (results.isEmpty()) null else newSearchResponseList(results, hasNext)
}
override suspend fun load(url: String): LoadResponse {
val document = app.get(url).document
val title = document.selectFirst("h1, meta[property=og:title]")?.text()?.trim() ?: ""
val poster = fixUrlNull(document.selectFirst("img, [property='og:image']")?.attr("src"))
val description = document.selectFirst("p, meta[property=og:description]")?.text()?.trim()
return newMovieLoadResponse(title, url, TvType.NSFW, url) {
this.posterUrl = poster
this.plot = description
}
}
override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
val doc = app.get(data).document
val downloadUrl: String = doc.select("a").firstOrNull { it.attr("href").startsWith("https://cdn.podjav.tv/download/") }?.attr("href") ?: return false
callback.invoke(
newExtractorLink(
this.name,
"Download (ZIP containing MP4)",
downloadUrl,
ExtractorLinkType.VIDEO
) {
this.referer = mainUrl
this.quality = Qualities.P720.value
}
)
// If there are embeds, load them (but none found)
// loadExtractor(..., subtitleCallback, callback)
// No external subtitles found, subs are likely integrated in the video
return true
}
}
