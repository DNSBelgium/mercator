package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.persistence.SearchRepository;
import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
public class SearchResultTest {

  @MockitoBean
  private SearchRepository searchRepository;

  private final ObjectMother objectMother = new ObjectMother();

  @Autowired
  private MockMvc mockMvc;

  @Test
  public void test() throws Exception {
    System.out.println(objectMother);
  }

  @Test
  public void shouldReturnViewWithPrefilledData() throws Exception {
    WebCrawlResult webCrawlResult1 = objectMother.webCrawlResult1();
    WebCrawlResult webCrawlResult2 = objectMother.webCrawlResult1();
    when(searchRepository.searchVisitIdsWeb("dnsbelgium.be")).thenReturn(List.of(webCrawlResult1, webCrawlResult2));

    // Matchers.arrayContaining(webCrawlResult1, webCrawlResult2).toString()

    this.mockMvc
            .perform(MockMvcRequestBuilders.get("/search/web"))
            .andExpect(view().name("search"))
           // .andExpect(model().attributeExists( "tlsCrawlResults"))
           // .andExpect(model().attributeExists("search"))
            .andExpect(content().string(containsString("BE0466158640")))
    ;

  }

}