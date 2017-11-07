package com.leoh.webapp.net;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.YuvImage;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.webkit.CookieManager;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by leoh on 2/2/17.
 */

public class WebParser {
    private static final String TAG = "appwebparser";
    private static final java.lang.String FILE_BLACKLIST = "blacklist";
    private static final java.lang.String FILE_WHITELIST = "whitelist";
    private static final java.lang.String FILE_ADHOSTS = "adhosts";
    private final CookieManager webviewCookieManager = CookieManager.getInstance();
    private Document doc;
    private String url;
    private Map<String, List<String>> blackMap = new HashMap<>();
    private Properties whiteMap = new Properties();
    private Set<String> adsSet = new HashSet<>();
    private List<Pattern> adsList = new ArrayList<>(32);
    private String whiteContent;
    private List<String> blackContent;

    public WebParser(Context c) {
        loadBlackList(c);
        loadWhiteList(c);
        loadBlockedDomains(c);
    }

    public static String createHtmlMessage(String body) {
        return "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/><meta charset=”UTF-8″/>" +
                "<style>body{font-size:18px;display:block;background:#fff1e0 none repeat scroll 0 0}img {max-width:100%}</style></head>" +
                "<body >" + body + "</body></html>";
    }

    private static void print(String msg, Object... args) {
        System.out.println(String.format(msg, args));
    }

    private static String trim(String s, int width) {
        if (s.length() > width)
            return s.substring(0, width - 1) + ".";
        else
            return s;
    }

    public WebResponse parseUrl(String url, String userAgent) throws IOException {
        setUrl(url);

        Connection conn = Jsoup.connect(url)
                .timeout(5000)
                .header("User-Agent", userAgent);
        String cookie = webviewCookieManager.getCookie(url);
        if (cookie != null) {
            //Log.d(TAG, "=>"+cookie);
            conn.header("Cookie", cookie);
        }

        Connection.Response res = conn.execute();
        Map<String, String> cookies = res.cookies();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            webviewCookieManager.setCookie(url, entry.getKey() + "=" + entry.getValue());
        }

        String type = res.contentType();
        String charset = res.charset();
        if (charset == null)
            charset = "UTF-8";
        doc = res.parse();
        String ret = getDoc();
        if (ret == null)
            ret = doc.html();
        WebResponse resp = new WebResponse(res.url().toString(), type, charset, ret.getBytes(charset));
        resp.title = doc.title();

        //listLinks();

        return resp;
    }

    public WebResponse loadUrl(String url) {
        WebResponse resp = null;
        try {
            Connection.Response response = Jsoup.connect(url)
                    .ignoreContentType(true).execute();
            resp = new WebResponse(url, response.contentType(), "", response.bodyAsBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return resp;
    }

    public boolean setUrl(String url) {
        this.url = url;
        Uri uri = Uri.parse(url);
        String host = uri.getHost();
        whiteContent = whiteMap.getProperty(host);
        blackContent = blackMap.get(host);
        return (whiteContent != null || blackContent != null);
    }

    private String getDoc() {
        Uri uri = Uri.parse(url);
        String host = uri.getHost();
        //Log.d(TAG, host + "      " + uri.getPath());

        if (whiteContent != null) {
            //Log.d(TAG, content);
            Elements div = doc.select(whiteContent);
            if (!div.isEmpty()) {
                return createHtmlMessage(div.toString());
            }
        }
        Elements ele;
        if (blackContent != null) {
            for (String e : blackContent) {
                ele = doc.select(e);
                if (!ele.isEmpty()) {
                    //Log.d(TAG, ele.toString());
                    ele.remove();
                }
            }
            return doc.html();
        }

        return null;
    }

    public boolean isBlock(Uri uri) {
        //String host = uri.getHost();
        //Log.d(TAG, "Req=" + uri);
        for (Pattern p : adsList) {
            Matcher m = p.matcher(uri.toString());
            if (m.find()) {
                //Log.d(TAG, p.toString());
                return true;
            }
        }

        return false;
    }

    public boolean isBlock0(Uri uri) {
        boolean block = false;
        String host = uri.getHost();


        if (host != null) {
            block = adsSet.contains(host);
            if (!block && !host.isEmpty()) {
                int n = host.indexOf(".");
                if (n > 0) {
                    String sub = host.substring(n);
                    block = adsSet.contains(sub);
                }
            }
        }

        return block;
    }

    private void loadWhiteList(Context context) {
        InputStream in = null;
        File file = getExternalFile(FILE_WHITELIST);
        try {
            if (file.exists()) {
                Log.d(TAG, "Load external list");
                in = new FileInputStream(file);
            } else {
                AssetManager asset = context.getAssets();
                in = asset.open(FILE_WHITELIST);
            }
            whiteMap.load(in);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadBlackList(Context context) {
        BufferedReader reader = null;
        File file = getExternalFile(FILE_BLACKLIST);
        try {
            if (file.exists()) {
                Log.d(TAG, "Load external list");
                reader = new BufferedReader(new FileReader(file));
            } else {
                AssetManager asset = context.getAssets();
                reader = new BufferedReader(new InputStreamReader(
                        asset.open(FILE_BLACKLIST)));
            }

            String line, tag, attr;
            ArrayList<String> list = null;
            Scanner scan;
            StringBuilder text;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;
                if (line.startsWith("[")) {
                    list = new ArrayList<>();
                    tag = line.substring(1, line.length() - 1);
                    blackMap.put(tag, list);
                    //Log.d(TAG, tag);
                } else {
                    scan = new Scanner(line);
                    tag = scan.next();
                    attr = scan.next();
                    text = new StringBuilder();
                    if (!tag.startsWith("*"))
                        text.append(tag);

                    if (!attr.startsWith("*")) {
                        text.append("[");
                        text.append(attr);
                        text.append("]");
                    }

                    list.add(text.toString());
                    //Log.d(TAG, text.toString());
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadBlockedDomains(Context context) {
        BufferedReader reader = null;
        File file = getExternalFile(FILE_ADHOSTS);

        try {
            if (file.exists()) {
                Log.d(TAG, "Load external list");
                reader = new BufferedReader(new FileReader(file));
            } else {
                AssetManager asset = context.getAssets();
                reader = new BufferedReader(new InputStreamReader(asset.open(FILE_ADHOSTS)));
            }

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                adsSet.add(line);
                Log.d(TAG, line);
                adsList.add(Pattern.compile(line));
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private File getExternalFile(String fileName) {
        File extStore = Environment.getExternalStorageDirectory();
        return new File(extStore.getAbsolutePath() + "/APPWEB/" + fileName);
    }

    private void listLinks() {
        //Elements links = doc.select("a[href]");
        Elements scripts = doc.select("script");
        Elements imports = doc.select("[src]");

        print("\n%s [%s]", TAG, url);
        print("\nScripts: (%d)", scripts.size());
        for (Element src : scripts) {
            print("%s  * %s: <%s>", TAG, src.tagName(), src.attr("abs:src"));
        }

        print("\n%s Imports: (%d)", TAG, imports.size());
        for (Element link : imports) {
            print("%s  * %s <%s>", TAG, link.tagName(), link.attr("abs:src"));
        }

//        print("\nLinks: (%d)", links.size());
//        for (Element link : links) {
//            print(" * a: <%s>  (%s)", link.attr("abs:href"), trim(link.text(), 35));
//        }

    }

}
