package jp.ginyolith.kamen_rider_matome.data

import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import okhttp3.*
import org.jsoup.Jsoup
import java.io.IOException
import java.io.StringReader

object SingletonOkHttpClient {
    val okHttpClient = OkHttpClient()
}

class HttpAccess {
    fun getRSSFeed(url : String, callback : Callback){
        val req = Request.Builder()
                .url(url)
                .build()
        SingletonOkHttpClient.okHttpClient.newCall(req).enqueue(callback)

    }

    fun getBlogInfoFromRSSFeed(url : String,
                               onFailure : (Call?, IOException?) -> Unit,
                               onResponse : (Call?, Response?, Blog?) -> Unit) {
        this.getFeed(url, onFailure) {call, response, feed ->
            onResponse(call, response, Blog.fromFeed(requireNotNull(feed)))
        }
    }


    private fun getFeed(url : String,
                onFailure : (Call?, IOException?) -> Unit,
                onResponse : (Call?, Response?, SyndFeed?) -> Unit) {

        this.getRSSFeed(url, object : Callback {
            override fun onFailure(call: Call?, e: IOException?) {
                onFailure(call, e)
            }

            override fun onResponse(call: Call?, response: Response?) {
                val feed = SyndFeedInput().build(StringReader(response?.body()?.source()?.readUtf8()))
                onResponse(call, response, feed)
            }
        })


    }

    fun getArticles(url : String,
                    onFailure : (Call?, IOException?) -> Unit,
                    onResponse : (Call?, Response?, List<Article>?) -> Unit
        ) {

        fun getThumbnailUrl(entry: SyndEntry, blog: Blog): String? {
            val findFirstImgTagSrc = { html : String ->
                Jsoup.parse(html).getElementsByTag("img").firstOrNull()?.attr("src")
            }

            return when(blog.enum) {
                Blog.Enum.JIHOU,
                Blog.Enum.TOKUSATSU_MATOME,
                Blog.Enum.HENSHIN_SOKUHOU  -> findFirstImgTagSrc(entry.contents[0].value)
                Blog.Enum.MATOME_2GOU -> findFirstImgTagSrc(entry.description.value)
            }
        }

        this.getFeed(url, onFailure) { call, response, feed ->
            val blog = Blog.fromFeed(requireNotNull(feed))

            val articles = feed?.entries?.map {
                Article(0, blog._id, it.publishedDate, it.title, it.link, getThumbnailUrl(it, blog))
            }?.toList()

            onResponse(call, response, articles)
        }

    }
}