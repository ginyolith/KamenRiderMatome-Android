package jp.ginyolith.kamen_rider_matome.data

import com.rometools.rome.feed.synd.SyndFeed
import java.util.*

data class Article(
        val blog : Blog,
        val pubDate : Date,
        val title : String,
        val url : String,
        val thumbnailUrl : String?
)

data class Blog(
        val enum : Enum,
        val description : String
) {
    companion object {
        fun fromFeed(feed : SyndFeed) : Blog {
            return Blog(
                    Blog.Enum.fromUrl(feed.link)!!,
                    feed.description
            )
        }
    }

    enum class Enum(val blogName : String, val url : String, val feedUrl : String) {
        MATOME_2GOU("仮面ライダーまとめ２号", "http://kamenrider2.net",  "http://kamenrider2.net/feed")
        ,HENSHIN_SOKUHOU("変身速報","http://www.henshin-hero.com", "http://www.henshin-hero.com/index.rdf")
        ,TOKUSATSU_MATOME("特撮まとめちゃんねる", "http://maskrider-futaba.info", "http://maskrider-futaba.info/feed/")
        ,JIHOU("仮面ライダー遅報","http://www.kr753.com", "http://www.kr753.com/feed");

        companion object  {
            fun fromUrl(url : String): Blog.Enum? = Blog.Enum.values().firstOrNull { it.url == url }
        }
    }
}