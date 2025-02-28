package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.persistence.SearchRepository;
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
        String json = controller.visit(model, "dnsbelgium.be", "tls");
        System.out.println("json = " + json);
    }

}