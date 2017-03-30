package com.leoh.webapp.net;

import android.util.Log;
import android.webkit.CookieManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by leoh on 2/2/17.
 */

public class WebInterceptor implements Interceptor, CookieJar {
    private static final String TAG = "appweblog";
    private final CookieManager webviewCookieManager = CookieManager.getInstance();

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        long t1 = System.nanoTime();
        Log.d(TAG, String.format("Sending request %s on %s%n%s",
                request.url(), chain.connection(), request.headers()));

        Response response = chain.proceed(request);

        long t2 = System.nanoTime();
        Log.d(TAG, String.format("Received response for %s in %.1fms%n%s",
                response.request().url(), (t2 - t1) / 1e6d, response.headers()));
        webviewCookieManager.flush();
        return response;

    }
    //CookieJar
    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        String urlString = url.toString();

        for (Cookie cookie : cookies) {
            //Log.d(TAG, "<=" + cookie.toString());
            webviewCookieManager.setCookie(urlString, cookie.toString());
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        String urlString = url.toString();
        String cookiesString = webviewCookieManager.getCookie(urlString);

        if (cookiesString != null && !cookiesString.isEmpty()) {
            //We can split on the ';' char as the cookie manager only returns cookies
            //that match the url and haven't expired, so the cookie attributes aren't included
            String[] cookieHeaders = cookiesString.split(";");
            List<Cookie> cookies = new ArrayList<>(cookieHeaders.length);

            for (String header : cookieHeaders) {
                //Log.d(TAG, "=>" + header);
                if (header.indexOf('=') > 0) // crash?
                    cookies.add(Cookie.parse(url, header));
            }

            return cookies;
        }

        return Collections.emptyList();
    }
}
