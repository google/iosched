package com.turbomanage.httpclient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

/**
 * Minimal representation of the raw HTTP response copied from {@link HttpURLConnection}. 
 * 
 * @author David M. Chandler
 */
public class HttpResponse {
    
    private int status;
    private String url;
    private Map<String, List<String>> headers;
    private byte[] body;
    
    public HttpResponse(HttpURLConnection urlConnection, byte[] body) {
        try {
            this.status = urlConnection.getResponseCode();
            this.url = urlConnection.getURL().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.headers = urlConnection.getHeaderFields();
        this.body = body;
    }
    
    public int getStatus() {
        return status;
    }
    
    public String getUrl() {
        return url;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }
    
    public byte[] getBody() {
        return body;
    }

    public String getBodyAsString() {
        if (body != null) {
            return new String(body);
        }
        return null;
    }
    
}
