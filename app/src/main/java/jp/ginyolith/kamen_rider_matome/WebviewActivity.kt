package jp.ginyolith.kamen_rider_matome

import android.databinding.DataBindingUtil
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import jp.ginyolith.kamen_rider_matome.databinding.ActivityWebviewBinding

class WebviewActivity : AppCompatActivity() {
    private lateinit var binding : ActivityWebviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        binding.webview.loadUrl(intent.getStringExtra("url"))
    }


}
