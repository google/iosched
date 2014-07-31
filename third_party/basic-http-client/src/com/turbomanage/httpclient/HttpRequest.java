package com.turbomanage.httpclient;

/**
 * Holds data for an HTTP request to be made with the attached HTTP client.
 * 
 * @author David M. Chandler
 */
public abstract class HttpRequest {
    
    public static final String URLENCODED = "application/x-www-form-urlencoded;charset=UTF-8";
    public static final String MULTIPART = "multipart/form-data";
    
    protected String path = ""; // avoid null in URL
    protected HttpMethod httpMethod;
    protected String contentType;
    protected byte[] content;
    
    /**
     * Constructs a request with optional params appended
     * to the query string.
     * 
     * @param path
     * @param params
     */
    public HttpRequest(String path, ParameterMap params) {
        String queryString = null;
        if (path != null) {
            this.path = path;
        }
        if (params != null) {
            queryString = params.urlEncode();
            this.path += "?" + queryString;
        }
    }

    public String getPath() {
        return path;
    }
    
    public HttpMethod getHttpMethod() {
        return httpMethod;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public byte[] getContent() {
        return content;
    }

}
