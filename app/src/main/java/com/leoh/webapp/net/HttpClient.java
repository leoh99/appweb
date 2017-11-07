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
                    webHostory.put(url, res);
                    webClient.loadDataWithBaseURL(res.url, new String(res.data), res.mime, res.encode);
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                    webClient.loadUrl(url);
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

        if (response == null && url.endsWith("favicon.ico")) {
            WebResponse res = parser.loadUrl(url);
            if (res != null) {
                response = new WebResourceResponse(res.mime, res.encode,
                        new ByteArrayInputStream(res.data));
                //Log.d(TAG, "resp=[" + cache.mime + "]");
                webCache.put(url, res);
            }
        }

        return response;
    }

    private WebResourceResponse getFromCache(String url, WebCache cache) {
        WebResponse resp = cache.get(url);
        if (resp != null) {
            //Log.d(TAG, "HIT=" + url);
            resp.cacheHit++;
            return new WebResourceResponse(resp.mime, resp.encode, new ByteArrayInputStream(resp.data));
        }

        return null;
    }

}
