
package com.turbomanage.httpclient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Default {@link RequestHandler} used by {@link BasicHttpClient}. It is
 * intended to be used for simple requests with small amounts of data only (a
 * few kB), as it does no buffering, chunking, streaming, etc. Only character
 * set supported is UTF-8. Only {@link String} content is supported. All
 * responses are treated as {@link String}s. This class is abstract so that
 * it can be easily extended in an anonymous inner class when constructing
 * a client.
 *
 * @author David M. Chandler
 */
public abstract class BasicRequestHandler implements RequestHandler {

    private final RequestLogger logger;

    /**
     * Constructs a handler with default logger.
     */
    public BasicRequestHandler() {
        this(new ConsoleRequestLogger());
    }

    /**
     * Constructs a handler with supplied logger.
     *
     * @param logger
     */
    public BasicRequestHandler(RequestLogger logger) {
        this.logger = logger;
    }

    @Override
    public HttpURLConnection openConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection uc = (HttpURLConnection) url.openConnection();
        return uc;
    }

    @Override
    public void prepareConnection(HttpURLConnection urlConnection, HttpMethod httpMethod,
            String contentType) throws IOException {
        // Configure connection for request method
        urlConnection.setRequestMethod(httpMethod.getMethodName());
        urlConnection.setDoOutput(httpMethod.getDoOutput());
        urlConnection.setDoInput(httpMethod.getDoInput());
        if (contentType != null) {
            urlConnection.setRequestProperty("Content-Type", contentType);
        }
        // Set additional properties
        urlConnection.setRequestProperty("Accept-Charset", UTF8);
    }

    @Override
    public OutputStream openOutput(HttpURLConnection urlConnection)
    		throws IOException {
    		return urlConnection.getOutputStream();
    }

    @Override
    public void writeStream(OutputStream out, byte[] content) throws IOException {
        out.write(content);
    }

    @Override
    public InputStream openInput(HttpURLConnection urlConnection)
    		throws IOException {
    		return urlConnection.getInputStream();
    }

    @Override
    public byte[] readStream(InputStream in) throws IOException {
        int nRead;
        byte[] data = new byte[16384];
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        while ((nRead = in.read(data)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    @Override
    public boolean onError(HttpRequestException e) {
        HttpResponse res = e.getHttpResponse();
        if (logger.isLoggingEnabled()) {
            logger.log("BasicRequestHandler.onError got");
            e.printStackTrace();
        }
        if (res != null) {
            int status = res.getStatus();
            if (status > 0) {
                // Perhaps a 404, 501, or something that will be fixed later
                return true;
            }
        }
        // Connection refused, host unreachable, etc.
        return false;
    }

}
