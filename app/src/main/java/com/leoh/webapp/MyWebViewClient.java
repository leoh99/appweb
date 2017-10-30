package com.leoh.webapp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.leoh.webapp.net.HttpClient;
import com.leoh.webapp.net.WebHistoryItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by leoh on 1/12/17.
 */

public class MyWebViewClient extends WebViewClient {

    private static final String TAG = "appwebclient";
    private final AdvancedWebView webView;
    private final ProgressBar progressBar;
    private final HttpClient httpClient;
    private final Activity activity;
    private String reqUrl;

    public MyWebViewClient(Activity c, AdvancedWebView view, ProgressBar progress) {
        activity = c;
        webView = view;
        progressBar = progress;
        httpClient = new HttpClient(c, this);
        setUserAgentString(webView.getSettings().getUserAgentString());
    }

    private void setUserAgentString(String string) {
        httpClient.setUserAgentString(string);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        //Log.d(TAG, "start=" + url);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        webView.getSettings().setBlockNetworkImage(true);
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageCommitVisible(WebView view, String url) {
        super.onPageCommitVisible(view, url);
        Log.d(TAG, "Visible:" +url);
        webView.getSettings().setBlockNetworkImage(false);
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        Log.e(TAG, "Receive error:"+request.getUrl());
        super.onReceivedError(view, request, error);
    }

    @Override
    public void onPageFinished(WebView view, String url)
    {
        Log.d(TAG, "finish:" +url);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Uri uri = request.getUrl();
        String url = uri.toString();
        Log.d(TAG, "Over=" + url);

        if (url.startsWith("intent"))
            return true;

        if(reqUrl != null && url != null && url.equals(reqUrl)) {
            Log.d(TAG, "Back=" + url);
            webView.goBack();
            return true;
        }
        Log.d(TAG, "u=" + reqUrl);
        reqUrl = url;
        if (url.startsWith("http:") || url.startsWith("https:")) {
            if (httpClient.loadUrl(url)) {
                progressBar.setProgress(0);
                progressBar.setVisibility(View.VISIBLE);
                return true;
            }
        }

        return false;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        WebResourceResponse response = null;
        Uri uri = request.getUrl();
        String url = uri.toString();
        //Log.d(TAG, "Req=" + url);
        if (url.startsWith("http:") || url.startsWith("https:")) {
            response = httpClient.getWebResourceResponse(uri, request.getRequestHeaders());
        }
        //Log.d(TAG, "responsed");
        return response;

    }

    public void loadDataWithBaseURL(final String url, final String html, final String mime, final String encode) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.loadDataWithBaseURL(url, html, mime, encode, url);
            }
        });
    }

    public void loadUrl(final String url) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl(url);
            }
        });
    }

    public void onLoadError(final String msg) {
        Log.d(TAG, "Error:" + msg);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    @JavascriptInterface
    public void showToast(final String toast) {
        Toast.makeText(activity, toast, Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void saveData() {
        httpClient.webHostory.save();
        httpClient.webCache.save();
    }

    @JavascriptInterface
    public void clearData() {
        showToast("All cleared");
        httpClient.webCache.clear();
        httpClient.webHostory.clear();
        //webView.clearCache(true);
        //webView.clearMatches();
        //webView.clearHistory();
        //clearCookies();
     }

    @JavascriptInterface
    public void clearCookies() {
        CookieManager webviewCookieManager = CookieManager.getInstance();
        webviewCookieManager.removeAllCookies(null);
    }

    @JavascriptInterface
    public void goHistory() {
        Log.d(TAG, "goHistory");
        String h1 = "<!doctype html><html>" +
                "<head>" +
                "<meta charset=\"utf-8\">" +
                "<meta name=\"referrer\" content=\"no-referrer\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
                "<link rel=\"stylesheet\" href=\"jquery/jquery.mobile-1.4.5.min.css\">" +
                "<script src=\"appweb.js\"></script>" +
                "<script src=\"jquery/jquery-1.11.3.min.js\"></script>" +
                "<script src=\"jquery/jquery.mobile-1.4.5.min.js\"></script>" +
                "</head><body><div data-role=\"page\" data-theme=\"b\">" +
                "<a href=\"#\" onClick=\"javascript:clearData();\">Clean</a>" +
                "<div data-role=\"content\">\n" +
                "<ul id=\"list\" class=\"touch\" data-role=\"listview\" data-icon=\"false\" data-split-icon=\"delete\" data-split-theme=\"d\">";

        String h2 ="</ul></div></div></body></html>";

        StringBuilder builder = new StringBuilder(h1);

        List<WebHistoryItem> histList = httpClient.webHostory.getHistory();
        for (WebHistoryItem i : histList) {
            builder.append("<li><a href=\"");
            builder.append(i.url);
            builder.append("\"><h3>");
            builder.append(i.title);
            builder.append("</h3></a><a href=\"#\" class=\"delete\">Delete</a></li>");
        }

        builder.append(h2);
        loadDataWithBaseURL("file:///android_asset/www/", builder.toString(), "text/html", "UTF-8");
    }

    @JavascriptInterface
    public void goSetting() {
        showToast("Show setting page");
    }

    @JavascriptInterface
    public String getHistory() {
        JSONObject jResult = new JSONObject();
        JSONArray jArray = new JSONArray();
        List<WebHistoryItem> histList = httpClient.webHostory.getHistory();
        try {
            for (int i = 0; i < histList.size(); i++) {
                JSONObject jGroup = new JSONObject();
                jGroup.put("url", histList.get(i).url);
                jGroup.put("title", histList.get(i).title);

                jArray.put(jGroup);
            }
            jResult.put("recordset", jArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jResult.toString();
    }
}
