// copied from jappalyzer library
package be.dnsbelgium.mercator.web.wappalyzer.jappalyzer;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PatternWithVersionTest {

    @Test
    public void shouldContainsVersion() {
        PatternWithVersion pattern = new PatternWithVersion("/jquery(?:-(\\d+\\.\\d+\\.\\d+))[/.-]\\;version:\\1");
        PatternMatch match = pattern.match("/jquery-3.1.2.js");
        assertThat(match.isMatched()).isTrue();
        assertThat(match.getVersion()).isEqualTo("3.1.2");
    }

    @Test
    public void shouldNotContainsVersion() {
        PatternWithVersion pattern = new PatternWithVersion("/jquery");
        PatternMatch match = pattern.match("/jquery.js");
        assertThat(match.isMatched()).isTrue();
        assertThat(match.getVersion()).isEqualTo("");
    }

}