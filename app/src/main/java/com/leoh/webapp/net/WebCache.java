package com.leoh.webapp.net;

import android.content.Context;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Created by leoh on 2/5/17.
 */

public class WebCache {
    private static final String TAG = "appwebcache";
    private final Context context;
    private final String fileName;
    private Map<String, WebResponse> cacheMap = new HashMap<String, WebResponse>();

    public WebCache(Context c, String name) {
        context = c;
        fileName = name;
        load();
     }

    public void put(String url, WebResponse res) {
        synchronized (cacheMap) {
            cacheMap.put(url, res);
        }
    }

    public WebResponse get(String url) {
        return cacheMap.get(url);
    }

    public void clear() {
        synchronized (cacheMap) {
            cacheMap.clear();
        }
        Log.d(TAG, "Cache cleared");
    }

    public List<WebHistoryItem> getHistory() {
        List<WebHistoryItem> list = new ArrayList();
        synchronized (cacheMap) {
            Set<String> keys = cacheMap.keySet();
            for (String key : keys) {
                list.add(new WebHistoryItem(key, cacheMap.get(key).title));
            }
        }
        return list;
    }

    public List<String> getCache() {
        List<String> list;
        synchronized (cacheMap) {
            list = new ArrayList<String>(cacheMap.keySet());
        }

        return list;
    }

    public void load() {
        try {
            FileInputStream fileIn = context.openFileInput(fileName);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            cacheMap = (HashMap<String, WebResponse>) in.readObject();
            in.close();
            fileIn.close();
            Log.d(TAG, "Cache  loaded:" + fileName);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void save() {
        synchronized (cacheMap) {
            try {
                FileOutputStream fileOut = context.openFileOutput(fileName, Context.MODE_PRIVATE);
                ObjectOutputStream out = new ObjectOutputStream(fileOut);
                out.writeObject(cacheMap);
                out.close();
                fileOut.close();
                Log.d(TAG, "Cache saved:" + fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
