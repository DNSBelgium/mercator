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

// TODO: Bram: we probably do not need this test anymore.

class WebSearchControllerTest {

    private WebRepository webRepository;
    private static final Logger logger = LoggerFactory.getLogger(WebSearchControllerTest.class);
    private final ObjectMother objectMother = new ObjectMother();

    @Test
    public void findLatestResult_NotFound() {
        webRepository = mock(WebRepository.class);
        logger.info("webRepository = {}", webRepository);
        when(webRepository.findLatestResult("abc.be")).thenReturn(Optional.empty());
        WebSearchController controller = new WebSearchController(webRepository);
        Model model = new ConcurrentModel();
        controller.findLatestResult(model, "abc.be");
        verify(webRepository).findLatestResult("abc.be");
        verifyNoMoreInteractions(webRepository);
        assertThat(model.containsAttribute("webCrawlResults")).isFalse();
    }


    @Test
    public void searchVisitIds_NotFound() {
        webRepository = mock(WebRepository.class);
        logger.info("webRepository = {}", webRepository);
        when(webRepository.searchVisitIds("abc.be")).thenReturn(List.of());
        WebSearchController controller = new WebSearchController(webRepository);
        Model model = new ConcurrentModel();
        String viewName = controller.searchVisitIds(model, "abc.be");
        logger.info("viewName = {}", viewName);
        logger.info("model = {}", model);
        verify(webRepository).searchVisitIds("abc.be");
        verifyNoMoreInteractions(webRepository);
        assertThat(model.containsAttribute("idList")).isFalse();
    }

    @Test
    public void searchVisitIds_Found() {
        webRepository = mock(WebRepository.class);
        logger.info("webRepository = {}", webRepository);
        List<String> visitIds = List.of("id1", "id2", "id3");
        when(webRepository.searchVisitIds("abc.be")).thenReturn(visitIds);
        WebSearchController controller = new WebSearchController(webRepository);
        Model model = new ConcurrentModel();
        String viewName = controller.searchVisitIds(model, "abc.be");
        logger.info("viewName = {}", viewName);
        logger.info("model = {}", model);
        verify(webRepository).searchVisitIds("abc.be");
        verifyNoMoreInteractions(webRepository);
        assertThat(model.getAttribute("visitIds")).isEqualTo(visitIds);
    }

    @Test
    public void findByVisitId_NotFound() {
        webRepository = mock(WebRepository.class);
        logger.info("webRepository = {}", webRepository);
        when(webRepository.findByVisitId("aakjkjkj-ojj")).thenReturn(Optional.empty());
        WebSearchController controller = new WebSearchController(webRepository);
        Model model = new ConcurrentModel();
        String viewName = controller.findByVisitId(model, "aakjkjkj-ojj");
        logger.info("viewName = {}", viewName);
        logger.info("model = {}", model);
        verify(webRepository).findByVisitId("aakjkjkj-ojj");
        verifyNoMoreInteractions(webRepository);
        assertThat(model.containsAttribute("webCrawlResults")).isFalse();

    }

    @Test
    public void findByVisitId_IsFound() {
        webRepository = mock(WebRepository.class);
        logger.info("webRepository = {}", webRepository);
        WebCrawlResult crawlResult = objectMother.webCrawlResult1();
        when(webRepository.findByVisitId("aakjkjkj-ojj")).thenReturn(Optional.of(crawlResult));
        WebSearchController controller = new WebSearchController(webRepository);
        Model model = new ConcurrentModel();
        String viewName = controller.findByVisitId(model, "aakjkjkj-ojj");
        logger.info("viewName = {}", viewName);
        logger.info("model = {}", model);
        verify(webRepository).findByVisitId("aakjkjkj-ojj");
        verifyNoMoreInteractions(webRepository);
        assertThat(model.getAttribute("webCrawlResults")).isEqualTo(List.of(crawlResult));
    }

}