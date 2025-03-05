package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.mvc.SearchController;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

class SearchControllerTest {

    @Test
    public void tls() {
        // TODO: this  method doesn't do anything ?
        SearchRepository repository = new SearchRepository();
        SearchController controller = new SearchController(repository);
        Model model = new ConcurrentModel();
        //controller.search(model, "dnsbelgium.be");
        //String json = controller.visit(model, "dnsbelgium.be", "tls");
        // System.out.println("json = " + json);
    }

    @Test
    @Disabled
    public void searchLatestTls() throws JsonProcessingException {
        // TODO: use a mocked Repository when testing the SearchController
        SearchRepository repository = new SearchRepository();
        // TODO: SearchRepository.searchlatestTlsResult should return an Optional<TlsCrawlResult>
        SearchController controller = new SearchController(repository);
        Model model = new ConcurrentModel();
        String xf = controller.searchTls(model, "dnsbelgium.be", "on");
        System.out.println(xf);
        // TODO: do not write to System.out
        // TODO: add relevant assert statements

    }

}