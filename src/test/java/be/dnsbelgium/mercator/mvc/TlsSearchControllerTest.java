package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.persistence.TlsRepository;
import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

class TlsSearchControllerTest {

  TlsRepository tlsRepository;
  private static final Logger logger = LoggerFactory.getLogger(TlsSearchControllerTest.class);
  private final ObjectMother objectMother = new ObjectMother();

  @Test
  public void findLatestCrawlResult_NotFound() {
    tlsRepository = mock(TlsRepository.class);
    logger.info("tlsRepository = {}", tlsRepository);
    when(tlsRepository.findLatestResult("abc.be")).thenReturn(Optional.empty());
    TlsSearchController controller = new TlsSearchController(tlsRepository);
    Model model = new ConcurrentModel();
    String viewName = controller.getTlsLatest(model, "abc.be");
    verify(tlsRepository).findLatestResult("abc.be");
    verifyNoMoreInteractions(tlsRepository);
    assertThat(model.containsAttribute("tlsCrawlResults")).isFalse();
  }

  @Test
  public void findLatestCrawlResult_IsFound() {
    tlsRepository = mock(TlsRepository.class);
    TlsCrawlResult crawlResult = objectMother.tlsCrawlResult1();
    when(tlsRepository.findLatestResult("abc.be")).thenReturn(Optional.of(crawlResult));
    TlsSearchController controller = new TlsSearchController(tlsRepository);
    Model model = new ConcurrentModel();
    String viewName = controller.getTlsLatest(model, "abc.be");
    verify(tlsRepository).findLatestResult("abc.be");
    verifyNoMoreInteractions(tlsRepository);
    assertThat(model.getAttribute("tlsCrawlResults")).isEqualTo(List.of(crawlResult));
  }

  @Test
  public void getIdsAreNotFound() {
    tlsRepository = mock(TlsRepository.class);
    logger.info("tlsRepository = {}", tlsRepository);
    List<String> emptyIdsList = List.of();
    when(tlsRepository.searchVisitIds("abc.be")).thenReturn(emptyIdsList);
    TlsSearchController controller = new TlsSearchController(tlsRepository);
    Model model = new ConcurrentModel();
    String viewName = controller.getTlsIds(model, "abc.be");
    logger.info("viewName = {}", viewName);
    logger.info("model = {}", model);
    verify(tlsRepository).searchVisitIds("abc.be");
    verifyNoMoreInteractions(tlsRepository);
    assertThat(model.containsAttribute("idList")).isFalse();
  }

  @Test
  public void getIdsAreFound() {
    tlsRepository = mock(TlsRepository.class);
    logger.info("tlsRepository = {}", tlsRepository);
    List<String> idsList = List.of("id1", "id2", "id3");
    when(tlsRepository.searchVisitIds("abc.be")).thenReturn(idsList);
    TlsSearchController controller = new TlsSearchController(tlsRepository);
    Model model = new ConcurrentModel();
    String viewName = controller.getTlsIds(model, "abc.be");
    logger.info("viewName = {}", viewName);
    logger.info("model = {}", model);
    verify(tlsRepository).searchVisitIds("abc.be");
    verifyNoMoreInteractions(tlsRepository);
    assertThat(model.getAttribute("idList")).isEqualTo(idsList);
  }

  @Test
  public void getByIdIsNotFound() {
    tlsRepository = mock(TlsRepository.class);
    logger.info("tlsRepository = {}", tlsRepository);
    TlsCrawlResult crawlResult = objectMother.tlsCrawlResult1();
    when(tlsRepository.findByVisitId("aakjkjkj-ojj")).thenReturn(Optional.empty());
    TlsSearchController controller = new TlsSearchController(tlsRepository);
    Model model = new ConcurrentModel();
    String viewName = controller.getTls(model, "aakjkjkj-ojj");
    logger.info("viewName = {}", viewName);
    logger.info("model = {}", model);
    verify(tlsRepository).findByVisitId("aakjkjkj-ojj");
    verifyNoMoreInteractions(tlsRepository);
    assertThat(model.containsAttribute("tlsCrawlResults")).isFalse();

  }

  @Test
  public void getByIdIsFound() {
    tlsRepository = mock(TlsRepository.class);
    logger.info("tlsRepository = {}", tlsRepository);
    TlsCrawlResult crawlResult = objectMother.tlsCrawlResult1();
    when(tlsRepository.findByVisitId("aakjkjkj-ojj")).thenReturn(Optional.of(crawlResult));
    TlsSearchController controller = new TlsSearchController(tlsRepository);
    Model model = new ConcurrentModel();
    String viewName = controller.getTls(model, "aakjkjkj-ojj");
    logger.info("viewName = {}", viewName);
    logger.info("model = {}", model);
    verify(tlsRepository).findByVisitId("aakjkjkj-ojj");
    verifyNoMoreInteractions(tlsRepository);
    assertThat(model.getAttribute("tlsCrawlResults")).isEqualTo(List.of(crawlResult));

  }


}