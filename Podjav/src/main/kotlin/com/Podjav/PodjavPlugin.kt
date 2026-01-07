package com.Podjav

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class PodjavPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Podjav())
    }
}
