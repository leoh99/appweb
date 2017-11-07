package com.leoh.webapp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
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

import java.io.File;
import java.util.Collections;
import java.util.List;

import static android.util.Log.d;

/**
 * Created by leoh on 1/12/17.
 */

public class MyWebViewClient extends WebViewClient {

    private static final String TAG = "appwebclient";
    private final AdvancedWebView webView;
    private final ProgressBar progressBar;
    private final HttpClient httpClient;
    private final Activity activity;
    //private String curUrl;
    private String homeUrl = "file:///android_asset/www/index.html";

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
        //Log.d(TAG, "Visible:" +url);
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
        //Log.d(TAG, "finish:" +url);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Uri uri = request.getUrl();
        String scheme = uri.getScheme();
        String url = uri.toString();

        if (scheme.equals("intent")) {
            return true;
        }

        if (scheme.startsWith("http")) {
            if (httpClient.loadUrl(url)) {
                progressBar.setProgress(0);
                progressBar.setVisibility(View.VISIBLE);
                Log.d(TAG, "OverrideUrlLoading=" + url);
                return true;
            }
        }

        Log.d(TAG, "Over=" + url);
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

    public void goHome() {
        File extStore = Environment.getExternalStorageDirectory();
        File myFile = new File(extStore.getAbsolutePath() + "/APPWEB/index.html");
        if(myFile.exists()){
            homeUrl = "file://"+ myFile;
            d(TAG, myFile.toString());
        }

        loadUrl(homeUrl);
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
        webView.clearCache(true);
        webView.clearMatches();
        webView.clearHistory();
        clearCookies();
     }

    @JavascriptInterface
    public void clearCookies() {
        CookieManager webviewCookieManager = CookieManager.getInstance();
        webviewCookieManager.removeAllCookies(null);
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
        Collections.sort(histList);
        try {
            for (WebHistoryItem  l : histList) {
                JSONObject jGroup = new JSONObject();
                jGroup.put("url", l.url);
                jGroup.put("title", l.title);

                jArray.put(jGroup);
            }
            jResult.put("recordset", jArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jResult.toString();
    }
}
