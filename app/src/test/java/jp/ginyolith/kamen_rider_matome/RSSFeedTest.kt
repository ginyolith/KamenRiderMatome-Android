package jp.ginyolith.kamen_rider_matome

import android.media.MediaRouter
import com.rometools.rome.feed.synd.SyndFeed
import jp.ginyolith.kamen_rider_matome.data.HttpAccess
import jp.ginyolith.kamen_rider_matome.data.article.Article
import jp.ginyolith.kamen_rider_matome.data.blog.Blog
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.HashMap
import java.util.concurrent.CountDownLatch
import javax.security.auth.callback.Callback


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class RSSFeedTest {
    enum class TestBlogUrl(val blog: Blog.Enum, url: String) {
        MATOME_2GOU(Blog.Enum.MATOME_2GOU, "test_rss_matome2gou.txt")
        ,HENSHIN_SOKUHOU(Blog.Enum.HENSHIN_SOKUHOU,"test_rss_henshin_sokuhou.txt")
        ,TOKUSATSU_MATOME(Blog.Enum.TOKUSATSU_MATOME, "test_rss.txt")
        ,JIHOU(Blog.Enum.JIHOU,"test_rss_jihou.txt");

        private val domein = "https://kamen-rider-matome.firebaseapp.com/"

        val testFeedUrl : String

        init {
            this.testFeedUrl = domein + url
        }
    }

    @Test
    fun RSSを取得して正常にレスポンスが帰ってくる() {
        val latch = CountDownLatch(TestBlogUrl.values().size)
        val result = HashMap<TestBlogUrl, String?>()

        val sendReq = {url : TestBlogUrl ->
            HttpAccess.sendRequest(
                    url.testFeedUrl,
                    object : okhttp3.Callback {
                        override fun onFailure(call: Call?, e: IOException?) {
                            latch.countDown()
                        }

                        override fun onResponse(call: Call?, response: Response?) {
                            try {
                                result[url] = response?.body()?.source()?.readUtf8()
                            } finally {
                                latch.countDown()
                            }
                        }
                    }
            )
        }

        TestBlogUrl.values().forEach { sendReq(it) }

        latch.await()

        assertEquals(TestBlogUrl.values().size, result.size)
        result.forEach {
            assertNotNull(it.value)
            assertNotNull(it.value)
            assertNotEquals("", it.value)
        }
    }
}
//
//    @Test
//    fun RSSを取得してXMLをパースしブログ情報を取得する() {
//        val result = HttpAccess().getBlogInfoFromRSSFeed(TestBlogUrl.TOKUSATSU_MATOME.testFeedUrl)
//        assertNotNull(result.enum.blogName)
//        assertEquals(Blog.Enum.TOKUSATSU_MATOME.blogName, result.enum.blogName)
//        assertNotNull(result.description)
//        assertEquals("仮面ライダーとか戦隊物のブログ", result.description)
//    }
//
//    @Test
//    fun 特撮まとめちゃんねる2018年7月29日時点のRSSを取得してXMLをパースし記事情報を取得する() {
//        val article : Article = requireNotNull(HttpAccess().getLatestArticle(TestBlogUrl.TOKUSATSU_MATOME.testFeedUrl))
//        assertNotNull(article)
//
//        assertEquals("【 ウルトラマンタロウ】今でも子供人気は高い気がする", article.title)
//        assertEquals("2018/07/29",
//                SimpleDateFormat("yyyy/MM/dd").format(article.pubDate))
//        assertEquals("15:00:29",
//                SimpleDateFormat("HH:mm:dd").format(article.pubDate))
//
//        assertEquals("http://maskrider-futaba.info/2018/07/29/post-12181/", article.url)
//        assertEquals("http://img6.futabalog.com/2018/07/942b2ddf04b14f07defb941f39927d28.jpg", article.thumbnailUrl)
//    }
//    @Test
//    fun 仮面ライダーまとめ２号2018年7月29日時点のRSSを取得してXMLをパースし記事情報を取得する() {
//        val targetBlog = TestBlogUrl.MATOME_2GOU
//        val article : Article = requireNotNull(HttpAccess().getLatestArticle(targetBlog.testFeedUrl))
//        assertNotNull(article)
//
//        assertEquals(targetBlog.blog.blogName, article.blog.enum.blogName)
//        assertEquals("並行世界設定がクローズアップされはじめたけど【仮面ライダービルド 46話】", article.title)
//        assertEquals("http://kamenrider2.net/archives/33190", article.url)
//
//        assertNotNull(article.thumbnailUrl)
//        assertNotEquals(article.thumbnailUrl, "")
//        assertEquals(article.thumbnailUrl, "http://kamenrider2.net/wp-content/uploads/DjOznw7V4AElhfo-300x169.jpg")
//    }
//
//    @Test
//    fun 変身速報2018年7月29日時点のRSSを取得してXMLをパースし記事情報を取得する() {
//        val targetBlog = TestBlogUrl.HENSHIN_SOKUHOU
//        val article : Article = requireNotNull(HttpAccess().getLatestArticle(targetBlog.testFeedUrl))
//        assertNotNull(article)
//
//        assertEquals(targetBlog.blog.blogName, article.blog.enum.blogName)
//        assertEquals("『ウルトラマンR/B(ルーブ)』第4話「光のウイニングボール」感想まとめ", article.title)
//        assertEquals("http://www.henshin-hero.com/archives/25177329.html", article.url)
//
//        assertNotNull(article.thumbnailUrl)
//        assertNotEquals(article.thumbnailUrl, "")
//        assertEquals(article.thumbnailUrl, "http://livedoor.blogimg.jp/henshinhero/imgs/2/0/20907f6e-s.jpg")
//    }
//
//    @Test
//    fun 仮面ライダー遅報2018年7月29日時点のRSSを取得してXMLをパースし記事情報を取得する() {
//        val targetBlog = TestBlogUrl.JIHOU
//        val article : Article = requireNotNull(HttpAccess().getLatestArticle(targetBlog.testFeedUrl))
//        assertNotNull(article)
//
//        assertEquals(targetBlog.blog.blogName, article.blog.enum.blogName)
//        assertEquals("ニンテンドースイッチで「仮面ライダー クライマックススクランブル ジオウ」が発売決定へ", article.title)
//        assertEquals("http://www.kr753.com/34630", article.url)
//
//        assertNotNull(article.thumbnailUrl)
//        assertNotEquals(article.thumbnailUrl, "")
//        assertEquals(article.thumbnailUrl, "https://i0.wp.com/www.kr753.com/wp-content/uploads/2018/07/kamenrider_zi-o_0004.jpg?resize=640%2C770")
//    }
