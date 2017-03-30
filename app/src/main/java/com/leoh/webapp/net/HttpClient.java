package com.leoh.webapp.net;

import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceResponse;

import com.leoh.webapp.MyWebViewClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http2.Header;

import static okhttp3.CacheControl.FORCE_NETWORK;


/**
 * Created by leoh on 1/15/17.
 */

public class HttpClient {
    public static final  WebResourceResponse emptyRes = new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));;
    private static final String TAG = "appwebhttp";
    private static final String tmpImg = "http://127.0.0.1/img.png";
    private final OkHttpClient client;
    private final MyWebViewClient webClient;
    private final WebParser parser;
    private String userAgent = "";
    private Call calling;
    public WebCache webHostory;
    public WebCache webCache;

    public HttpClient(Context c, MyWebViewClient webViewClient) {
        webClient = webViewClient;
        WebInterceptor webInterceptor = new WebInterceptor();
        client = new OkHttpClient.Builder()
                .cookieJar(webInterceptor)
                .retryOnConnectionFailure(false)
                //.addInterceptor(webInterceptor)
                //.connectTimeout(15, TimeUnit.SECONDS)
                .build();
        parser = new WebParser(c);
        webHostory = new WebCache(c, "history.data");
        webCache = new WebCache(c, "cache.data");
    }

    public void setUserAgentString(String string) {
        userAgent = string;
    }

    public boolean loadUrl(final String url) {
        cancelRequest();
        if (getMimeType(url).startsWith("image")) {
            return false;
        }

        final Request request = new Request.Builder()
                .header("User-Agent", userAgent)
                .addHeader("Connection","close")
                .url(url)
                .build();
        calling = client.newCall(request);
        calling.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                WebResponse cache = webHostory.get(url);
                if (cache == null)
                    webClient.onLoadError("Load Error");
                else
                    webClient.loadDataWithBaseURL(cache.url, new String(cache.data), cache.mime, cache.encode);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Unexpected:" + response.code());
                    webClient.onLoadError(response.toString());
                    return;
                }

                WebResponse res = parseResponse(url, response);
                //Log.d(TAG, "resp=[" + res.mime + "]" + response.isRedirect());

                if (res.mime.startsWith("image")) {
                    webClient.loadUrl(url);
                } else {
                    String html = res.encode.isEmpty() ? new String(res.data) : new String(res.data, res.encode);
                    if (res.mime.startsWith("text/html") || res.mime.startsWith("application/xhtml")) {
                        try {
                            html = parser.parse(url, html);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    webClient.loadDataWithBaseURL(res.url, html, res.mime, res.encode);
                    res.data = html.getBytes();
                    webHostory.put(url, res);
                }
            }
        });

        return true;
    }

    public WebResourceResponse getWebResourceResponse(final Uri uri, Map<String, String> headers) {
        String url = uri.toString();
        if (url.endsWith("favicon.ico") || parser.isBlock(uri)) {
            return emptyRes;
        }

        WebResourceResponse response = getFromCache(url, webCache);
        if (response == null) {
            try {
                Response res = callRequest(url, headers);
                if (res.isSuccessful()) {
                    WebResponse cache = parseResponse(url, res);
                    if (cache != null) {
                        response = new WebResourceResponse(cache.mime, cache.encode,
                                new ByteArrayInputStream(cache.data));
                        Log.d(TAG, "resp=[" + cache.mime + "]");
                        if (isCacheUrl(url, cache.mime))
                            webCache.put(url, cache);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException:" + url);
            }
        }

        return response;
    }

    public WebResourceResponse geResponseFromCache(String url) {
        return getFromCache(url, webHostory);
    }

    public void cancelRequest() {
        if (calling != null)
            calling.cancel();
    }

    public void setTitle(String url, String title) {
        WebResponse w = webHostory.get(url);
        if (w != null) {
            w.title = title;
        }
    }

    private Response callRequest(final String url, Map<String, String> headers) throws IOException {
        Log.d(TAG, "REQ=" + url);
        Request.Builder builder = new Request.Builder()
                .addHeader("Connection", "close")
                .cacheControl(CacheControl.FORCE_NETWORK)
                .url(url);

        Set<String> keys = headers.keySet();
        for (String k : keys) {
            String v = headers.get(k);
            builder = builder.addHeader(k, v);
            //Log.d(TAG, k + "=" + v);
        }
        builder.header("User-Agent", userAgent);
        //Log.d(TAG, " Req =" + url);
        Request request = builder.build();
        calling = client.newCall(request);
        return calling.execute();
    }

    private WebResourceResponse getFromCache(String url, WebCache cache) {
        WebResponse resp = cache.get(url);
        if (resp != null) {
            Log.d(TAG, "HIT=" + url);
            return new WebResourceResponse(resp.mime, resp.encode, new ByteArrayInputStream(resp.data));
        }

        return null;
    }

    private boolean isCacheUrl(String url, String mime) {
        return mime.equals("text/css") ||
                mime.endsWith("javascript") ||
                mime.startsWith("image") ||
                mime.equals("application/font") ||
                mime.startsWith("text/plain");
    }

    private static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        if (type == null)
            type = "";
        return type;
    }

    private WebResponse parseResponse(final String url, final Response response) throws IOException {
        WebResponse resp = null;
        ResponseBody body = response.body();
        MediaType contentType = body.contentType();
        String encode = "";
        String type = null;

        if (contentType != null)
            type = contentType.type();
        if (type != null && !type.equals("application/octet-stream")) {
            Charset charset = contentType.charset();
            if (charset != null)
                encode = charset.name();
            String mime = type + "/" + contentType.subtype();
            String realUrl = response.request().url().toString();
            resp = new WebResponse(realUrl, mime, encode, body.bytes());
        }

        return resp;
    }
}
