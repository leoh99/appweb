package com.leoh.webapp;

import android.content.Context;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;

import static android.util.Log.d;

/**
 * Created by leoh on 1/13/17.
 */

public class AdvancedWebView extends WebView {
    private static final String TAG = "appwebview";
    public static final String URL_HOME = "file:///android_asset/www/index.html";
    private final float maxFling;
    private GestureDetector gd;
    private WebViewClient client;

    public AdvancedWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        setBackgroundColor(0x0);

        WebSettings webSettings = getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setUseWideViewPort(true);
        //webSettings.setLoadWithOverviewMode(true);
        webSettings.setMinimumFontSize(16);
        webSettings.setDefaultTextEncodingName("UTF-8");
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        //webSettings.setAppCacheEnabled(true);
        //webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        webSettings.setAllowUniversalAccessFromFileURLs(true);
        //webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setGeolocationEnabled(true);

        maxFling = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
        gd = new GestureDetector(context, new CustomGestureListener());
    }

    public void goHome() {
        File extStore = Environment.getExternalStorageDirectory();
        File myFile = new File(extStore.getAbsolutePath() + "/APPWEB/index.html");
        if(myFile.exists()){
            loadUrl("file://"+ myFile);
            d(TAG, myFile.toString());
        } else {
            loadUrl(URL_HOME);
        }
    }

    public void resume() {
        resumeTimers();
        onResume();
        Log.d(TAG, "resume");
    }

    public void pause() {
        pauseTimers();
        onPause();
        Log.d(TAG, "pause");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && canGoBack()) {
            goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gd.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
    }

    private class CustomGestureListener extends GestureDetector.SimpleOnGestureListener {
        private float dx;
        private float dy;
        private static final int POWER = 25;

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            dx = distanceX;
            dy = distanceY;
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            int powerX = (int) (velocityX * 100 / maxFling);
            int powerY = (int) (velocityY * 100 / maxFling);
            if (Math.abs(powerX) > Math.abs(powerY) ) {
                if (powerX < -POWER) {
                    goHome();
                } else if (powerX > POWER) {
                    goForward();
                }
                return true;
            }
            return false;
        }
    }
}
