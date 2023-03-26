package be.dnsbelgium.mercator.common.messaging.idn;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class IDN2008Test {

  @Test void ascii_remains_ascii() {
    String ascii  = IDN2008.toASCII("abc.be");
    String ulabel = IDN2008.toUnicode("abc.be");
    assertThat(ascii).isEqualTo("abc.be");
    assertThat(ulabel).isEqualTo("abc.be");
  }

  @Test void ulabel_input() {
    String input = "DnsBelgië.BE";
    String a_label = IDN2008.toASCII(input);
    String u_label = IDN2008.toUnicode(input);
    assertThat(a_label).isEqualTo("xn--dnsbelgi-01a.be");
    assertThat(u_label).isEqualTo("dnsbelgië.be");
  }

  @Test void alabel_input() {
    String input = "XN--dnsBelgi-01a.BE";
    String a_label = IDN2008.toASCII(input);
    String u_label = IDN2008.toUnicode(input);
    assertThat(a_label).isEqualTo("xn--dnsbelgi-01a.be");
    assertThat(u_label).isEqualTo("dnsbelgië.be");
  }

  @Test
  void invalid_Input_throws() {
    assertThrows(IdnException.class, () -> IDN2008.toASCII("--xx--.be"));
    assertThrows(IdnException.class, () -> IDN2008.toUnicode("--xx--.be"));
  }

  @Test void eszett_is_not_ss() {
    String input = "dnß.BE";
    // under IDN2003 a_label and u_label would be dnss.be but not under IDNA2008
    String a_label = IDN2008.toASCII(input);
    String u_label = IDN2008.toUnicode(input);
    assertThat(a_label).isEqualTo("xn--dn-hia.be");
    assertThat(u_label).isEqualTo("dnß.be");
  }
}