package be.dnsbelgium.mercator.feature.extraction;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ReplacingInputStreamTest {

    @Test
    void testNoReplacement() throws IOException {
        String input = "Hello World";
        ReplacingInputStream ris = new ReplacingInputStream(new ByteArrayInputStream(input.getBytes()), "foo", "bar");
        String output = new String(ris.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(output).isEqualTo("Hello World");
    }

    @Test
    void testSimpleReplacement() throws IOException {
        String input = "Hello World";
        ReplacingInputStream ris = new ReplacingInputStream(new ByteArrayInputStream(input.getBytes()), "World", "Universe");
        String output = new String(ris.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(output).isEqualTo("Hello Universe");
    }

    @Test
    void testMultipleReplacements() throws IOException {
        String input = "aba";
        ReplacingInputStream ris = new ReplacingInputStream(new ByteArrayInputStream(input.getBytes()), "a", "b");
        String output = new String(ris.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(output).isEqualTo("bbb");
    }

    @Test
    void testReplacementAtStart() throws IOException {
        String input = "abc";
        ReplacingInputStream ris = new ReplacingInputStream(new ByteArrayInputStream(input.getBytes()), "a", "x");
        String output = new String(ris.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(output).isEqualTo("xbc");
    }

    @Test
    void testReplacementAtEnd() throws IOException {
        String input = "abc";
        ReplacingInputStream ris = new ReplacingInputStream(new ByteArrayInputStream(input.getBytes()), "c", "z");
        String output = new String(ris.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(output).isEqualTo("abz");
    }
}
