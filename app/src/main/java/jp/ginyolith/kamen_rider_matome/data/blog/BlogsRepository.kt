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
import jp.ginyolith.kamen_rider_matome.data.blog.local.BlogsLocalDataSource
import jp.ginyolith.kamen_rider_matome.data.blog.remote.BlogsRemoteDataSource
import java.util.*
import java.util.concurrent.CountDownLatch

/**
 * Concrete implementation to load tasks from the data sources into a cache.
 *
 *
 * For simplicity, this implements a dumb synchronisation between locally persisted data and data
 * obtained from the server, by using the remote data source only if the local database doesn't
 * exist or is empty.
 */
class BlogsRepository// Prevent direct instantiation.
private constructor(remote: BlogsRemoteDataSource,
                    local: BlogsLocalDataSource) : BlogsDataSource {

    private val mRemote: BlogsRemoteDataSource = remote
    private val mLocal: BlogsLocalDataSource = local

    /**
     * This variable has package local visibility so it can be accessed from tests.
     */
    private var mCachedBlogs: MutableMap<Long, Blog>? = null

    /**
     * Marks the cache as invalid, to force an update the next time data is requested. This variable
     * has package local visibility so it can be accessed from tests.
     */
    private var mCacheIsDirty = true

    /**
     * Gets tasks from cache, local data source (SQLite) or remote data source, whichever is
     * available first.
     *
     *
     * Note: [LoadTasksCallback.onDataNotAvailable] is fired if all data sources fail to
     * get the data.
     */
    override fun getBlogs(callback: BlogsDataSource.LoadBlogsCallback) {
        // Respond immediately with cache if available and not dirty
        if (mCachedBlogs != null && !mCacheIsDirty) {
            callback.onAllBlogsLoaded(ArrayList<Blog>(mCachedBlogs!!.values))
            return
        }

        if (mCacheIsDirty) {
            // If the cache is dirty we need to fetch new data from the network.
            mRemote.getBlogs(object : BlogsDataSource.LoadBlogsCallback{
                override fun onAllBlogsLoaded(blogs: List<Blog>) {
                    mLocal.insertOrUpdateBlogs(blogs)
                    callback.onAllBlogsLoaded(blogs)
                }

                override fun onSomeDataNotAvailable(blogs: List<Blog>, errors: List<BlogsDataSource.BlogLoadError>) {
                    mLocal.insertOrUpdateBlogs(blogs)
                    callback.onSomeDataNotAvailable(blogs, errors)
                }

                override fun onAllDataNotAvailable(errors: List<BlogsDataSource.BlogLoadError>) {
                    callback.onAllDataNotAvailable(errors)
                }
            })
        }

        // Query the local storage if available. If not, query the network.
        mLocal.getBlogs(object : BlogsDataSource.LoadBlogsCallback{
            override fun onAllBlogsLoaded(blogs: List<Blog>) {
                refreshCache(blogs)
                callback.onAllBlogsLoaded(blogs)
            }

            override fun onSomeDataNotAvailable(blogs: List<Blog>, errors: List<BlogsDataSource.BlogLoadError>) {
                refreshCache(blogs)
                callback.onSomeDataNotAvailable(blogs, errors)
            }

            override fun onAllDataNotAvailable(errors: List<BlogsDataSource.BlogLoadError>) {
                callback.onAllDataNotAvailable(errors)
            }

        })
    }

    fun getBlogsSync(): ArrayList<Blog> {
        val currentBlogs = ArrayList<Blog>()
        val latch = CountDownLatch(1)
        getBlogs(object : BlogsDataSource.LoadBlogsCallback{
            override fun onAllBlogsLoaded(blogs: List<Blog>) {
                currentBlogs.addAll(blogs)
                latch.countDown()
            }

            override fun onSomeDataNotAvailable(blogs: List<Blog>, errors: List<BlogsDataSource.BlogLoadError>) {
                currentBlogs.addAll(blogs)
                latch.countDown()
            }

            override fun onAllDataNotAvailable(errors: List<BlogsDataSource.BlogLoadError>) {
                latch.countDown()
            }
        })
        latch.await()

        return currentBlogs
    }

    fun saveBlogAsInitialized(blog : Blog) {
        mLocal.saveBlogAsInitialized(blog)

    }

    private fun refreshCache(blogs: List<Blog>) {
        if (mCachedBlogs == null) {
            mCachedBlogs = LinkedHashMap()
        }
        mCachedBlogs!!.clear()
        for (blog in blogs) {
            mCachedBlogs!![blog._id] = blog
        }
        mCacheIsDirty = false
    }

    fun getBlogSync(id : Long): Blog? {
        if (mCacheIsDirty || mCachedBlogs == null) {
            val latch = CountDownLatch(1)
            getBlogs(object : BlogsDataSource.LoadBlogsCallback {
                override fun onAllBlogsLoaded(blogs: List<Blog>) {
                    latch.countDown()
                }
                override fun onSomeDataNotAvailable(blogs: List<Blog>, errors: List<BlogsDataSource.BlogLoadError>) {
                    latch.countDown()
                }
                override fun onAllDataNotAvailable(errors: List<BlogsDataSource.BlogLoadError>) {
                    latch.countDown()
                }
            })
            latch.await()
        }

        // Respond immediately with cache if available and not dirty
        return mCachedBlogs?.get(id)
    }

    companion object {

        private var INSTANCE: BlogsRepository? = null

        /**
         * singletonインスタンスを返す。
         * 初期化の必要がある場合、インスタンスを生成する。
         */
        fun getInstance(context : Context): BlogsRepository {
            if (INSTANCE == null) {
                val remote = BlogsRemoteDataSource.instance
                val local = BlogsLocalDataSource.getInstance(RSSDatabase.getInstance(context).blogDao())
                INSTANCE = BlogsRepository(remote, requireNotNull(local))
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
