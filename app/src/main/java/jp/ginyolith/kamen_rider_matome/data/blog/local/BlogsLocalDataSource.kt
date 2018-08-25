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

package jp.ginyolith.kamen_rider_matome.data.blog.local

import android.support.annotation.VisibleForTesting
import jp.ginyolith.kamen_rider_matome.data.blog.Blog
import jp.ginyolith.kamen_rider_matome.data.blog.BlogsDataSource


/**
 * ブログ情報のデータソース (DB）
 */
class BlogsLocalDataSource private constructor(private val mBlogsDao: BlogsDao) : BlogsDataSource {

    /**
     * [ArticlessDataSource.LoadBlogsCallback.onAllDataNotAvailable]はデータが存在しない時に発火される
     */
    override fun getBlogs(callback: BlogsDataSource.LoadBlogsCallback) {
        val runnable = Runnable {
            val blogs = mBlogsDao.selectAll()
            if (blogs.isEmpty()) {
                // This will be called if the table is new or just empty.
                callback.onAllDataNotAvailable(ArrayList())
            } else {
                callback.onAllBlogsLoaded(blogs)
            }
        }

        Thread(runnable).start()
    }

    fun insertOrUpdateBlogs(blogs : List<Blog>) {
        mBlogsDao.insertOrUpdate(*blogs.toTypedArray())
    }

    fun saveBlogAsInitialized(blog: Blog) {
        mBlogsDao.insertOrUpdate(blog.apply { blog.initialized = true })
    }

    companion object {
        @Volatile
        private var INSTANCE: BlogsLocalDataSource? = null

        fun getInstance(blogsDao: BlogsDao): BlogsLocalDataSource? {
            if (INSTANCE == null) {
                synchronized(BlogsLocalDataSource::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = BlogsLocalDataSource(blogsDao)
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
