package jp.ginyolith.kamen_rider_matome.data.article.remote

import com.rometools.rome.feed.synd.SyndFeed
import jp.ginyolith.kamen_rider_matome.data.HttpAccess
import jp.ginyolith.kamen_rider_matome.data.article.Article
import jp.ginyolith.kamen_rider_matome.data.blog.ArticlesDataSource
import jp.ginyolith.kamen_rider_matome.data.blog.BlogsRepository
import okhttp3.Call
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.CountDownLatch

class ArticlesRemoteDataSource private constructor(
        private val getBlogsRepository: () -> BlogsRepository): ArticlesDataSource {

    override fun getArticles(callback: ArticlesDataSource.Callback) {
        Thread(Runnable {
            val articles = ArrayList<Article>()
            val blogs = getBlogsRepository().getBlogsSync()

            val latch = CountDownLatch(blogs.size)
            val errors = ArrayList<ArticlesDataSource.Error>()

            blogs.forEach {blog ->
                HttpAccess.getFeed(blog.enum.getFeedUrl(), object : HttpAccess.FeedRequestCallback {
                    override fun onFailure(call: Call?, e: IOException?) {
                        errors.add(RemoteArticleLogError(call, e))
                        latch.countDown()
                    }

                    override fun onResponse(call: Call?, response: Response?, syndFeed: SyndFeed) {
                        if (syndFeed.entries == null) {
                            onFailure(call, IOException())
                            latch.countDown()
                            return
                        }

                        syndFeed.entries.map {
                            Article.fromFeedEntry(it, blog)
                        }.toList().let {
                            articles.addAll(it)
                        }

                        latch.countDown()
                    }
                })
            }

            latch.await()

            when {
                errors.size == blogs.size -> callback.onAllFaild(errors)
                errors.isNotEmpty() -> callback.onSomeFaild(articles, errors)
                else -> callback.onAllSuccess(articles)
            }
        }).start()
    }

    private class RemoteArticleLogError(val call : Call?, val e : IOException?) : ArticlesDataSource.Error {
        override fun getErrorMsg(): String
                = "Http request error url = ${call?.request()?.url()}"
    }


    companion object {
        private var sINSTANCE: ArticlesRemoteDataSource? = null

        fun getInstance(getBlogsRepository: () -> BlogsRepository) : ArticlesRemoteDataSource{
            if (sINSTANCE == null) {
                sINSTANCE = ArticlesRemoteDataSource(getBlogsRepository)
            }
            return requireNotNull(sINSTANCE)
        }
    }
}