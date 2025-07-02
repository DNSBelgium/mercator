package be.dnsbelgium.mercator.web.domain;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static be.dnsbelgium.mercator.web.domain.DomainNameValidator.isValidDomainName;


public class DomainNameValidatorTest {

  @Test
  public void testValidDomainName() {
    Assertions.assertThat(isValidDomainName("abc.be")).isTrue();
    Assertions.assertThat(isValidDomainName("café.be")).isTrue();
    Assertions.assertThat(isValidDomainName("xn--caf-dma.be")).isTrue();
    Assertions.assertThat(isValidDomainName("ab c.be")).isFalse();
    Assertions.assertThat(isValidDomainName("abc..be")).isFalse();
    Assertions.assertThat(isValidDomainName("---a.be")).isTrue();
    Assertions.assertThat(isValidDomainName("a.be")).isTrue();
  }

}
