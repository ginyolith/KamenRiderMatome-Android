package jp.ginyolith.kamen_rider_matome.data.article

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import com.rometools.rome.feed.synd.SyndEntry
import jp.ginyolith.kamen_rider_matome.data.blog.Blog
import org.jsoup.Jsoup
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

@Entity
data class Article(
        @PrimaryKey(autoGenerate = true)
        val _id : Long = 0,
        @ForeignKey(
                entity = Blog::class,
                parentColumns = ["_id"],
                childColumns = ["blogId"]
        ) var blogId : Long,
        val pubDate : Date,
        val title : String,
        val url : String,
        val thumbnailUrl : String?
) : Serializable {

    @Ignore
    lateinit var blog : Blog

    object Singleton {
        val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPAN)
    }

    fun getFormattedPubDate() : String = Singleton.simpleDateFormat.format(pubDate)
    fun isSameBlog(url: String): Boolean = url.startsWith(this.blog.enum.url)
    fun isNewArticle() : Boolean = pubDate > blog.lastUpdateDate || !blog.initialized


    companion object {
        fun fromFeedEntry(entry: SyndEntry, blog : Blog): Article {
            return Article(
                    _id = 0,
                    blogId = blog._id,
                    pubDate = entry.publishedDate,
                    title = entry.title,
                    url = entry.link,
                    thumbnailUrl = getThumbnailUrl(entry, blog))
                    .apply { this.blog = blog }
        }

        private fun getThumbnailUrl(entry: SyndEntry, blog: Blog): String? {
            val findFirstImgTagSrc = { html: String ->
                Jsoup.parse(html).getElementsByTag("img").firstOrNull()?.attr("src")
            }

            return when (blog.enum) {
                Blog.Enum.JIHOU,
                Blog.Enum.TOKUSATSU_MATOME,
                Blog.Enum.HENSHIN_SOKUHOU -> findFirstImgTagSrc(entry.contents[0].value)
                Blog.Enum.MATOME_2GOU -> findFirstImgTagSrc(entry.description.value)
            }
        }
    }
}