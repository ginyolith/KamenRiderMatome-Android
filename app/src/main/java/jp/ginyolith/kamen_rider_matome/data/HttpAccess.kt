package jp.ginyolith.kamen_rider_matome.data

import android.support.annotation.VisibleForTesting
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import okhttp3.*
import java.io.IOException
import java.io.StringReader

object HttpAccess {
    private val client : OkHttpClient by lazy {
        OkHttpClient()
    }

    @VisibleForTesting
    fun sendRequest(url : String, callback : Callback){
        val req = Request.Builder()
                .url(url)
                .build()
        client.newCall(req).enqueue(callback)
    }

    fun getFeed(url : String, callback : FeedRequestCallback) {
        this.sendRequest(url, object : Callback {
            override fun onFailure(call: Call?, e: IOException?) {
                callback.onFailure(call, e)
            }

            override fun onResponse(call: Call?, response: Response?) {
                val body = response?.body()?.source()?.readUtf8()
                val feed = SyndFeedInput().build(StringReader(body))
                callback.onResponse(call, response, feed)
            }
        })
    }

    interface FeedRequestCallback {
        fun onFailure(call: Call?, e: IOException?)
        fun onResponse(call: Call?, response: Response?, syndFeed: SyndFeed)
    }

}