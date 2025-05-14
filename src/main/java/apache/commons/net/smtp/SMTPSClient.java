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

package apache.commons.net.smtp;

import apache.commons.net.SocketClient;
import apache.commons.net.io.LineReader;
import apache.commons.net.util.SSLContextUtils;
import apache.commons.net.util.SSLSocketUtils;

import javax.net.ssl.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * SMTP over SSL processing. Copied from FTPSClient.java and modified to suit SMTP. If implicit mode is selected (NOT the default), SSL/TLS negotiation starts
 * right after the connection has been established. In explicit mode (the default), SSL/TLS negotiation starts when the user calls execTLS() and the server
 * accepts the command. Implicit usage:
 *
 * <pre>
 * SMTPSClient c = new SMTPSClient(true);
 * c.connect("127.0.0.1", 465);
 * </pre>
 *
 * Explicit usage:
 *
 * <pre>
 * SMTPSClient c = new SMTPSClient();
 * c.connect("127.0.0.1", 25);
 * if (c.execTLS()) {
 *     // Rest of the commands here
 * }
 * </pre>
 *
 * <em>Warning</em>: the hostname is not verified against the certificate by default, use {@link #setHostnameVerifier(HostnameVerifier)} or
 * {@link #setEndpointCheckingEnabled(boolean)} (on Java 1.7+) to enable verification.
 *
 * @since 3.0
 */
@SuppressWarnings("LombokGetterMayBeUsed")
public class SMTPSClient extends SMTPClient {
    /** Default secure socket protocol name, like TLS */
    private static final String DEFAULT_PROTOCOL = "TLS";

    /** The security mode. True - Implicit Mode / False - Explicit Mode. */

    private final boolean isImplicit;
    /** The secure socket protocol to be used, like SSL/TLS. */

    private final String protocol;
    /** The context object. */

    private SSLContext context;
    /**
     * The cipher suites. SSLSockets have a default set of these anyway, so no initialization required.
     */

    private String[] suites;
    /** The protocol versions. */

    private String[] protocols;

    /** The {@link TrustManager} implementation, default null (i.e. use system managers). */
    @SuppressWarnings("unused")
    private TrustManager trustManager;

    /** The {@link KeyManager}, default null (i.e. use system managers). */
    @SuppressWarnings("unused")
    private KeyManager keyManager; // seems not to be required

    /** The {@link HostnameVerifier} to use post-TLS, default null (i.e. no verification). */
    private HostnameVerifier hostnameVerifier;

    /** Use Java 1.7+ HTTPS Endpoint Identification Algorithm. */
    private boolean tlsEndpointChecking;

    /**
     * Constructor for SMTPSClient, using {@link #DEFAULT_PROTOCOL} i.e. TLS Sets security mode to explicit (isImplicit = false).
     */
    @SuppressWarnings("unused")
    public SMTPSClient() {
        this(DEFAULT_PROTOCOL, false);
    }

    /**
     * Constructor for SMTPSClient, using {@link #DEFAULT_PROTOCOL} i.e. TLS
     *
     * @param implicit The security mode, {@code true} for implicit, {@code false} for explicit
     */
    @SuppressWarnings("unused")
    public SMTPSClient(final boolean implicit) {
        this(DEFAULT_PROTOCOL, implicit);
    }

    /**
     * Constructor for SMTPSClient, using {@link #DEFAULT_PROTOCOL} i.e. TLS
     *
     * @param implicit The security mode, {@code true} for implicit, {@code false} for explicit
     * @param ctx      A pre-configured SSL Context.
     */
    public SMTPSClient(final boolean implicit, final SSLContext ctx) {
        isImplicit = implicit;
        context = ctx;
        protocol = DEFAULT_PROTOCOL;
    }

    /**
     * Constructor for SMTPSClient.
     *
     * @param context A pre-configured SSL Context.
     * @see #SMTPSClient(boolean, SSLContext)
     */
    public SMTPSClient(final SSLContext context) {
        this(false, context);
    }

    /**
     * Constructor for SMTPSClient, using explicit security mode.
     *
     * @param proto the protocol.
     */
    @SuppressWarnings("unused")
    public SMTPSClient(final String proto) {
        this(proto, false);
    }

    /**
     * Constructor for SMTPSClient.
     *
     * @param proto    the protocol.
     * @param implicit The security mode, {@code true} for implicit, {@code false} for explicit
     */
    public SMTPSClient(final String proto, final boolean implicit) {
        protocol = proto;
        isImplicit = implicit;
    }

    /**
     * Constructor for SMTPSClient.
     *
     * @param proto    the protocol.
     * @param implicit The security mode, {@code true} for implicit, {@code false} for explicit
     * @param encoding the encoding
     * @since 3.3
     */
    @SuppressWarnings("unused")
    public SMTPSClient(final String proto, final boolean implicit, final String encoding) {
        super(encoding);
        protocol = proto;
        isImplicit = implicit;
    }

    /**
     * Because there are so many connect() methods, the _connectAction_() method is provided as a means of performing some action immediately after establishing
     * a connection, rather than reimplementing all the connect() methods.
     *
     * @throws IOException If it is thrown by _connectAction_().
     * @see SocketClient#_connectAction_()
     */
    @Override
    protected void _connectAction_() throws IOException {
        // Implicit mode.
        if (isImplicit) {
            applySocketAttributes();
            performSSLNegotiation();
        }
        super._connectAction_();
        // Explicit mode - don't do anything. The user calls execTLS()
    }

    /**
     * The TLS command execution.
     *
     * @throws IOException If an I/O error occurs while sending the command or performing the negotiation.
     * @return TRUE if the command and negotiation succeeded.
     */
    public boolean execTLS() throws IOException {
        if (!SMTPReply.isPositiveCompletion(sendCommand("STARTTLS"))) {
            return false;
            // throw new SSLException(getReplyString());
        }
        performSSLNegotiation();
        return true;
    }

    /**
     * Returns the names of the cipher suites which could be enabled for use on this connection. When the underlying {@link java.net.Socket Socket} is not an
     * {@link SSLSocket} instance, returns null.
     *
     * @return An array of cipher suite names, or {@code null}.
     */
    @SuppressWarnings("unused")
    public String[] getEnabledCipherSuites() {
        if (_socket_ instanceof SSLSocket) {
            return ((SSLSocket) _socket_).getEnabledCipherSuites();
        }
        return null;
    }

    /**
     * Returns the names of the protocol versions which are currently enabled for use on this connection. When the underlying {@link java.net.Socket Socket} is
     * not an {@link SSLSocket} instance, returns null.
     *
     * @return An array of protocols, or {@code null}.
     */
    @SuppressWarnings("unused")
    public String[] getEnabledProtocols() {
        if (_socket_ instanceof SSLSocket) {
            return ((SSLSocket) _socket_).getEnabledProtocols();
        }
        return null;
    }

    /**
     * Gets the currently configured {@link HostnameVerifier}.
     *
     * @return A HostnameVerifier instance.
     * @since 3.4
     */
    @SuppressWarnings("unused")
    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    /**
     * Gets the {@link KeyManager} instance.
     *
     * @return The current {@link KeyManager} instance.
     */
    public KeyManager getKeyManager() {
        return keyManager;
    }

    /**
     * Gets the currently configured {@link TrustManager}.
     *
     * @return A TrustManager instance.
     */
    public TrustManager getTrustManager() {
        return trustManager;
    }

    /**
     * Performs a lazy init of the SSL context.
     *
     * @throws IOException When could not initialize the SSL context.
     */
    private void initSSLContext() throws IOException {
        if (context == null) {
            context = SSLContextUtils.createSSLContext(protocol, getKeyManager(), getTrustManager());
        }
    }

    /**
     * SSL/TLS negotiation. Acquires an SSL socket of a connection and carries out handshake processing.
     *
     * @throws IOException If server negotiation fails.
     */
    private void performSSLNegotiation() throws IOException {
        initSSLContext();

        final SSLSocketFactory ssf = context.getSocketFactory();
        final String host = _hostname_ != null ? _hostname_ : getRemoteAddress().getHostAddress();
        final int port = getRemotePort();
        final SSLSocket socket = (SSLSocket) ssf.createSocket(_socket_, host, port, true);
        socket.setEnableSessionCreation(true);
        socket.setUseClientMode(true);

        if (tlsEndpointChecking) {
            SSLSocketUtils.enableEndpointNameVerification(socket);
        }
        if (protocols != null) {
            socket.setEnabledProtocols(protocols);
        }
        if (suites != null) {
            socket.setEnabledCipherSuites(suites);
        }
        socket. startHandshake();

        _socket_ = socket;
        _input_ = socket.getInputStream();
        _output_ = socket.getOutputStream();

        reader = new LineReader(new InputStreamReader(_input_, encoding));
        writer = new BufferedWriter(new OutputStreamWriter(_output_, encoding));

        if (hostnameVerifier != null && !hostnameVerifier.verify(host, socket.getSession())) {
            throw new SSLHandshakeException("Hostname doesn't match certificate");
        }
    }

    /**
     * Controls which particular cipher suites are enabled for use on this connection. Called before server negotiation.
     *
     * @param cipherSuites The cipher suites.
     */
    @SuppressWarnings("unused")
    public void setEnabledCipherSuites(final String[] cipherSuites) {
        suites = cipherSuites.clone();
    }

    /**
     * Controls which particular protocol versions are enabled for use on this connection. I perform setting before a server negotiation.
     *
     * @param protocolVersions The protocol versions.
     */
    @SuppressWarnings("unused")
    public void setEnabledProtocols(final String[] protocolVersions) {
        protocols = protocolVersions.clone();
    }

    /**
     * Automatic endpoint identification checking using the HTTPS algorithm is supported on Java 1.7+. The default behavior is for this to be disabled.
     *
     * @param enable Enable automatic endpoint identification checking using the HTTPS algorithm on Java 1.7+.
     * @since 3.4
     */
    @SuppressWarnings("unused")
    public void setEndpointCheckingEnabled(final boolean enable) {
        tlsEndpointChecking = enable;
    }

    /**
     * Override the default {@link HostnameVerifier} to use.
     *
     * @param newHostnameVerifier The HostnameVerifier implementation to set or {@code null} to disable.
     * @since 3.4
     */
    @SuppressWarnings("unused")
    public void setHostnameVerifier(final HostnameVerifier newHostnameVerifier) {
        hostnameVerifier = newHostnameVerifier;
    }

}

