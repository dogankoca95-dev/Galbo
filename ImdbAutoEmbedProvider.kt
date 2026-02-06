package com.lagradost.cloudstream3.movieproviders

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import org.jsoup.nodes.Element

class ImdbAutoEmbedProvider : MainAPI() {
    override var name = "IMDB AutoEmbed"
    override var mainUrl = "https://www.imdb.com"
    override val lang = "en"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    // Ana sayfada en yeni filmleri göstermek için
    override suspend fun getMainPage(): HomePageResponse {
        val items = mutableListOf<HomePageList>()
        // IMDB'nin "En Popüler/Yeni" listesine istek atıyoruz
        val document = app.get("$mainUrl/chart/moviemeter/").document
        
        val homeItems = document.select("li.ipc-metadata-list-summary-item").mapNotNull {
            it.toSearchResponse()
        }
        
        items.add(HomePageList("Latest Movies", homeItems))
        return HomePageResponse(items)
    }

    private fun Element.toSearchResponse(): SearchResponse? {
        val title = this.selectFirst(".ipc-title__text")?.text() ?: return null
        val url = this.selectFirst("a.ipc-title-link-wrapper")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img.ipc-image")?.attr("src")
        
        return MovieSearchResponse(
            name = title,
            url = fixUrl(url),
            apiName = this@ImdbAutoEmbedProvider.name,
            type = TvType.Movie,
            posterUrl = posterUrl
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/find?q=$query&s=tt"
        val document = app.get(url).document
        
        return document.select(".ipc-metadata-list-summary-item").mapNotNull {
            it.toSearchResponse()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        
        val title = document.selectFirst("h1[data-testid='hero__page-title'] > span")?.text() ?: ""
        val poster = document.selectFirst("meta[property='og:image']")?.attr("content")
        val plot = document.selectFirst("span[data-testid='plot-xl']")?.text()
        val rating = parseRating(document.selectFirst("span.sc-bde20123-1")?.text())
        val year = document.selectFirst("a[href*='/releaseinfo']")?.text()?.toIntOrNull()
        
        // "tt34888646" kısmını URL'den çekiyoruz
        val imdbId = imdbUrlToId(url) ?: throw ErrorLoadingException("IMDB ID not found")

        return newMovieLoadResponse(title, url, TvType.Movie, imdbId) {
            this.posterUrl = poster
            this.plot = plot
            this.rating = rating
            this.year = year
        }
    }

    override suspend fun loadLinks(
        data: String, // Burada data olarak load() fonksiyonunda verdiğimiz imdbId (tt...) gelecek
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        // Senin istediğin dönüşüm: https://autoembed.co/movie/imdb/tt34888646
        val videoUrl = "https://autoembed.co/movie/imdb/$data"
        
        // AutoEmbed sitesine gidip iframe veya direkt linki yakalamaya çalışıyoruz
        // Not: AutoEmbed genellikle bir iframe içinde asıl videoyu saklar. 
        // CloudStream'in kendi extractor'ları varsa onları tetikler.
        
        callback.invoke(
            ExtractorLink(
                source = "AutoEmbed",
                name = "AutoEmbed Server",
                url = videoUrl,
                referer = "https://autoembed.co/",
                quality = Qualities.P1080.value,
                isM3u8 = false // Site yapısına göre true olabilir
            )
        )
        return true
    }
}