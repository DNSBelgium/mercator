package apache.commons.net.io;

import apache.commons.net.util.NetConstants;

import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.locks.ReentrantLock;

// Based on https://commons.apache.org/proper/commons-net/jacoco/org.apache.commons.net.io/CRLFLineReader.java.html
// with the use of synchronized replaced with a ReentrantLock to better work on Virtual Threads

public class LineReader {

    private static final char LF = '\n';
    private static final char CR = '\r';

    private final ReentrantLock reentrantLock = new ReentrantLock();

    private final Reader reader;

    /**
     * Creates a CRLFLineReader that wraps an existing Reader input source.
     *
     * @param reader The Reader input source.
     */
    public LineReader(final Reader reader) {
        //super(reader);
        this.reader = reader;
    }

    /**
     * Read a line of text. A line is considered to be terminated by carriage return followed immediately by a linefeed.
     * This contrasts with BufferedReader which also allows other combinations.
     */
    public String readLine() throws IOException {
        final StringBuilder sb = new StringBuilder();
        int character;
        boolean prevWasCR = false;

        reentrantLock.lock();
        try {
            while ((character = reader.read()) != NetConstants.EOS) {
                if (prevWasCR && character == LF) {
                    return sb.substring(0, sb.length() - 1);
                }
                prevWasCR = character == CR;
                sb.append((char) character);
            }
        } finally {
            reentrantLock.unlock();
        }
        final String string = sb.toString();
        if (string.isEmpty()) { // immediate EOF
            return null;
        }
        return string;
    }

}
