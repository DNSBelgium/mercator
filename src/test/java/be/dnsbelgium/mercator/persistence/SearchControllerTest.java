package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.mvc.SearchController;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

class SearchControllerTest {



    @Test
    public void tls() {
        SearchRepository repository = new SearchRepository();
        SearchController controller = new SearchController(repository);
        Model model = new ConcurrentModel();
        //controller.search(model, "dnsbelgium.be");
        //String json = controller.visit(model, "dnsbelgium.be", "tls");
        // System.out.println("json = " + json);
    }

    @Test
    public void searchLatestTls() throws JsonProcessingException {
        SearchRepository repository = new SearchRepository();
        SearchController controller = new SearchController(repository);
        Model model = new ConcurrentModel();
        String xf = controller.searchTls(model, "dnsbelgium.be", "on");
        System.out.println(xf);

    }

}