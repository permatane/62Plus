package com.Anichin

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Auratail : Anichin() {
    override var mainUrl              = "https://auratail.vip/"
    override var name                 = "Auratail"
    
    override val mainPage = mainPageOf(
        "anime/?status=&type=&order=update" to "Update Terbaru",
    )
}
