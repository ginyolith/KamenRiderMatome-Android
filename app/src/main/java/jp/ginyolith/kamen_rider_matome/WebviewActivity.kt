package jp.ginyolith.kamen_rider_matome

import android.content.Intent
import android.databinding.DataBindingUtil
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import jp.ginyolith.kamen_rider_matome.data.Article
import jp.ginyolith.kamen_rider_matome.data.Blog
import jp.ginyolith.kamen_rider_matome.databinding.ActivityWebviewBinding

class WebViewActivity : AppCompatActivity() {
    private lateinit var binding : ActivityWebviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val article = intent.getSerializableExtra("article") as Article

        binding = DataBindingUtil.setContentView(this, R.layout.activity_webview)
        binding.webview.settings.run {
            useWideViewPort = true
            loadWithOverviewMode = true
            javaScriptEnabled = true
            loadsImagesAutomatically = true
            builtInZoomControls = true
            setSupportZoom(true)
            defaultTextEncodingName = "utf-8"
            setAppCacheEnabled(true)
        }

        binding.webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                requireNotNull(request?.url)
                return if (article.isSameBlog(request?.url.toString())) {
                    false
                } else {
                    startActivity(Intent(Intent.ACTION_VIEW, request?.url))
                    true
                }
            }
        }

        binding.webview.loadUrl(article.url)
        setSupportActionBar(binding.toolbar)

        binding.toolbar.setOnClickListener { binding.webview.scrollTo(0,0) }
    }

    override fun onBackPressed() {
        if (binding.webview.canGoBack()) {
            binding.webview.goBack()
        } else {
            super.onBackPressed()
        }
    }

}
