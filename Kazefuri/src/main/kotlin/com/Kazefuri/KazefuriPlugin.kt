package com.Kazefuri

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class KazefuriPlugin: Plugin() {
    // Gunakan Any? agar tidak perlu import android.content.Context
    override fun load(context: Any?) {
        registerMainAPI(Kazefuri())
    }
}
