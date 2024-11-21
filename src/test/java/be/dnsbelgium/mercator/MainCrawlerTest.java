package be.dnsbelgium.mercator;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.visits.MainCrawler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SuppressWarnings("CommentedOutCode")
@SpringBootTest(classes = { MercatorApplication.class })
@ActiveProfiles({"test", "local"})
@EnabledIfEnvironmentVariable(named = "MainCrawlerTest.enabled", matches = "true")
class MainCrawlerTest {

    private static final Logger logger = LoggerFactory.getLogger(MainCrawlerTest.class);

    @Autowired
    MainCrawler mainCrawler;
    
    @Test
    public void dnsbelgium_be() {
        logger.info("true = " + true);
        logger.info("mainCrawler = {}", mainCrawler);
        VisitRequest dnsbelgium = new VisitRequest("dnsbelgium.be");
        mainCrawler.visit(dnsbelgium);
    }

    @Test
    public void idn() {
        VisitRequest request = new VisitRequest("gasheizkörper.de");
        mainCrawler.visit(request);
    }

//    @Test
//    public void vat() {
//        VisitRequest dnsbelgium = new VisitRequest("dnsbelgium.be");
//        mainCrawler.vat(dnsbelgium);
//    }

//    @Test
//    public void dnsVisit() {
//        mainCrawler.dnsVisit();
//    }

//    @Test
//    public void smtp() {
//        mainCrawler.smtp();
//    }


}