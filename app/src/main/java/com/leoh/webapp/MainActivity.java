package com.leoh.webapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

import static com.leoh.webapp.R.id.webView;

public class MainActivity extends AppCompatActivity {
    private MyWebViewClient webclient;
    private AdvancedWebView webview;
    private ClipboardManager clipboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        setupWebView();

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_VIEW.equals(action)) {
            Uri uri = intent.getData();
            webclient.shouldOverrideUrlLoading(webview, uri.toString());
        } else if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent); // Handle text being sent
            } else if (type.startsWith("image/")) {
                handleSendImage(intent); // Handle single image being sent
            }
        } else {
            webview.goHome();
        }

    }

    private void handleSendImage(Intent intent) {
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            // Update UI to reflect image being shared
            Toast.makeText(getApplicationContext(), imageUri.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            String url = sharedText.startsWith("http")?
                    sharedText : "http://www.linguee.com/english-chinese/translation/"+sharedText+ ".html?cw=336";
            webview.loadUrl(url);
        }
    }

    private void setupWebView() {
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setMax(100);
        webview = (AdvancedWebView) findViewById(webView);
        registerForContextMenu(webview);

        webclient= new MyWebViewClient(this, webview, progressBar);
        webview.setWebViewClient(webclient);
        webview.addJavascriptInterface(webclient, "Android");
        webview.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                progressBar.setProgress(progress);
                if (progress == progressBar.getMax())
                    progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                super.onGeolocationPermissionsShowPrompt(origin, callback);
                callback.invoke(origin, true, false);
            }
        });
    }

    private void copyClipData(String data) {
        ClipData clip = ClipData.newPlainText("label", data);
        clipboard.setPrimaryClip(clip);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        WebView w = (WebView)v;
        WebView.HitTestResult result = w.getHitTestResult();
        //Only detect image format
        if(result.getType() == WebView.HitTestResult.IMAGE_TYPE){
           // menu.addSubMenu(1, 1, 1, "Scan");
            String strUrl = result.getExtra();
            copyClipData(strUrl);
            Toast.makeText(getApplicationContext(), strUrl, Toast.LENGTH_SHORT).show();
        } else if (result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE){
            //Through result.getExtra (URL) removal
            String strUrl = result.getExtra();
            copyClipData(strUrl);
            Toast.makeText(getApplicationContext(), strUrl, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(webview != null)
            webview.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(webview != null)
            webview.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webclient != null)
            webclient.saveData();
        if(webview != null){
            webview.stopLoading();
            webview.setWebChromeClient(null);
            webview.setWebViewClient(null);
            webview.destroy();
            webview = null;
        }
    }
}
