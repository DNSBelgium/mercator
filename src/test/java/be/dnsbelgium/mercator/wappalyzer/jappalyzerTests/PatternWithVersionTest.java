// copied from jappalyzer library
package be.dnsbelgium.mercator.wappalyzer.jappalyzerTests;

import org.junit.Test;

import be.dnsbelgium.mercator.wappalyzer.jappalyzer.PatternMatch;
import be.dnsbelgium.mercator.wappalyzer.jappalyzer.PatternWithVersion;

import static org.assertj.core.api.Assertions.*;

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