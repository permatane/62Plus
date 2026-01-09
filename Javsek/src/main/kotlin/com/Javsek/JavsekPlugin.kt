package com.Javsek

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context
import com.lagradost.cloudstream3.extractors.StreamTape
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.fixUrlNull
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.ExtractorLink

@CloudstreamPlugin
class JavsekPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Javsek())
        registerExtractorAPI(Earnvid())
        registerExtractorAPI(Earnvids())
        registerExtractorAPI(Earnvida())
        registerExtractorAPI(Luluvideo())
        registerExtractorAPI(Lulustream())
        
    }
}
