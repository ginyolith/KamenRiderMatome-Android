/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.ginyolith.kamen_rider_matome.data.blog

import android.content.Context
import jp.ginyolith.kamen_rider_matome.data.RSSDatabase
import jp.ginyolith.kamen_rider_matome.data.article.Article
import jp.ginyolith.kamen_rider_matome.data.article.local.ArticlesDao
import jp.ginyolith.kamen_rider_matome.data.article.local.ArticlesLocalDataSource
import jp.ginyolith.kamen_rider_matome.data.article.remote.ArticlesRemoteDataSource
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.collections.ArrayList

/**
 * Concrete implementation to load tasks from the data sources into a cache.
 *
 *
 * For simplicity, this implements a dumb synchronisation between locally persisted data and data
 * obtained from the server, by using the remote data source only if the local database doesn't
 * exist or is empty.
 */
class ArticlesRepository// Prevent direct instantiation.
private constructor(remote: ArticlesRemoteDataSource,
                    local: ArticlesLocalDataSource,
                    val getBlogsRepository : () -> BlogsRepository) : ArticlesDataSource {

    private val mRemote: ArticlesRemoteDataSource = remote
    private val mLocal: ArticlesLocalDataSource = local

    /**
     * This variable has package local visibility so it can be accessed from tests.
     */
    private var mCachedArticles: MutableMap<Long, Article>? = null

    /**
     * Marks the cache as invalid, to force an update the next time data is requested. This variable
     * has package local visibility so it can be accessed from tests.
     */
    private var mCacheIsDirty = true

    override fun getArticles(callback: ArticlesDataSource.Callback) {
        val latch = CountDownLatch(1)
        val errors = ArrayList<ArticlesDataSource.Error>()
        val loadNewArticle = { articles : List<Article> ->
            val newArticles = articles.filter { it.isNewArticle()}
            if (newArticles.isEmpty()) {
                callback.onNoNewArticle()
            } else {
                mLocal.insert(newArticles)
            }

            newArticles.map{it.blog}.distinct().forEach {
                if (!it.initialized) {
                    getBlogsRepository().saveBlogAsInitialized(it)
                }
            }

            latch.countDown()
        }

        mRemote.getArticles(object : ArticlesDataSource.Callback{
            override fun onNoNewArticle() {}

            override fun onAllSuccess(articles: List<Article>)
                = loadNewArticle(articles)

            override fun onSomeFaild(articles: List<Article>, errorsRemote: List<ArticlesDataSource.Error>) {
                loadNewArticle(articles)
                errors.addAll(errorsRemote)
            }

            override fun onAllFaild(errorsRemote: List<ArticlesDataSource.Error>) {
                errors.addAll(errorsRemote)
                latch.countDown()
            }
        })

        latch.await()

        // Respond immediately with cache if available and not dirty
        if (mCachedArticles != null && !mCacheIsDirty) {
            callback.onAllSuccess(ArrayList<Article>(mCachedArticles!!.values))
            return
        }


        mLocal.getArticles(object : ArticlesDataSource.Callback{
            override fun onNoNewArticle() {}

            override fun onAllSuccess(articles: List<Article>) {
                refreshCache(articles)
                callback.onAllSuccess(articles)
            }

            override fun onSomeFaild(articles: List<Article>, errorsLocal: List<ArticlesDataSource.Error>) {
                refreshCache(articles)
                callback.onSomeFaild(articles, errors.apply { addAll(errorsLocal) })
            }

            override fun onAllFaild(errorsLocal: List<ArticlesDataSource.Error>) {
                callback.onAllFaild(errors.apply { addAll(errorsLocal) })
            }

        })
    }

    private fun refreshCache(articles: List<Article>) {
        if (mCachedArticles == null) {
            mCachedArticles = LinkedHashMap()
        }
        mCachedArticles!!.clear()
        for (article in articles) {
            mCachedArticles!![article._id] = article
        }
//        mCacheIsDirty = false
    }

    companion object {

        private var INSTANCE: ArticlesRepository? = null

        /**
         * singletonインスタンスを返す。
         * 初期化の必要がある場合、インスタンスを生成する。
         */
        fun getInstance(context : Context): ArticlesRepository {
            val getBlogsRepository = {
                BlogsRepository.getInstance(context)
            }

            if (INSTANCE == null) {
                val remote = ArticlesRemoteDataSource.getInstance(getBlogsRepository)
                val local = ArticlesLocalDataSource.getInstance(RSSDatabase.getInstance(context).articleDao(), getBlogsRepository)
                INSTANCE = ArticlesRepository(remote, requireNotNull(local), getBlogsRepository)
            }
            return requireNotNull(INSTANCE)
        }

        /**
         * Used to force [.getInstance] to create a new instance
         * next time it's called.
         */
        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
