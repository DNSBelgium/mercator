package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.persistence.SmtpRepository;
import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

class SmtpSearchControllerTest {

    SmtpRepository smtpRepository;
    private static final Logger logger = LoggerFactory.getLogger(SmtpSearchControllerTest.class);
    private final ObjectMother objectMother = new ObjectMother();

    @Test
    public void findLatestCrawlResult_NotFound() {
        smtpRepository = mock(SmtpRepository.class);
        logger.info("smtpRepository = {}", smtpRepository);
        when(smtpRepository.findLatestResult("abc.be")).thenReturn(Optional.empty());
        SmtpSearchController controller = new SmtpSearchController(smtpRepository);
        Model model = new ConcurrentModel();
        String viewName = controller.getLatestSmtp(model, "abc.be");
        verify(smtpRepository).findLatestResult("abc.be");
        verifyNoMoreInteractions(smtpRepository);
        assertThat(model.containsAttribute("smtpConversationResults")).isFalse();
    }

    @Test
    public void findLatestCrawlResult_IsFound() {
        smtpRepository = mock(SmtpRepository.class);
        SmtpConversation smtpConversation = objectMother.smtpConversation1();
        when(smtpRepository.findLatestResult("abc.be")).thenReturn(Optional.of(smtpConversation));
        SmtpSearchController controller = new SmtpSearchController(smtpRepository);
        Model model = new ConcurrentModel();
        String viewName = controller.getLatestSmtp(model, "abc.be");
        verify(smtpRepository).findLatestResult("abc.be");
        verifyNoMoreInteractions(smtpRepository);
        assertThat(model.getAttribute("smtpConversationResults")).isEqualTo(List.of(smtpConversation));
    }

    @Test
    public void getIdsAreNotFound() {
        smtpRepository = mock(SmtpRepository.class);
        logger.info("smtpRepository = {}", smtpRepository);
        List<String> emptyIdsList = List.of();
        when(smtpRepository.searchVisitIds("abc.be")).thenReturn(emptyIdsList);
        SmtpSearchController controller = new SmtpSearchController(smtpRepository);
        Model model = new ConcurrentModel();
        String viewName = controller.getSmtpIds(model, "abc.be");
        logger.info("viewName = {}", viewName);
        logger.info("model = {}", model);
        verify(smtpRepository).searchVisitIds("abc.be");
        verifyNoMoreInteractions(smtpRepository);
        assertThat(model.containsAttribute("idList")).isFalse();
    }

    @Test
    public void getIdsAreFound() {
        smtpRepository = mock(SmtpRepository.class);
        logger.info("smtpRepository = {}", smtpRepository);
        List<String> idsList = List.of("id1", "id2", "id3");
        when(smtpRepository.searchVisitIds("abc.be")).thenReturn(idsList);
        SmtpSearchController controller = new SmtpSearchController(smtpRepository);
        Model model = new ConcurrentModel();
        String viewName = controller.getSmtpIds(model, "abc.be");
        logger.info("viewName = {}", viewName);
        logger.info("model = {}", model);
        verify(smtpRepository).searchVisitIds("abc.be");
        verifyNoMoreInteractions(smtpRepository);
        assertThat(model.getAttribute("visitIds")).isEqualTo(idsList);
    }

    @Test
    public void getByIdIsNotFound() {
        smtpRepository = mock(SmtpRepository.class);
        logger.info("smtpRepository = {}", smtpRepository);
        when(smtpRepository.findByVisitId("aakjkjkj-ojj")).thenReturn(Optional.empty());
        SmtpSearchController controller = new SmtpSearchController(smtpRepository);
        Model model = new ConcurrentModel();
        String viewName = controller.getSmtp(model, "aakjkjkj-ojj");
        logger.info("viewName = {}", viewName);
        logger.info("model = {}", model);
        verify(smtpRepository).findByVisitId("aakjkjkj-ojj");
        verifyNoMoreInteractions(smtpRepository);
        assertThat(model.containsAttribute("smtpConversationResults")).isFalse();

    }

    @Test
    public void getByIdIsFound() {
        smtpRepository = mock(SmtpRepository.class);
        logger.info("smtpRepository = {}", smtpRepository);
        SmtpConversation smtpConversation = objectMother.smtpConversation1();
        when(smtpRepository.findByVisitId("aakjkjkj-ojj")).thenReturn(Optional.of(smtpConversation));
        SmtpSearchController controller = new SmtpSearchController(smtpRepository);
        Model model = new ConcurrentModel();
        String viewName = controller.getSmtp(model, "aakjkjkj-ojj");
        logger.info("viewName = {}", viewName);
        logger.info("model = {}", model);
        verify(smtpRepository).findByVisitId("aakjkjkj-ojj");
        verifyNoMoreInteractions(smtpRepository);
        assertThat(model.getAttribute("smtpConversationResults")).isEqualTo(List.of(smtpConversation));

    }


}