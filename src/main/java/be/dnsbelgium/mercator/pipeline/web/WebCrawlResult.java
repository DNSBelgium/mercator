package be.dnsbelgium.mercator.pipeline.web;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class WebCrawlResult {

    private String html;
    private String bodyText;
    private String title;
    private int numTags;
    private int numInputs;
    private int numWords;

}
