/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package apache.commons.net;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

/**
 * The SocketClient provides the basic operations that are required of client objects accessing sockets. It is meant to be subclassed to avoid having to rewrite
 * the same code over and over again to open a socket, close a socket, set timeouts, etc. All classes derived from SocketClient should use the {@link #_socketFactory_ _socketFactory_ } member variable to
 * create Socket and ServerSocket instances rather than instantiating them by directly invoking a constructor. By honoring this contract you guarantee that a
 * user will always be able to provide his own Socket implementations by substituting his own SocketFactory.
 *
 * @see SocketFactory
 */

public abstract class SocketClient {

    /**
     * The end of line character sequence used by most IETF protocols. That is a carriage return followed by a newline: "\r\n"
     */
    public static final String NETASCII_EOL = "\r\n";

    /** The default SocketFactory shared by all SocketClient instances. */
    private static final SocketFactory DEFAULT_SOCKET_FACTORY = SocketFactory.getDefault();

    /** The default {@link ServerSocketFactory} */
    private static final ServerSocketFactory DEFAULT_SERVER_SOCKET_FACTORY = ServerSocketFactory.getDefault();

    /** The socket's connect timeout (0 = infinite timeout) */
    private static final int DEFAULT_CONNECT_TIMEOUT = 60000;

    /** The timeout to use after opening a socket. */
    protected int _timeout_;

    /** The socket used for the connection. */
    protected Socket _socket_;

    /** The hostname used for the connection (null = no hostname supplied). */
    protected String _hostname_;

    /** The remote socket address used for the connection. */
    protected InetSocketAddress remoteInetSocketAddress;

    /** The default port the client should connect to. */
    protected int _defaultPort_;

    /** The socket's InputStream. */
    protected InputStream _input_;

    /** The socket's OutputStream. */
    protected OutputStream _output_;

    /** The socket's SocketFactory. */
    protected SocketFactory _socketFactory_;

    /** The socket's ServerSocket Factory. */
    protected ServerSocketFactory _serverSocketFactory_;

    /**
     * Defaults to {@link #DEFAULT_CONNECT_TIMEOUT}.
     */
    protected int connectTimeout = DEFAULT_CONNECT_TIMEOUT;

    /** Hint for SO_RCVBUF size */
    private int receiveBufferSize = -1;

    /** Hint for SO_SNDBUF size */
    private int sendBufferSize = -1;

    /**
     * Default constructor for SocketClient. Initializes _socket_ to null, _timeout_ to 0, _defaultPort to 0, _isConnected_ to false, charset to
     * {@code Charset.defaultCharset()}
     * and _socketFactory_ to a shared instance of DefaultSocketFactory
     */
    public SocketClient() {
        _socket_ = null;
        _hostname_ = null;
        _input_ = null;
        _output_ = null;
        _timeout_ = 0;
        _defaultPort_ = 0;
        _socketFactory_ = DEFAULT_SOCKET_FACTORY;
        _serverSocketFactory_ = DEFAULT_SERVER_SOCKET_FACTORY;
    }

    // helper method to allow code to be shared with connect(String,...) methods
    private void _connect(final InetSocketAddress remoteInetSocketAddress, final InetAddress localAddr, final int localPort) throws IOException {
        this.remoteInetSocketAddress = remoteInetSocketAddress;
        _socket_ = _socketFactory_.createSocket();
        if (receiveBufferSize != -1) {
            _socket_.setReceiveBufferSize(receiveBufferSize);
        }
        if (sendBufferSize != -1) {
            _socket_.setSendBufferSize(sendBufferSize);
        }
        if (localAddr != null) {
            _socket_.bind(new InetSocketAddress(localAddr, localPort));
        }
        _socket_.connect(remoteInetSocketAddress, connectTimeout);
        _connectAction_();
    }

    /**
     * Because there are so many connect() methods, the _connectAction_() method is provided as a means of performing some action immediately after establishing
     * a connection, rather than reimplementing all the connect() methods. The last action performed by every connect() method after opening a socket is to
     * call this method.
     * <p>
     * This method sets the timeout on the just opened socket to the default timeout set by {@link #setDefaultTimeout setDefaultTimeout() }, sets _input_ and
     * _output_ to the socket's InputStream and OutputStream respectively, and sets _isConnected_ to true.
     * <p>
     * Subclasses overriding this method should start by calling {@code super._connectAction_()} first to ensure the initialization of the aforementioned
     * protected variables.
     *
     * @throws IOException (SocketException) if a problem occurs with the socket
     */
    protected void _connectAction_() throws IOException {
        applySocketAttributes();
        _input_ = _socket_.getInputStream();
        _output_ = _socket_.getOutputStream();
    }


    /**
     * Applies socket attributes.
     *
     * @throws SocketException if there is an error in the underlying protocol, such as a TCP error.
     * @since 3.8.0
     */
    protected void applySocketAttributes() throws SocketException {
        _socket_.setSoTimeout(_timeout_);
    }

    private void closeQuietly(final Closeable close) {
        if (close != null) {
            try {
                close.close();
            } catch (final IOException e) {
                // Ignored
            }
        }
    }

    private void closeQuietly(final Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (final IOException e) {
                // Ignored
            }
        }
    }

    /**
     * Opens a Socket connected to a remote host at the current default port and originating from the current host at a system assigned port. Before returning,
     * {@link #_connectAction_ _connectAction_() } is called to perform connection initialization actions.
     *
     * @param host The remote host.
     * @throws SocketException If the socket timeout could not be set.
     * @throws IOException     If the socket could not be opened. In most cases you will only want to catch IOException since SocketException is derived from
     *                         it.
     */
    public void connect(final InetAddress host) throws SocketException, IOException {
        _hostname_ = null;
        connect(host, _defaultPort_);
    }

    /**
     * Opens a Socket connected to a remote host at the specified port and originating from the current host at a system assigned port. Before returning,
     * {@link #_connectAction_ _connectAction_() } is called to perform connection initialization actions.
     *
     * @param host The remote host.
     * @param port The port to connect to on the remote host.
     * @throws SocketException If the socket timeout could not be set.
     * @throws IOException     If the socket could not be opened. In most cases you will only want to catch IOException since SocketException is derived from
     *                         it.
     */
    public void connect(final InetAddress host, final int port) throws SocketException, IOException {
        _hostname_ = null;
        _connect(new InetSocketAddress(host, port), null, -1);
    }

    /**
     * Opens a Socket connected to a remote host at the specified port and originating from the specified local address and port. Before returning,
     * {@link #_connectAction_ _connectAction_() } is called to perform connection initialization actions.
     *
     * @param host      The remote host.
     * @param port      The port to connect to on the remote host.
     * @param localAddr The local address to use.
     * @param localPort The local port to use.
     * @throws SocketException If the socket timeout could not be set.
     * @throws IOException     If the socket could not be opened. In most cases you will only want to catch IOException since SocketException is derived from
     *                         it.
     */
    public void connect(final InetAddress host, final int port, final InetAddress localAddr, final int localPort) throws SocketException, IOException {
        _hostname_ = null;
        _connect(new InetSocketAddress(host, port), localAddr, localPort);
    }

    /**
     * Opens a Socket connected to a remote host at the current default port and originating from the current host at a system assigned port. Before returning,
     * {@link #_connectAction_ _connectAction_() } is called to perform connection initialization actions.
     *
     * @param hostname The name of the remote host.
     * @throws SocketException               If the socket timeout could not be set.
     * @throws IOException                   If the socket could not be opened. In most cases you will only want to catch IOException since SocketException is
     *                                       derived from it.
     * @throws java.net.UnknownHostException If the hostname cannot be resolved.
     */
    public void connect(final String hostname) throws SocketException, IOException {
        connect(hostname, _defaultPort_);
    }

    /**
     * Opens a Socket connected to a remote host at the specified port and originating from the current host at a system assigned port. Before returning,
     * {@link #_connectAction_ _connectAction_() } is called to perform connection initialization actions.
     *
     * @param hostname The name of the remote host.
     * @param port     The port to connect to on the remote host.
     * @throws SocketException               If the socket timeout could not be set.
     * @throws IOException                   If the socket could not be opened. In most cases you will only want to catch IOException since SocketException is
     *                                       derived from it.
     * @throws java.net.UnknownHostException If the hostname cannot be resolved.
     */
    public void connect(final String hostname, final int port) throws SocketException, IOException {
        connect(hostname, port, null, -1);
    }

    /**
     * Opens a Socket connected to a remote host at the specified port and originating from the specified local address and port. Before returning,
     * {@link #_connectAction_ _connectAction_() } is called to perform connection initialization actions.
     *
     * @param hostname  The name of the remote host.
     * @param port      The port to connect to on the remote host.
     * @param localAddr The local address to use.
     * @param localPort The local port to use.
     * @throws SocketException               If the socket timeout could not be set.
     * @throws IOException                   If the socket could not be opened. In most cases you will only want to catch IOException since SocketException is
     *                                       derived from it.
     * @throws java.net.UnknownHostException If the hostname cannot be resolved.
     */
    public void connect(final String hostname, final int port, final InetAddress localAddr, final int localPort) throws SocketException, IOException {
        _hostname_ = hostname;
        _connect(new InetSocketAddress(hostname, port), localAddr, localPort);
    }

    /**
     * Disconnects the socket connection. You should call this method after you've finished using the class instance and also before you call {@link #connect
     * connect() } again. _isConnected_ is set to false, _socket_ is set to null, _input_ is set to null, and _output_ is set to null.
     *
     * @throws IOException not thrown, subclasses may throw.
     */
    @SuppressWarnings("unused") // subclasses may throw IOException
    public void disconnect() throws IOException {
        closeQuietly(_socket_);
        closeQuietly(_input_);
        closeQuietly(_output_);
        _socket_ = null;
        _hostname_ = null;
        _input_ = null;
        _output_ = null;
    }

    /**
     * @return The remote address to which the client is connected. Delegates to {@link Socket#getInetAddress()}
     * @throws NullPointerException if the socket is not currently open
     */
    public InetAddress getRemoteAddress() {
        return _socket_.getInetAddress();
    }

    /**
     * Returns the port number of the remote host to which the client is connected. Delegates to {@link Socket#getPort()}
     *
     * @return The port number of the remote host to which the client is connected.
     * @throws NullPointerException if the socket is not currently open
     */
    public int getRemotePort() {
        return _socket_.getPort();
    }

    /**
     * Sets the connection timeout in milliseconds, which will be passed to the {@link Socket} object's connect() method.
     *
     * @param connectTimeout The connection timeout to use (in ms)
     * @since 2.0
     */
    @SuppressWarnings("unused")
    public void setConnectTimeout(final int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * Sets the default port the SocketClient should connect to when a port is not specified. The {@link #_defaultPort_ _defaultPort_ } variable stores this
     * value. If never set, the default port is equal to zero.
     *
     * @param port The default port to set.
     */
    public void setDefaultPort(final int port) {
        _defaultPort_ = port;
    }

    /**
     * Sets the default timeout in milliseconds to use when opening a socket. This value is only used previous to a call to {@link #connect connect()} and
     * should not be confused with {@link #setSoTimeout setSoTimeout()} which operates on the currently opened socket. _timeout_ contains the new timeout value.
     *
     * @param timeout The timeout in milliseconds to use for the socket connection.
     */
    public void setDefaultTimeout(final int timeout) {
        _timeout_ = timeout;
    }

    /**
     * Sets the underlying socket receive buffer size.
     *
     * @param size The size of the buffer in bytes.
     * @since 2.0
     */
    @SuppressWarnings("unused") // subclasses may throw SocketException
    public void setReceiveBufferSize(final int size) {
        receiveBufferSize = size;
    }

    /**
     * Sets the underlying socket send buffer size.
     *
     * @param size The size of the buffer in bytes.
     * @since 2.0
     */
    @SuppressWarnings("unused") // subclasses may throw SocketException
    public void setSendBufferSize(final int size) {
        sendBufferSize = size;
    }

    /**
     * Sets the timeout in milliseconds of a currently open connection. Only call this method after a connection has been opened by {@link #connect connect()}.
     * <p>
     * To set the initial timeout, use {@link #setDefaultTimeout(int)} instead.
     *
     * @param timeout The timeout in milliseconds to use for the currently open socket connection.
     * @throws SocketException      If the operation fails.
     * @throws NullPointerException if the socket is not currently open
     */
    @SuppressWarnings("unused")
    public void setSoTimeout(final int timeout) throws SocketException {
        _socket_.setSoTimeout(timeout);
    }

    /*
     * N.B. Fields cannot be pulled up into a super-class without breaking binary compatibility, so the abstract method is needed to pass the instance to the
     * methods which were moved here.
     */
}
