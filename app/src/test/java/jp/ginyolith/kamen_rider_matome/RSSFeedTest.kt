package jp.ginyolith.kamen_rider_matome

import jp.ginyolith.kamen_rider_matome.data.Article
import jp.ginyolith.kamen_rider_matome.data.Blog
import jp.ginyolith.kamen_rider_matome.data.HttpAccess
import org.junit.Test
import org.junit.Assert.*
import java.text.SimpleDateFormat

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class RSSFeedTest {
    val testRssUrl = "https://kamen-rider-matome.firebaseapp.com/test_rss.txt"

    @Test
    fun RSSを取得して正常にレスポンスが帰ってくる() {
        val result = HttpAccess().getRSSFeed(testRssUrl)
        assertNotNull(result)
        assertNotEquals("", result)
    }

    @Test
    fun RSSを取得してXMLをパースしブログ情報を取得する() {
        val result = HttpAccess().getBlogInfoFromRSSFeed(testRssUrl)
        assertNotNull(result.enum.blogName)
        assertEquals(Blog.Enum.TOKUSATSU_MATOME.blogName, result.enum.blogName)
        assertNotNull(result.description)
        assertEquals("仮面ライダーとか戦隊物のブログ", result.description)
    }

    @Test
    fun RSSを取得してXMLをパースし記事情報を取得する() {
        val article : Article = requireNotNull(HttpAccess().getLatestArticle(testRssUrl))
        assertNotNull(article)

        assertEquals("【 ウルトラマンタロウ】今でも子供人気は高い気がする", article.title)
        assertEquals("2018/07/29",
                SimpleDateFormat("yyyy/MM/dd").format(article.pubDate))
        assertEquals("15:00:29",
                SimpleDateFormat("HH:mm:dd").format(article.pubDate))

        assertEquals("http://maskrider-futaba.info/2018/07/29/post-12181/", article.url)
        assertEquals("http://img6.futabalog.com/2018/07/942b2ddf04b14f07defb941f39927d28.jpg", article.thumbnailUrl)
    }
}
