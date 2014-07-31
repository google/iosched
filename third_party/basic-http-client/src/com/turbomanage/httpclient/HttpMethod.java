package com.turbomanage.httpclient;

/**
 * Enumerated type that represents an HTTP request method.
 * Besides the method name, it determines whether the client
 * should do the output phase of the connection.
 *  
 * @author David M. Chandler
 */
public enum HttpMethod {
    GET(true, false),
    POST(true, true),
    PUT(true, true),
    DELETE(true, false),
    HEAD(false, false);
    
    private boolean doInput;
    private boolean doOutput;
    
    private HttpMethod(boolean doInput, boolean doOutput) {
        this.doInput = doInput;
        this.doOutput = doOutput;
    }

    public boolean getDoInput() {
        return doInput;
    }

    /**
     * Whether the client should do the write phase, or just read
     * 
     * @return doOutput
     */
    public boolean getDoOutput() {
        return this.doOutput;
    }
    
    /**
     * Accessor method.
     * 
     * @return HTTP method name (GET, PUT, POST, DELETE)
     */
    public String getMethodName() {
        return this.toString();
    }

}
