package com.leoh.appweb

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ProgressBar

class MainActivity : Activity() {

    lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        webView = findViewById<WebView>(R.id.webView)

        setupWebView()
    }

    private fun setupWebView() {
        WebContent.setup(this)
        val progress: ProgressBar = findViewById(R.id.progressBar)
        val client = WebClient(this)
        with(webView) {
            webViewClient = client
            addJavascriptInterface(client, "Android")
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress == 100)
                        progress.visibility = View.GONE
                    else if (progress.visibility != View.VISIBLE)
                        progress.visibility = View.VISIBLE
                    progress.setProgress(newProgress)
                }
            }

            settings.javaScriptEnabled = true
            settings.minimumFontSize = 16

            loadUrl("file:///android_asset/www/index.html")
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack())
            webView.goBack();
        else
            super.onBackPressed()
    }
    override fun onPause() {
        super.onPause()
        webView.pauseTimers()
    }

    override fun onResume() {
        super.onResume()
        webView.resumeTimers()
    }
}
