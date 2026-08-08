package be.dnsbelgium.mercator.feature.extraction;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;

public class ReplacingInputStream extends FilterInputStream {

    private final byte[] pattern;
    private final byte[] replacement;
    private final Queue<Integer> inQueue = new LinkedList<>();
    private final Queue<Integer> outQueue = new LinkedList<>();

    public ReplacingInputStream(InputStream in, String pattern, String replacement) {
        this(in, pattern.getBytes(), replacement.getBytes());
    }

    public ReplacingInputStream(InputStream in, byte[] pattern, byte[] replacement) {
        super(in);
        this.pattern = pattern;
        this.replacement = replacement;
    }

    @Override
    public int read() throws IOException {
        if (outQueue.isEmpty()) {
            readAhead();
        }
        return outQueue.isEmpty() ? -1 : outQueue.poll();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int i = 0;
        while (i < len) {
            int b_read = read();
            if (b_read == -1) {
                return i == 0 ? -1 : i;
            }
            b[off + i] = (byte) b_read;
            i++;
        }
        return i;
    }

    private void readAhead() throws IOException {
        while (outQueue.isEmpty()) {
            // Need to read enough to match pattern
            while (inQueue.size() < pattern.length) {
                int next = super.read();
                if (next == -1) {
                    break;
                }
                inQueue.offer(next);
            }

            if (isMatchFound()) {
                for (int i = 0; i < pattern.length; i++) {
                    inQueue.poll();
                }
                for (byte b : replacement) {
                    outQueue.offer((int) b);
                }
            } else {
                if (!inQueue.isEmpty()) {
                    outQueue.offer(inQueue.poll());
                } else {
                    break; // End of stream
                }
            }
        }
    }

    private boolean isMatchFound() {
        if (inQueue.size() < pattern.length) {
            return false;
        }
        int i = 0;
        for (int b : inQueue) {
            if (i >= pattern.length) break;
            if ((byte) b != pattern[i]) {
                return false;
            }
            i++;
        }
        return true;
    }
}
