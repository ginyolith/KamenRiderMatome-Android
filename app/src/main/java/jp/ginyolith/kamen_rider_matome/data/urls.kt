package jp.ginyolith.kamen_rider_matome.data

import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.StringReader

object SingletonOkHttpClient {
    val okHttpClient = OkHttpClient()
}

class HttpAccess {
    fun getRSSFeed(url : String) : String{
        val req = Request.Builder()
                .url(url)
                .build()
        val result = SingletonOkHttpClient.okHttpClient.newCall(req).execute()

        return result.body()?.source()?.readUtf8() ?: ""
    }

    fun getBlogInfoFromRSSFeed(url : String): Blog
            = Blog.fromFeed(getFeed(url))

    fun getFeed(url : String) : SyndFeed
            = SyndFeedInput().build(StringReader(getRSSFeed(url)))

    fun getArticles(url : String): List<Article> {
        val feed = getFeed(url)
        val blog = Blog.fromFeed(feed)

        fun getThumbnailUrl(entry: SyndEntry, blog: Blog): String? {
            val findFirstImgTagSrc = { html : String ->
                Jsoup.parse(html).getElementsByTag("img").firstOrNull()?.attr("src")
            }

            return when(blog.enum) {
                Blog.Enum.JIHOU,
                Blog.Enum.TOKUSATSU_MATOME,
                Blog.Enum.HENSHIN_SOKUHOU -> findFirstImgTagSrc(entry.contents[0].value)
                Blog.Enum.MATOME_2GOU -> findFirstImgTagSrc(entry.description.value)
            }
        }

        return feed.entries.map {
            Article(0, blog._id, it.publishedDate, it.title, it.link, getThumbnailUrl(it, blog))
        }.toList()

    }
    fun getLatestArticle(url : String): Article? {
        return getArticles(url).firstOrNull( )
    }
}