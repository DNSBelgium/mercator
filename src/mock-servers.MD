# PageFetcherTest

* uses okhttp3.mockwebserver.MockWebServer   
* ONLY in fetchPageWithoutContentLengthHeaderAndBodyLengthOverMax


# VatScraperTest

* uses com.github.tomakehurst.wiremock.junit.WireMockRule;
* init()
* urlFor()
* stubFor()
   
indirect:

* noVatOnFirstPage => ok
* vatFoundOnSecondPage => ok
* vatFoundOnFirstPage => ok
* wrongFormat => ok
* wrongCheckSum => ok
* 
* fetchPageAndParseTestWith1PageThatErrors => disabled
* => takes 9 seconds


* uses okhttp3.mockwebserver.MockWebServer;
 * ONLY in fetchPageAndParseTestWith1PageThatErrors