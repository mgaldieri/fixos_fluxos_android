/*
 * Copyright (C) 2015 Neo Visionaries Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.neovisionaries.ws.client;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;


/**
 * Factory to create {@link WebSocket} instances.
 */
public class WebSocketFactory
{
    private final SocketFactorySettings mSocketFactorySettings;
    private final ProxySettings mProxySettings;
    private int mConnectionTimeout;


    public WebSocketFactory()
    {
        mSocketFactorySettings = new SocketFactorySettings();
        mProxySettings         = new ProxySettings(this);
    }


    /**
     * Get the socket factory that has been set by {@link
     * #setSocketFactory(SocketFactory)}.
     *
     * @return
     *         The socket factory.
     */
    public SocketFactory getSocketFactory()
    {
        return mSocketFactorySettings.getSocketFactory();
    }


    /**
     * Set a socket factory.
     * See {@link #createSocket(URI)} for details.
     *
     * @param factory
     *         A socket factory.
     *
     * @return
     *         {@code this} instance.
     */
    public WebSocketFactory setSocketFactory(SocketFactory factory)
    {
        mSocketFactorySettings.setSocketFactory(factory);

        return this;
    }


    /**
     * Get the SSL socket factory that has been set by {@link
     * #setSSLSocketFactory(SSLSocketFactory)}.
     *
     * @return
     *         The SSL socket factory.
     */
    public SSLSocketFactory getSSLSocketFactory()
    {
        return mSocketFactorySettings.getSSLSocketFactory();
    }


    /**
     * Set an SSL socket factory.
     * See {@link #createSocket(URI)} for details.
     *
     * @param factory
     *         An SSL socket factory.
     *
     * @return
     *         {@code this} instance.
     */
    public WebSocketFactory setSSLSocketFactory(SSLSocketFactory factory)
    {
        mSocketFactorySettings.setSSLSocketFactory(factory);

        return this;
    }


    /**
     * Get the SSL context that has been set by {@link #setSSLContext(SSLContext)}.
     *
     * @return
     *         The SSL context.
     */
    public SSLContext getSSLContext()
    {
        return mSocketFactorySettings.getSSLContext();
    }


    /**
     * Set an SSL context to get a socket factory.
     * See {@link #createSocket(URI)} for details.
     *
     * @param context
     *         An SSL context.
     *
     * @return
     *         {@code this} instance.
     */
    public WebSocketFactory setSSLContext(SSLContext context)
    {
        mSocketFactorySettings.setSSLContext(context);

        return this;
    }


    /**
     * Get the proxy settings.
     *
     * @return
     *         The proxy settings.
     *
     * @since 1.3
     *
     * @see ProxySettings
     */
    public ProxySettings getProxySettings()
    {
        return mProxySettings;
    }


    /**
     * Get the timeout value in milliseconds for socket connection.
     * The default value is 0 and it means an infinite timeout.
     *
     * <p>
     * When a {@code createSocket} method which does not have {@code
     * timeout} argument is called, the value returned by this method
     * is used as a timeout value for socket connection.
     * </p>
     *
     * @return
     *         The connection timeout value in milliseconds.
     *
     * @since 1.10
     */
    public int getConnectionTimeout()
    {
        return mConnectionTimeout;
    }


    /**
     * Set the timeout value in milliseconds for socket connection.
     * A timeout of zero is interpreted as an infinite timeout.
     *
     * @param timeout
     *         The connection timeout value in milliseconds.
     *
     * @return
     *         {@code this} object.
     *
     * @throws IllegalArgumentException
     *         The given timeout value is negative.
     *
     * @since 1.10
     */
    public WebSocketFactory setConnectionTimeout(int timeout)
    {
        if (timeout < 0)
        {
            throw new IllegalArgumentException("timeout value cannot be negative.");
        }

        mConnectionTimeout = timeout;

        return this;
    }


    /**
     * Create a web socket.
     *
     * <p>
     * This method is an alias of {@link #createSocket(String, int)
     * createSocket}{@code (uri, }{@link #getConnectionTimeout()}{@code )}.
     * </p>
     *
     * @param uri
     *         The URI of the web socket endpoint on the server side.
     *
     * @return
     *         A web socket.
     *
     * @throws IllegalArgumentException
     *         The given URI is {@code null} or violates RFC 2396.
     *
     * @throws IOException
     *         Failed to create a socket. Or, HTTP proxy handshake or SSL
     *         handshake failed.
     */
    public WebSocket createSocket(String uri) throws IOException
    {
        return createSocket(uri, getConnectionTimeout());
    }


    /**
     * Create a web socket.
     *
     * <p>
     * This method is an alias of {@link #createSocket(URI, int) createSocket}{@code
     * (}{@link URI#create(String) URI.create}{@code (uri), timeout)}.
     * </p>
     *
     * @param uri
     *         The URI of the web socket endpoint on the server side.
     *
     * @param timeout
     *         The timeout value in milliseconds for socket connection.
     *         A timeout of zero is interpreted as an infinite timeout.
     *
     * @return
     *         A web socket.
     *
     * @throws IllegalArgumentException
     *         The given URI is {@code null} or violates RFC 2396, or
     *         the given timeout value is negative.
     *
     * @throws IOException
     *         Failed to create a socket. Or, HTTP proxy handshake or SSL
     *         handshake failed.
     *
     * @since 1.10
     */
    public WebSocket createSocket(String uri, int timeout) throws IOException
    {
        if (uri == null)
        {
            throw new IllegalArgumentException("The given URI is null.");
        }

        if (timeout < 0)
        {
            throw new IllegalArgumentException("The given timeout value is negative.");
        }

        return createSocket(URI.create(uri), timeout);
    }


    /**
     * Create a web socket.
     *
     * <p>
     * This method is an alias of {@link #createSocket(URL, int) createSocket}{@code
     * (url, }{@link #getConnectionTimeout()}{@code )}.
     * </p>
     *
     * @param url
     *         The URL of the web socket endpoint on the server side.
     *
     * @return
     *         A web socket.
     *
     * @throws IllegalArgumentException
     *         The given URL is {@code null} or failed to be converted into a URI.
     *
     * @throws IOException
     *         Failed to create a socket. Or, HTTP proxy handshake or SSL
     *         handshake failed.
     */
    public WebSocket createSocket(URL url) throws IOException
    {
        return createSocket(url, getConnectionTimeout());
    }


    /**
     * Create a web socket.
     *
     * <p>
     * This method is an alias of {@link #createSocket(URI, int) createSocket}{@code
     * (url.}{@link URL#toURI() toURI()}{@code , timeout)}.
     * </p>
     *
     * @param url
     *         The URL of the web socket endpoint on the server side.
     *
     * @param timeout
     *         The timeout value in milliseconds for socket connection.
     *
     * @return
     *         A web socket.
     *
     * @throws IllegalArgumentException
     *         The given URL is {@code null} or failed to be converted into a URI,
     *         or the given timeout value is negative.
     *
     * @throws IOException
     *         Failed to create a socket. Or, HTTP proxy handshake or SSL
     *         handshake failed.
     *
     * @since 1.10
     */
    public WebSocket createSocket(URL url, int timeout) throws IOException
    {
        if (url == null)
        {
            throw new IllegalArgumentException("The given URL is null.");
        }

        if (timeout < 0)
        {
            throw new IllegalArgumentException("The given timeout value is negative.");
        }

        try
        {
            return createSocket(url.toURI(), timeout);
        }
        catch (URISyntaxException e)
        {
            throw new IllegalArgumentException("Failed to convert the given URL into a URI.");
        }
    }


    /**
     * Create a web socket. This method is an alias of {@link #createSocket(URI, int)
     * createSocket}{@code (uri, }{@link #getConnectionTimeout()}{@code )}.
     *
     * <p>
     * A socket factory (= a {@link SocketFactory} instance) to create a raw
     * socket (= a {@link Socket} instance) is determined as described below.
     * </p>
     *
     * <ol>
     * <li>
     *   If the scheme of the URI is either {@code wss} or {@code https},
     *   <ol type="i">
     *     <li>
     *       If an {@link SSLContext} instance has been set by {@link
     *       #setSSLContext(SSLContext)}, the value returned from {@link
     *       SSLContext#getSocketFactory()} method of the instance is used.
     *     <li>
     *       Otherwise, if an {@link SSLSocketFactory} instance has been
     *       set by {@link #setSSLSocketFactory(SSLSocketFactory)}, the
     *       instance is used.
     *     <li>
     *       Otherwise, the value returned from {@link SSLSocketFactory#getDefault()}
     *       is used.
     *   </ol>
     * <li>
     *   Otherwise (= the scheme of the URI is either {@code ws} or {@code http}),
     *   <ol type="i">
     *     <li>
     *       If a {@link SocketFactory} instance has been set by {@link
     *       #setSocketFactory(SocketFactory)}, the instance is used.
     *     <li>
     *       Otherwise, the value returned from {@link SocketFactory#getDefault()}
     *       is used.
     *   </ol>
     * </ol>
     *
     * @param uri
     *         The URI of the web socket endpoint on the server side.
     *         The scheme part of the URI must be one of {@code ws},
     *         {@code wss}, {@code http} and {@code https}
     *         (case-insensitive).
     *
     * @return
     *         A web socket.
     *
     * @throws IllegalArgumentException
     *         The given URI is {@code null} or violates RFC 2396.
     *
     * @throws IOException
     *         Failed to create a socket. Or, HTTP proxy handshake or SSL
     *         handshake failed.
     */
    public WebSocket createSocket(URI uri) throws IOException
    {
        return createSocket(uri, getConnectionTimeout());
    }


    /**
     * Create a web socket.
     *
     * <p>
     * A socket factory (= a {@link SocketFactory} instance) to create a raw
     * socket (= a {@link Socket} instance) is determined as described below.
     * </p>
     *
     * <ol>
     * <li>
     *   If the scheme of the URI is either {@code wss} or {@code https},
     *   <ol type="i">
     *     <li>
     *       If an {@link SSLContext} instance has been set by {@link
     *       #setSSLContext(SSLContext)}, the value returned from {@link
     *       SSLContext#getSocketFactory()} method of the instance is used.
     *     <li>
     *       Otherwise, if an {@link SSLSocketFactory} instance has been
     *       set by {@link #setSSLSocketFactory(SSLSocketFactory)}, the
     *       instance is used.
     *     <li>
     *       Otherwise, the value returned from {@link SSLSocketFactory#getDefault()}
     *       is used.
     *   </ol>
     * <li>
     *   Otherwise (= the scheme of the URI is either {@code ws} or {@code http}),
     *   <ol type="i">
     *     <li>
     *       If a {@link SocketFactory} instance has been set by {@link
     *       #setSocketFactory(SocketFactory)}, the instance is used.
     *     <li>
     *       Otherwise, the value returned from {@link SocketFactory#getDefault()}
     *       is used.
     *   </ol>
     * </ol>
     *
     * @param uri
     *         The URI of the web socket endpoint on the server side.
     *         The scheme part of the URI must be one of {@code ws},
     *         {@code wss}, {@code http} and {@code https}
     *         (case-insensitive).
     *
     * @param timeout
     *         The timeout value in milliseconds for socket connection.
     *
     * @return
     *         A web socket.
     *
     * @throws IllegalArgumentException
     *         The given URI is {@code null} or violates RFC 2396, or
     *         the given timeout value is negative.
     *
     * @throws IOException
     *         Failed to create a socket. Or, HTTP proxy handshake or SSL
     *         handshake failed.
     *
     * @since 1.10
     */
    public WebSocket createSocket(URI uri, int timeout) throws IOException
    {
        if (uri == null)
        {
            throw new IllegalArgumentException("The given URI is null.");
        }

        if (timeout < 0)
        {
            throw new IllegalArgumentException("The given timeout value is negative.");
        }

        // Split the URI.
        String scheme   = uri.getScheme();
        String userInfo = uri.getUserInfo();
        String host     = uri.getHost();
        int port        = uri.getPort();
        String path     = uri.getRawPath();
        String query    = uri.getRawQuery();

        return createSocket(scheme, userInfo, host, port, path, query, timeout);
    }


    private WebSocket createSocket(
        String scheme, String userInfo, String host, int port, String path, String query, int timeout) throws IOException
    {
        // True if 'scheme' is 'wss' or 'https'.
        boolean secure = isSecureConnectionRequired(scheme);

        // Check if 'host' is specified.
        if (host == null || host.length() == 0)
        {
            throw new IllegalArgumentException("The host part is empty.");
        }

        // Determine the path.
        path = determinePath(path);

        // Create a Socket instance.
        Socket socket = createRawSocket(host, port, secure, timeout);

        // Create a WebSocket instance.
        return createWebSocket(secure, userInfo, host, port, path, query, socket, timeout);
    }



    private static boolean isSecureConnectionRequired(String scheme)
    {
        if (scheme == null || scheme.length() == 0)
        {
            throw new IllegalArgumentException("The scheme part is empty.");
        }

        if ("wss".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
        {
            return true;
        }

        if ("ws".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
        {
            return false;
        }

        throw new IllegalArgumentException("Bad scheme: " + scheme);
    }


    private static String determinePath(String path)
    {
        if (path == null || path.length() == 0)
        {
            return "/";
        }

        if (path.startsWith("/"))
        {
            return path;
        }
        else
        {
            return "/" + path;
        }
    }


    private Socket createRawSocket(String host, int port, boolean secure, int timeout) throws IOException
    {
        // Determine the port number. Especially, if 'port' is -1,
        // it is converted to 80 or 443.
        port = determinePort(port, secure);

        // True if a proxy server should be used.
        boolean proxied = (mProxySettings.getHost() != null);

        // See "Figure 2 -- Proxy server traversal decision tree" at
        // http://www.infoq.com/articles/Web-Sockets-Proxy-Servers

        if (proxied)
        {
            // Connect to the proxy server and perform the proxy handshake.
            // As necessary, perform the SSL handshake in the tunnel.
            return createProxiedRawSocket(host, port, secure, timeout);
        }
        else
        {
            // Connect to the WebSocket endpoint directly.
            return createDirectRawSocket(host, port, secure, timeout);
        }
    }


    private Socket createProxiedRawSocket(String host, int port, boolean secure, int timeout) throws IOException
    {
        // Determine the port number of the proxy server.
        // Especially, if getPort() returns -1, the value
        // is converted to 80 or 443.
        int proxyPort = determinePort(mProxySettings.getPort(), mProxySettings.isSecure());

        // Select a socket factory.
        SocketFactory factory = mProxySettings.selectSocketFactory();

        // Let the socket factory create a socket.
        Socket socket = factory.createSocket();

        // Connect to the host.
        socket.connect(new InetSocketAddress(mProxySettings.getHost(), proxyPort), timeout);

        try
        {
            // Perform proxy handshake (and SSL handshake as necessary).
            return doProxyHandshake(socket, host, port, secure);
        }
        catch (IOException e)
        {
            try
            {
                socket.close();
            }
            catch (IOException ioe)
            {
            }

            throw e;
        }
    }


    private Socket doProxyHandshake(Socket socket, String host, int port, boolean secure) throws IOException
    {
        // Delegate the task to ProxyHandshaker.
        new ProxyHandshaker(socket, host, port, mProxySettings).perform();

        // If TLS handshake is needed in the tunnel.
        if (secure)
        {
            // Get an SSL socket factory to create an SSL socket which
            // performs the SSL handshake with the WebSocket endpoint.
            SSLSocketFactory sslSocketFactory =
                (SSLSocketFactory)mSocketFactorySettings.selectSocketFactory(secure);

            // Overlay the existing socket.
            socket = sslSocketFactory.createSocket(socket, host, port, true);

            // Start the SSL handshake manually. As for the reason, see
            // http://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/samples/sockets/client/SSLSocketClient.java
            ((SSLSocket)socket).startHandshake();
        }

        return socket;
    }


    private Socket createDirectRawSocket(String host, int port, boolean secure, int timeout) throws IOException
    {
        // Select a socket factory.
        SocketFactory factory = mSocketFactorySettings.selectSocketFactory(secure);

        // Let the socket factory create a socket.
        Socket socket = factory.createSocket();

        // Connect to the host.
        socket.connect(new InetSocketAddress(host, port), timeout);

        return socket;
    }


    private static int determinePort(int port, boolean secure)
    {
        if (0 <= port)
        {
            return port;
        }

        if (secure)
        {
            return 443;
        }
        else
        {
            return 80;
        }
    }


    private WebSocket createWebSocket(
        boolean secure, String userInfo, String host, int port, String path, String query,
        Socket socket, int timeout)
    {
        // The value for "Host" HTTP header.
        if (0 <= port)
        {
            host = host + ":" + port;
        }

        // The value for Request-URI of Request-Line.
        if (query != null)
        {
            path = path + "?" + query;
        }

        return new WebSocket(this, secure, userInfo, host, path, socket, timeout);
    }
}
