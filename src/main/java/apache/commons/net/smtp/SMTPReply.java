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

/**
 * SMTPReply stores a set of constants for SMTP reply codes. To interpret the meaning of the codes, familiarity with RFC 821 is assumed. The mnemonic constant
 * names are transcriptions from the code descriptions of RFC 821.
 */
@SuppressWarnings("unused")
public final class SMTPReply {

    /** SMTP reply code {@value}. */
    public static final int SYSTEM_STATUS = 211;

    /** SMTP reply code {@value}. */
    public static final int HELP_MESSAGE = 214;

    /** SMTP reply code {@value}. */
    public static final int SERVICE_READY = 220;

    /** SMTP reply code {@value}. */
    public static final int SERVICE_CLOSING_TRANSMISSION_CHANNEL = 221;

    /** SMTP reply code {@value}. */
    public static final int ACTION_OK = 250;

    /** SMTP reply code {@value}. */
    public static final int USER_NOT_LOCAL_WILL_FORWARD = 251;

    /** SMTP reply code {@value}. */
    public static final int START_MAIL_INPUT = 354;

    /** SMTP reply code {@value}. */
    public static final int SERVICE_NOT_AVAILABLE = 421;

    /** SMTP reply code {@value}. */
    public static final int ACTION_NOT_TAKEN = 450;

    /** SMTP reply code {@value}. */
    public static final int ACTION_ABORTED = 451;

    /** SMTP reply code {@value}. */
    public static final int INSUFFICIENT_STORAGE = 452;

    /** SMTP reply code {@value}. */
    public static final int UNRECOGNIZED_COMMAND = 500;

    /** SMTP reply code {@value}. */
    public static final int SYNTAX_ERROR_IN_ARGUMENTS = 501;

    /** SMTP reply code {@value}. */
    public static final int COMMAND_NOT_IMPLEMENTED = 502;

    /** SMTP reply code {@value}. */
    public static final int BAD_COMMAND_SEQUENCE = 503;

    /** SMTP reply code {@value}. */
    public static final int COMMAND_NOT_IMPLEMENTED_FOR_PARAMETER = 504;

    /** SMTP reply code {@value}. */
    public static final int MAILBOX_UNAVAILABLE = 550;

    /** SMTP reply code {@value}. */
    public static final int USER_NOT_LOCAL = 551;

    /**
    /** SMTP reply code {@value}. */
    public static final int STORAGE_ALLOCATION_EXCEEDED = 552;

    /** SMTP reply code {@value}. */
    public static final int MAILBOX_NAME_NOT_ALLOWED = 553;

    /** SMTP reply code {@value}. */
    public static final int TRANSACTION_FAILED = 554;

    /**
     * Tests if a reply code is a negative permanent response. All codes beginning with a 5 are negative permanent responses. The SMTP server will send a
     * negative permanent response on the failure of a command that cannot be reattempted with success.
     *
     * @param reply The reply code to test.
     * @return True if a reply code is a negative permanent response, false if not.
     */
    public static boolean isNegativePermanent(final int reply) {
        return reply >= 500 && reply < 600;
    }

    /**
     * Tests if a reply code is a negative transient response. All codes beginning with a 4 are negative transient responses. The SMTP server will send a
     * negative transient response on the failure of a command that can be reattempted with success.
     *
     * @param reply The reply code to test.
     * @return True if a reply code is a negative transient response, false if not.
     */
    public static boolean isNegativeTransient(final int reply) {
        return reply >= 400 && reply < 500;
    }

    /**
     * Tests if a reply code is a positive completion response. All codes beginning with a 2 are positive completion responses. The SMTP server will send a
     * positive completion response on the final successful completion of a command.
     *
     * @param reply The reply code to test.
     * @return True if a reply code is a positive completion response, false if not.
     */
    public static boolean isPositiveCompletion(final int reply) {
        return reply >= 200 && reply < 300;
    }

    /**
     * Tests if a reply code is a positive intermediate response. All codes beginning with a 3 are positive intermediate responses. The SMTP server will
     * send a positive intermediate response on the successful completion of one part of a multipart sequence of commands. For example, after a successful DATA
     * command, a positive intermediate response will be sent to indicate that the server is ready to receive the message data.
     *
     * @param reply The reply code to test.
     * @return True if a reply code is a positive intermediate response, false if not.
     */
    public static boolean isPositiveIntermediate(final int reply) {
        return reply >= 300 && reply < 400;
    }

    /**
     * Tests if a reply code is a positive preliminary response. All codes beginning with a 1 are positive preliminary responses. Positive preliminary
     * responses are used to indicate tentative success. No further commands can be issued to the SMTP server after a positive preliminary response until a
     * follow-up response is received from the server.
     * <p>
     * <b>Note:</b> <em> No SMTP commands defined in RFC 822 provide this type of reply. </em>
     * </p>
     *
     * @param reply The reply code to test.
     * @return True if a reply code is a positive preliminary response, false if not.
     */
    public static boolean isPositivePreliminary(final int reply) {
        return reply >= 100 && reply < 200;
    }

    /** Cannot be instantiated. */
    private SMTPReply() {
    }

}
