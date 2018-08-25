package jp.ginyolith.kamen_rider_matome.data.article.local

import android.support.annotation.VisibleForTesting
import jp.ginyolith.kamen_rider_matome.data.article.Article
import jp.ginyolith.kamen_rider_matome.data.blog.ArticlesDataSource
import jp.ginyolith.kamen_rider_matome.data.blog.Blog
import jp.ginyolith.kamen_rider_matome.data.blog.BlogsRepository

class ArticlesLocalDataSource
    private constructor(
        private val mArticlesDao : ArticlesDao,
        private val getBlogsRepository: () -> BlogsRepository) : ArticlesDataSource {

    override fun getArticles(callback: ArticlesDataSource.Callback) {
        val runnable = Runnable {
            val articles = mArticlesDao.selectAll().apply {
                forEach { it.blog = requireNotNull(getBlogsRepository().getBlogSync(it.blogId)) }
            }

            if (articles.isEmpty()) {
                callback.onAllFaild(ArrayList())
            } else {
                callback.onAllSuccess(articles)
            }
        }

        Thread(runnable).start()
    }

    fun insert(articles : List<Article>) {
        mArticlesDao.insert(*articles.toTypedArray())
    }

    companion object {
        @Volatile
        private var INSTANCE: ArticlesLocalDataSource? = null

        fun getInstance(articlesDao: ArticlesDao, getBlogsRepository: () -> BlogsRepository): ArticlesLocalDataSource? {
            if (INSTANCE == null) {
                synchronized(ArticlesLocalDataSource::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = ArticlesLocalDataSource(articlesDao, getBlogsRepository)
                    }
                }
            }
            return INSTANCE
        }

        @VisibleForTesting
        internal fun clearInstance() {
            INSTANCE = null
        }
    }
}