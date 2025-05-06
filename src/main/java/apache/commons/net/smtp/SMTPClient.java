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

import apache.commons.net.MalformedServerReplyException;
import apache.commons.net.SocketClient;

import java.io.IOException;

/**
 * SMTPClient encapsulates all the functionality necessary to send files through an SMTP server. This class takes care of all low level details of interacting
 * with an SMTP server and provides a convenient higher level interface. As with all classes derived from {@link SocketClient}, you must
 * first connect to the server with {@link SocketClient#connect connect } before doing anything, and finally
 * {@link SocketClient#disconnect disconnect } after you're completely finished interacting with the server. Then you need to check the
 * SMTP reply code to see if the connection was successful. For example:
 *
 * <pre>
 *    try {
 *      int reply;
 *      client.connect("mail.foobar.com");
 *      System.out.print(client.getReplyString());
 *
 *      // After connection attempt, you should check the reply code to verify
 *      // success.
 *      reply = client.getReplyCode();
 *
 *      if (!SMTPReply.isPositiveCompletion(reply)) {
 *        client.disconnect();
 *        System.err.println("SMTP server refused connection.");
 *        System.exit(1);
 *      }
 *
 *      // Do useful stuff here.
 *      ...
 *    } catch (IOException e) {
 *      if (client.isConnected()) {
 *        try {
 *          client.disconnect();
 *        } catch (IOException f) {
 *          // do nothing
 *        }
 *      }
 *      System.err.println("Could not connect to server.");
 *      e.printStackTrace();
 *      System.exit(1);
 *    }
 * </pre>
 * <p>
 * Immediately after connecting is the only real time you need to check the reply code (because connect is of type void). The convention for all the SMTP
 * command methods in SMTPClient is such that they either return a boolean value or some other value. The boolean methods return true on a successful completion
 * reply from the SMTP server and false on a reply resulting in an error condition or failure. The methods returning a value other than boolean return a value
 * containing the higher level data produced by the SMTP command, or null if a reply resulted in an error condition or failure. If you want to access the exact
 * SMTP reply code causing a success or failure, you must call {@link SMTP#getReplyCode getReplyCode } after a success or failure.
 * </p>
 * <p>
 * You should keep in mind that the SMTP server may choose to prematurely close a connection for various reasons. The SMTPClient class will detect a premature
 * SMTP server connection closing when it receives a {@link SMTPReply#SERVICE_NOT_AVAILABLE SMTPReply.SERVICE_NOT_AVAILABLE }
 * response to a command. When that occurs, the method encountering that reply will throw an {@link SMTPConnectionClosedException} .
 * {@code SMTPConnectionClosedException} is a subclass of {@code IOException} and therefore need not be caught separately, but if you are going to
 * catch it separately, its catch block must appear before the more general {@code IOException} catch block. When you encounter an
 * {@link SMTPConnectionClosedException} , you must disconnect the connection with {@link #disconnect disconnect() } to properly
 * clean up the system resources used by SMTPClient. Before disconnecting, you may check the last reply code and text with
 * {@link SMTP#getReplyCode getReplyCode }, {@link SMTP#getReplyString getReplyString }, and
 * {@link SMTP#getReplyStrings getReplyStrings}.
 * </p>
 * <p>
 * Rather than list it separately for each method, we mention here that every method communicating with the server and throwing an IOException can also throw a
 * {@link MalformedServerReplyException} , which is a subclass of IOException. A MalformedServerReplyException will be thrown when the
 * reply received from the server deviates enough from the protocol specification that it cannot be interpreted in a useful manner despite attempts to be as
 * lenient as possible.
 * </p>
 *
 * @see SMTP
 * @see SMTPConnectionClosedException
 * @see MalformedServerReplyException
 */
public class SMTPClient extends SMTP {

    /**
     * Default SMTPClient constructor. Creates a new SMTPClient instance.
     */
    public SMTPClient() {
    }

    /**
     * Overloaded constructor that takes an encoding specification
     *
     * @param encoding The encoding to use
     * @since 2.0
     */
    public SMTPClient(final String encoding) {
        super(encoding);
    }

    /**
     * Login to the SMTP server by sending the {@code HELO} command with the given hostname as an argument.
     * Before performing any mail commands, you must first log in.
     *
     * @param hostname The hostname with which to greet the SMTP server.
     * @return True if successfully completed, false if not.
     * @throws SMTPConnectionClosedException If the SMTP server prematurely closes the connection as a result of the client being idle or some other reason
     *                                       causing the server to send SMTP reply code 421. This exception may be caught either as an IOException or
     *                                       independently as itself.
     * @throws IOException                   If an I/O error occurs while either sending a command to the server or receiving a reply from the server.
     */
    public boolean login(final String hostname) throws IOException {
        return SMTPReply.isPositiveCompletion(helo(hostname));
    }

    /**
     * Logout of the SMTP server by sending the QUIT command.
     *
     * @return True if successfully completed, false if not.
     * @throws SMTPConnectionClosedException If the SMTP server prematurely closes the connection as a result of the client being idle or some other reason
     *                                       causing the server to send SMTP reply code 421. This exception may be caught either as an IOException or
     *                                       independently as itself.
     * @throws IOException                   If an I/O error occurs while either sending a command to the server or receiving a reply from the server.
     */
    @SuppressWarnings("unused")
    public boolean logout() throws IOException {
        return SMTPReply.isPositiveCompletion(quit());
    }

}
