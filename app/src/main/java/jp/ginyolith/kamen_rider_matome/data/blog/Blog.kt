package jp.ginyolith.kamen_rider_matome.data.blog

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.rometools.rome.feed.synd.SyndFeed
import java.io.Serializable
import java.util.*

@Entity
data class Blog(
        @PrimaryKey(autoGenerate = false)
        var _id : Long
) : Serializable {

    var enum : Enum = Blog.Enum.fromOrdinal(_id.toInt())!!
    var description : String = ""
    var initialized : Boolean = false
    lateinit var lastUpdateDate : Date

    constructor(_id : Long, enum : Enum,  description : String, lastUpdateDate : Date?) : this(_id) {
        this._id = _id
        this.enum = enum
        this.description = description
        this.lastUpdateDate = lastUpdateDate ?: Date()
    }

    companion object {
        fun fromFeed(feed : SyndFeed) : Blog {
            return Blog.Enum.fromUrl(feed.link)!!.let{
                Blog(it.ordinal.toLong(),
                        it,
                        feed.description,
                        feed.publishedDate
                )
            }
        }
    }

    enum class Enum(val blogName : String, val url : String, val feedPath : String) : Serializable {
        MATOME_2GOU("仮面ライダーまとめ２号", "http://kamenrider2.net",  "/feed")
        ,HENSHIN_SOKUHOU("変身速報","http://www.henshin-hero.com/", "index.rdf")
        ,TOKUSATSU_MATOME("特撮まとめちゃんねる", "http://maskrider-futaba.info", "/feed/")
        ,JIHOU("仮面ライダー遅報","http://www.kr753.com", "/feed");

        companion object  {
            fun fromUrl(url : String): Blog.Enum?
                    = Blog.Enum.values().firstOrNull { it.url == url }
            fun fromOrdinal(ordinal : Int) : Blog.Enum?
                    = Blog.Enum.values().firstOrNull { it.ordinal  == ordinal }
        }

        val id = ordinal.toLong()

        fun getFeedUrl() : String {
            return this.url + this.feedPath
        }
    }
}