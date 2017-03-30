package com.leoh.webapp.net;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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

/**
 * Created by leoh on 2/2/17.
 */

public class WebParser {
    private static final String TAG = "appwebparser";
    private static final java.lang.String FILE_BLACKLIST = "blacklist";
    private static final java.lang.String FILE_WHITELIST = "whitelist";
    private static final java.lang.String FILE_ADHOSTS = "adhosts";
    private Document doc;
    private String url;
    private Map<String, List<String>> blackMap = new HashMap<>();
    private Properties whiteMap = new Properties();
    private Set<String> adsSet = new HashSet<>();
    private String whiteContent;
    private List<String> blackContent;

    public WebParser(Context c) {
        loadBlackList(c);
        loadWhiteList(c);
        loadBlockedDomains(c);
    }

    public String parse(String url, String html) throws Exception {
        if (setUrl(url)) {
            doc = Jsoup.parse(html, url, Parser.xmlParser());
            //System.out.println(html);
            String ret = getDoc();
            if (ret != null)
                return ret;
        }

        return html;
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
        if (blackContent != null ) {
            //Log.d(TAG, list.toString());
            for (String e : blackContent) {
                ele = doc.select(e);
                if (!ele.isEmpty())
                    ele.remove();
            }
            return doc.outerHtml();
        }

        return null;
    }

    public boolean isBlock(Uri uri) {
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

    public static String createHtmlMessage(String body) {
        return "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/><meta charset=”UTF-8″/>" +
                "<style>body{font-size:18px;display:block;background:#fff1e0 none repeat scroll 0 0}img {max-width:100%}</style></head>" +
                "<body >" + body + "</body></html>";
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
}
