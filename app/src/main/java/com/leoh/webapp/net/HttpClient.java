package com.leoh.webapp.net;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebResourceResponse;

import com.leoh.webapp.MyWebViewClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;


/**
 * Created by leoh on 1/15/17.
 */

public class HttpClient {
    public static final WebResourceResponse emptyRes = new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
    ;
    private static final String TAG = "appwebhttp";
    private final MyWebViewClient webClient;
    private final WebParser parser;
    public WebCache webHostory;
    public WebCache webCache;
    private String userAgent = "";

    public HttpClient(Context c, MyWebViewClient webViewClient) {
        webClient = webViewClient;
        parser = new WebParser(c);
        webHostory = new WebCache(c, "history.data");
        webCache = new WebCache(c, "cache.data");
    }

    public void setUserAgentString(String string) {
        userAgent = string;
    }

    public boolean loadUrl(final String url) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    WebResponse res = parser.parseUrl(url, userAgent);
                    webClient.loadDataWithBaseURL(res.url, new String(res.data), res.mime, res.encode);
                } catch (IOException e) {
                    e.printStackTrace();
                    WebResponse cache = webHostory.get(url);
                    if (cache == null)
                        webClient.loadUrl(url);
                    else
                        webClient.loadDataWithBaseURL(cache.url, new String(cache.data), cache.mime, cache.encode);
                }

            }
        }).start();

        return true;
    }


    public WebResourceResponse getWebResourceResponse(final Uri uri, Map<String, String> headers) {
        String url = uri.toString();
        if (parser.isBlock(uri)) {
            return emptyRes;
        }

        WebResourceResponse response = getFromCache(url, webCache);
        return response;
    }

    private WebResourceResponse getFromCache(String url, WebCache cache) {
        WebResponse resp = cache.get(url);
        if (resp != null) {
            Log.d(TAG, "HIT=" + url);
            resp.cacheHit++;
            return new WebResourceResponse(resp.mime, resp.encode, new ByteArrayInputStream(resp.data));
        }

        return null;
    }

}
