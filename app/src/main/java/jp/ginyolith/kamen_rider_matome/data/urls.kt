package jp.ginyolith.kamen_rider_matome.data

import android.provider.DocumentsContract
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

    private fun getFeed(url : String) : SyndFeed
            = SyndFeedInput().build(StringReader(getRSSFeed(url)))

    fun getArticles(url : String): List<Article> {
        val feed = getFeed(url)
        val blog = Blog.fromFeed(feed)

        fun getThumbnailUrl(entry: SyndEntry): String? {
            val doc = Jsoup.parse(entry.contents[0].value)
            return doc.getElementsByTag("img").firstOrNull()?.attr("src")
        }

        return feed.entries.map {
            Article(blog, it.publishedDate, it.title, it.link, getThumbnailUrl(it))
        }.toList()

    }
    fun getLatestArticle(url : String): Article? {
        return getArticles(url).firstOrNull( )
    }
}