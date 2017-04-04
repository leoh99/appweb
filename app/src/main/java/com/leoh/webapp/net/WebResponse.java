package com.leoh.webapp.net;

import java.io.Serializable;

/**
 * Created by leoh on 2/5/17.
 */

public class WebResponse implements Serializable {
    public final String url;
    public String mime;
    public String encode;
    public byte[] data;
    public String title;
    public int cacheHit;

    public WebResponse(String url, String mime, String encode, byte[] data) {
        this.mime = mime;
        this.url = url;
        this.encode = encode;
        this.data = data;
        cacheHit = 0;
    }
}
