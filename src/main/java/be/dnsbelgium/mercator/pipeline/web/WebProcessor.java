package be.dnsbelgium.mercator.pipeline.web;

import be.dnsbelgium.mercator.pipeline.service.ItemProcessor;
import be.dnsbelgium.mercator.pipeline.service.VisitRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component(value = "WebProcessor_todo")
public class WebProcessor implements ItemProcessor<VisitRequest, WebCrawlResult> {

    @Override
    public WebCrawlResult processItem(VisitRequest visitRequest) {

        log.info("Processing item: {}", visitRequest);

        // TODO: use OKHttpClient to fetch the page and extract the required information

        WebCrawlResult result = WebCrawlResult.builder()
                .bodyText("Sample body text")
                .html("<html><title>Title</title><body>Sample body text</body></html>")
                .title("Title")
                .numInputs(1)
                .numTags(13)
                .numWords(5)
                .build();

        log.info("result = {}", result);
        return result;
    }
}
