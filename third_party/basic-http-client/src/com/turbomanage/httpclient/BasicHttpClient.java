
package com.turbomanage.httpclient;


/**
 * Minimal HTTP client that facilitates simple GET, POST, PUT, and DELETE
 * requests. To implement buffering, streaming, or set other request properties,
 * set an alternate {@link RequestHandler}.
 *
 * <p>Sample usage:</p>
 * <pre>
 *    BasicHttpClient httpClient = new BasicHttpClient("http://www.google.com");
 *    ParameterMap params = httpClient.newParams().add("q", "GOOG");
 *    HttpResponse httpResponse = httpClient.get("/finance", params);
 *    System.out.println(httpResponse.getBodyAsString());
 * </pre>
 *
 * @author David M. Chandler
 */
public class BasicHttpClient extends AbstractHttpClient {

    static {
		ensureCookieManager();
    }

    /**
     * Constructs the default client with empty baseUrl.
     */
    public BasicHttpClient() {
        this("");
    }

    /**
     * Constructs the default client with baseUrl.
     *
     * @param baseUrl
     */
    public BasicHttpClient(String baseUrl) {
        this(baseUrl, new BasicRequestHandler() {
        });
    }

    /**
     * Constructs a client with baseUrl and custom {@link RequestHandler}.
     *
     * @param baseUrl
     * @param requestHandler
     */
    public BasicHttpClient(String baseUrl, RequestHandler requestHandler) {
        super(baseUrl, requestHandler);
    }

}
