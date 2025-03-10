package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.persistence.WebRepository;
import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

class WebSearchControllerTest {

    WebRepository webRepository;
    private static final Logger logger = LoggerFactory.getLogger(WebSearchControllerTest.class);
    private final ObjectMother objectMother = new ObjectMother();

    @Test
    public void findLatestCrawlResult_NotFound() {
        webRepository = mock(WebRepository.class);
        logger.info("webRepository = {}", webRepository);
        when(webRepository.findLatestResult("abc.be")).thenReturn(Optional.empty());
        WebSearchController controller = new WebSearchController(webRepository);
        Model model = new ConcurrentModel();
        String viewName = controller.getWebLatest(model, "abc.be");
        verify(webRepository).findLatestResult("abc.be");
        verifyNoMoreInteractions(webRepository);
        assertThat(model.containsAttribute("webCrawlResults")).isFalse();
    }

    @Test
    public void findLatestCrawlResult_IsFound() {
        webRepository = mock(WebRepository.class);
        WebCrawlResult crawlResult = objectMother.webCrawlResult1();
        when(webRepository.findLatestResult("abc.be")).thenReturn(Optional.of(crawlResult));
        WebSearchController controller = new WebSearchController(webRepository);
        Model model = new ConcurrentModel();
        String viewName = controller.getWebLatest(model, "abc.be");
        verify(webRepository).findLatestResult("abc.be");
        verifyNoMoreInteractions(webRepository);
        assertThat(model.getAttribute("webCrawlResults")).isEqualTo(List.of(crawlResult));
    }

    @Test
    public void getIdsAreNotFound() {
        webRepository = mock(WebRepository.class);
        logger.info("webRepository = {}", webRepository);
        List<String> emptyIdsList = List.of();
        when(webRepository.searchVisitIds("abc.be")).thenReturn(emptyIdsList);
        WebSearchController controller = new WebSearchController(webRepository);
        Model model = new ConcurrentModel();
        String viewName = controller.getWebIds(model, "abc.be");
        logger.info("viewName = {}", viewName);
        logger.info("model = {}", model);
        verify(webRepository).searchVisitIds("abc.be");
        verifyNoMoreInteractions(webRepository);
        assertThat(model.containsAttribute("idList")).isFalse();
    }

    @Test
    public void getIdsAreFound() {
        webRepository = mock(WebRepository.class);
        logger.info("webRepository = {}", webRepository);
        List<String> idsList = List.of("id1", "id2", "id3");
        when(webRepository.searchVisitIds("abc.be")).thenReturn(idsList);
        WebSearchController controller = new WebSearchController(webRepository);
        Model model = new ConcurrentModel();
        String viewName = controller.getWebIds(model, "abc.be");
        logger.info("viewName = {}", viewName);
        logger.info("model = {}", model);
        verify(webRepository).searchVisitIds("abc.be");
        verifyNoMoreInteractions(webRepository);
        assertThat(model.getAttribute("visitIds")).isEqualTo(idsList);
    }

    @Test
    public void getByIdIsNotFound() {
        webRepository = mock(WebRepository.class);
        logger.info("webRepository = {}", webRepository);
        WebCrawlResult crawlResult = objectMother.webCrawlResult1();
        when(webRepository.findByVisitId("aakjkjkj-ojj")).thenReturn(Optional.empty());
        WebSearchController controller = new WebSearchController(webRepository);
        Model model = new ConcurrentModel();
        String viewName = controller.getWeb(model, "aakjkjkj-ojj");
        logger.info("viewName = {}", viewName);
        logger.info("model = {}", model);
        verify(webRepository).findByVisitId("aakjkjkj-ojj");
        verifyNoMoreInteractions(webRepository);
        assertThat(model.containsAttribute("webCrawlResults")).isFalse();

    }

    @Test
    public void getByIdIsFound() {
        webRepository = mock(WebRepository.class);
        logger.info("webRepository = {}", webRepository);
        WebCrawlResult crawlResult = objectMother.webCrawlResult1();
        when(webRepository.findByVisitId("aakjkjkj-ojj")).thenReturn(Optional.of(crawlResult));
        WebSearchController controller = new WebSearchController(webRepository);
        Model model = new ConcurrentModel();
        String viewName = controller.getWeb(model, "aakjkjkj-ojj");
        logger.info("viewName = {}", viewName);
        logger.info("model = {}", model);
        verify(webRepository).findByVisitId("aakjkjkj-ojj");
        verifyNoMoreInteractions(webRepository);
        assertThat(model.getAttribute("webCrawlResults")).isEqualTo(List.of(crawlResult));

    }


}