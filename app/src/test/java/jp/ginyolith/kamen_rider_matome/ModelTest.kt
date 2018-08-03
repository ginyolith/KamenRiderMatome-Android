package jp.ginyolith.kamen_rider_matome

import jp.ginyolith.kamen_rider_matome.data.Article
import jp.ginyolith.kamen_rider_matome.data.Blog
import org.junit.Test
import org.junit.Assert.*
import java.util.*

class ModelTest {
    @Test
    fun Articleクラスの同サイト判定処理(){
        val url = "http://www.kr753.com/34750"
        val articleJihou = Article(0, Blog.Enum.JIHOU.id, Date(), "", "http://www.kr753" +
                ".com/34750", "")
        assertTrue(articleJihou.isSameBlog(url))

        val articleMatome = Article(0, Blog.Enum.TOKUSATSU_MATOME.id, Date(), "", "http://www" +
                ".kr753" +
                ".com/34750", "")
        assertFalse(articleMatome.isSameBlog(url))
    }
}