package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.persistence.TlsRepository;
import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.junit.jupiter.api.Assertions.*;

class TlsSearchControllerTest {

  TlsRepository tlsRepository;
  private static final Logger logger = LoggerFactory.getLogger(TlsSearchControllerTest.class);
  private final ObjectMother objectMother = new ObjectMother();

  @Test
  public void getLatestNotFound() {
    tlsRepository = mock(TlsRepository.class);
    logger.info("tlsRepository = {}", tlsRepository);
    when(tlsRepository.findLatestCrawlResult("abc.be")).thenReturn(Optional.empty());
    TlsSearchController controller = new TlsSearchController(tlsRepository);
    Model model = new ConcurrentModel();
    String viewName = controller.getLatest(model, "abc.be");
    logger.info("viewName = {}", viewName);
    logger.info("model = {}", model);
    verify(tlsRepository).findLatestCrawlResult("abc.be");
    verifyNoMoreInteractions(tlsRepository);
    assertThat(model.containsAttribute("tlsCrawlResults")).isFalse();
  }

  @Test
  public void getLatestIsFound() {
    tlsRepository = mock(TlsRepository.class);
    TlsCrawlResult crawlResult = objectMother.tlsCrawlResult1();
    when(tlsRepository.findLatestCrawlResult("abc.be")).thenReturn(Optional.of(crawlResult));
    TlsSearchController controller = new TlsSearchController(tlsRepository);
    Model model = new ConcurrentModel();
    String viewName = controller.getLatest(model, "abc.be");
    logger.info("viewName = {}", viewName);
    logger.info("model = {}", model);
    verify(tlsRepository).findLatestCrawlResult("abc.be");
    verifyNoMoreInteractions(tlsRepository);
    assertThat(model.getAttribute("tlsCrawlResults")).isEqualTo(List.of(crawlResult));
  }

}