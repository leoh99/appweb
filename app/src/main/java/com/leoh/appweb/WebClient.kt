package com.leoh.appweb

import android.util.Log
import android.webkit.*
import android.widget.Toast
import kotlinx.coroutines.experimental.async
import org.json.JSONObject
import org.jsoup.Jsoup

/**
 * Created by leoh on 12/14/17.
 */
class WebClient(private val activity: MainActivity) : WebViewClient() {
    private val webviewCookieManager = CookieManager.getInstance()

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (request.method == "POST")
            return false

        if (request.hasGesture()||request.isRedirect) {
            when (request.url.scheme) {
                "http", "https", "file" -> getUrl(view, request)
            }
        }

        return true
    }
    
    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
        val url = request.url.toString()
        if (WebContent.isAdv(url))
            return emptyResponse
        
        return null
    }

    private fun getUrl(view: WebView, request: WebResourceRequest) {
        val userAgent: String = view.settings.userAgentString
        val url = request.url.toString()
        Log.d("TAG", "Connecting...$url")
        async {
            try {
                val conn = Jsoup.connect(url)
                        .userAgent(userAgent)
                        .timeout(5000)

                webviewCookieManager.getCookie(url)?.let {
                    conn.header("Cookie", it)
                }

                val res = conn.execute()
                var doc = res.parse()
                Log.d("TAG", doc.title())
                val rmList = WebContent.getSiteCfg(request.url.host)
                rmList?.forEach { doc.select(it).remove() }
                for ((key, value) in res.cookies())
                    webviewCookieManager.setCookie(url, "$key=$value")
                loadDataWithBaseURL(view, res.url().toString(), doc.html(), res.contentType(), res.charset())
            } catch (e: Exception) {
                loadUrl(view, url)
            }
        }
    }

    fun loadDataWithBaseURL(view: WebView, url: String, data: String, mime: String, encoding: String) {
        activity.runOnUiThread {
            view.loadDataWithBaseURL(url, data, mime,encoding,url)
        }
    }

    fun loadUrl(view: WebView, url: String) {
        activity.runOnUiThread {
            view.loadUrl(url)
        }
    }

    @JavascriptInterface
    fun showToast(toast: String) {
        Toast.makeText(activity, toast, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun clearData() {
        showToast("All cleared")
    }

    @JavascriptInterface
    fun clearCookies() {
        val webviewCookieManager = CookieManager.getInstance()
        webviewCookieManager.removeAllCookies(null)
    }

    @JavascriptInterface
    fun goSetting() {
        showToast("Show setting page")
    }

    @JavascriptInterface
    fun getHistory() : String {
        val jResult = JSONObject()
//        val jArray = JSONArray()
//        val histList = webHostory.history
//        Collections.sort(histList)
//        try {
//            for (l in histList) {
//                val jGroup = JSONObject()
//                jGroup.put("url", l.url)
//                jGroup.put("title", l.title)
//
//                jArray.put(jGroup)
//            }
//            jResult.put("recordset", jArray)
//        } catch (e: JSONException) {
//            e.printStackTrace()
//        }

        return jResult.toString()
    }

    object emptyResponse : WebResourceResponse("", "", null)
}