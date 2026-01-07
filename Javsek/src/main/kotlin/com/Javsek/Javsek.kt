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
        val url = if (page <= 1) "$mainUrl/${request.data.replace("page/%d/", "")}" 
                  else "$mainUrl/${request.data.format(page)}"
        
        val document = app.get(url).document
        val home = document.select("article").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, home)
    }


}
