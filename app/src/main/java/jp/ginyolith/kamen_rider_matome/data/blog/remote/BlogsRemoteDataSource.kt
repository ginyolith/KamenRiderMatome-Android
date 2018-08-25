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

package jp.ginyolith.kamen_rider_matome.data.blog.remote

import com.rometools.rome.feed.synd.SyndFeed
import jp.ginyolith.kamen_rider_matome.data.HttpAccess
import jp.ginyolith.kamen_rider_matome.data.blog.Blog
import jp.ginyolith.kamen_rider_matome.data.blog.BlogsDataSource
import okhttp3.Call
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.CountDownLatch

/**
 * Implementation of the data source that adds a latency simulating network.
 */
class BlogsRemoteDataSource // Prevent direct instantiation.
private constructor() : BlogsDataSource {

    override fun getBlogs(callback: BlogsDataSource.LoadBlogsCallback) {
        Thread(Runnable {
            val blogs = ArrayList<Blog>()
            val urls = Blog.Enum.values().map(Blog.Enum::getFeedUrl)

            val latch = CountDownLatch(urls.size)
            val errors = ArrayList<RemoteBlogLogError>()

            urls.forEach {
                HttpAccess.getFeed(it, object : HttpAccess.FeedRequestCallback {
                    override fun onFailure(call: Call?, e: IOException?) {
                        errors.add(RemoteBlogLogError(call, e))
                        latch.countDown()
                    }

                    override fun onResponse(call: Call?, response: Response?, syndFeed: SyndFeed) {
                        blogs.add(Blog.fromFeed(feed = requireNotNull(syndFeed)))
                        latch.countDown()
                    }
                })
            }

            latch.await()

            when {
                errors.size == urls.size -> callback.onAllDataNotAvailable(errors)
                errors.isNotEmpty() -> callback.onSomeDataNotAvailable(blogs, errors)
                else -> callback.onAllBlogsLoaded(blogs)
            }
        }).start()
    }


    private class RemoteBlogLogError(val call : Call?, val e : IOException?) : BlogsDataSource.BlogLoadError {
        override fun getErrorMsg(): String
            = "Http request error url = ${call?.request()?.url()}"
    }



    companion object {
        private var sINSTANCE: BlogsRemoteDataSource? = null

        val instance: BlogsRemoteDataSource
            get() {
                if (sINSTANCE == null) {
                    sINSTANCE = BlogsRemoteDataSource()
                }
                return requireNotNull(sINSTANCE)
            }
    }
}
