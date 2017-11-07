package com.leoh.webapp.net;

import android.support.annotation.NonNull;

import java.util.Comparator;
import java.util.Date;

/**
 * Created by leoh on 3/26/17.
 */

public class WebHistoryItem implements Comparable<WebHistoryItem> {
    public final String url;
    public final String title;
    public final Date date;

    public WebHistoryItem(String url, String title) {
        this.url = url;
        this.title = title;
        date = new Date();
    }

    @Override
    public int compareTo(@NonNull WebHistoryItem webHistoryItem) {
        return webHistoryItem.date.compareTo(date);
    }
}
