package com.lagradost

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class ImdbPlugin: Plugin() {
    override fun load(context: Context) {
        // Bu satır, yazdığın Provider'ı CloudStream'e kaydeder
        registerMainAPI(ImdbAutoEmbedProvider())
    }
}
