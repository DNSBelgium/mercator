package be.dnsbelgium.mercator.feature.extraction;

import be.dnsbelgium.mercator.common.VisitIdGenerator;
import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.persistence.DuckDataSource;
import be.dnsbelgium.mercator.vat.WebCrawler;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.ArrayList;
import java.util.List;

@Disabled // TODO solve the OOM
@SpringBootTest
class MercatorLanguageDetectorTest {

    private static final Logger logger = LoggerFactory.getLogger(MercatorLanguageDetectorTest.class);
    private static final MercatorLanguageDetector   MODEL = new MercatorLanguageDetector();

    @Autowired
    private WebCrawler webCrawler;


    private final JdbcClient jdbcClient = JdbcClient.create(DuckDataSource.memory());
    @Test
    public void detectDutch() {
        MercatorLanguageDetector detector = new MercatorLanguageDetector();
        var lang = detector.detectCommonLanguageOf("Ik ben soms stout");
        logger.info("lang = {}", lang);
    }

    @Test
    public void detectDutchMemoryUsage() {

        Runtime runtime = Runtime.getRuntime();
        for (int i = 0; i < 10; i++) {
            long before = runtime.totalMemory() - runtime.freeMemory();
            var lang = MODEL.detectCommonLanguageOf("Ik ben soms stout");
            long after = runtime.totalMemory() - runtime.freeMemory();

            logger.info("Memory used: {} bytes, in Mb: {}", (after - before), ((after - before) / 1_048_576.0));
            logger.info("Detected language: {}", lang);

        }

    }

    @Test
    public void detectFromEvenMoreOfChineseMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        for (int i = 0; i<10; i++ ) {
            long before = runtime.totalMemory() - runtime.freeMemory();
            var lang = MODEL.detectCommonLanguageOf("开始时，域名的字符仅限于ASCII字符的一个子集。2008年，ICANN通过一项决议，允许使用其它语言作为互联网顶级域名的字符。使用基于Punycode码的IDNA系统，可以将Unicode字符串映射为有效的DNS字符集。因此，诸如“XXX.中国”、“XXX.台灣”的域名可以在地址栏直接输入并访问，而不需要安装插件。但是，由于英语的广泛使用，使用其他语言字符作为域名会产生多种问题，例如难以输入、难以在国际推广等。 ");
            long after = runtime.totalMemory() - runtime.freeMemory();
            logger.info("Memory used: {} bytes", (after - before));
        }

    }

    @Test
    public void detectFromChineseAndDutch() {
        Runtime runtime = Runtime.getRuntime();
        for (int i = 0; i<10; i++ ) {
            long before = runtime.totalMemory() - runtime.freeMemory();
            var lang = MODEL.detectCommonLanguageOf("Het Domain Name System (DNS) is het systeem en netwerkprotocol dat op het internet gebruikt wordt om namen van internetdomeinen naar numerieke adressen (IP-adressen) te vertalen en omgekeerd. Hoewel dit \"vertalen\" genoemd wordt, is het niet meer dan opzoeken van namen in tabellen waaraan nummers gekoppeld zijn."
            +"\n" + "網域名稱系統 (DNS) 是互聯網上使用的系統和網路協議，用於將網域名稱轉換為數位位址（IP 位址），反之亦然。儘管這稱為“翻譯”，但它只是在表中查找具有與其關聯的數字的名稱。");
            long after = runtime.totalMemory() - runtime.freeMemory();
            logger.info("Memory used: {} bytes", (after - before));
            logger.info("lang = {}", lang);
        }

    }

    @Test
    public void detectFromAlotOfText() {
        Runtime runtime = Runtime.getRuntime();
        for (int i = 0; i<10; i++ ) {
            long before = runtime.totalMemory() - runtime.freeMemory();
            var lang = MODEL.detectCommonLanguageOf("According to Tech Jury, despite a number of cool apps and tips for successful time management, only 17% of people track their time. 50% of people have never thought about time waste, even though they are always late and running out of time. Time management is a skill. It helps people handle their daily duties without burnout and severe exhaustion. The N.I.L.C. includes time management on the list of top ten demanded soft skills that employees require in 2022. Why is it so important to manage one’s time correctly? Stephen Covey once said, “The key is not spending time, but in investing it”. It means that proper timing guarantees a person’s success in many life areas.\n".repeat(20));
            long after = runtime.totalMemory() - runtime.freeMemory();
            logger.info("Memory used: {} bytes, in Mb: {}", (after - before), (after - before) / 1_048_576.0); // 18113984 bytes, 14495816 bytes, 32524936 bytes, 46461024 bytes, 44885960 bytes
            logger.info("lang = {}", lang);
        }

    }

    @Test
    public void detectWithActualCrawls() {
        int crawlAmount = 100;

        List<String> domainNames = jdbcClient
                .sql("select domain_name from 'tranco_be.parquet' order by tranco_rank DESC limit ?")
                .param(crawlAmount)
                .query(String.class)
                .list();
        List<VisitRequest> visitRequests = new ArrayList<>();
        for (String domainName: domainNames) {
            VisitRequest visitRequest = new VisitRequest(VisitIdGenerator.generate(), domainName);
            visitRequests.add(visitRequest);
        }


        List<Long> memoryAmounts = new ArrayList<>();


        for (VisitRequest visitRequest: visitRequests) {
            try {
                WebCrawlResult webCrawlResult = webCrawler.crawl(visitRequest);
                logger.info(webCrawlResult.getDomainName());
                logger.info(webCrawlResult.getHtmlFeatures().getFirst().body_text_language);
                logger.info(webCrawlResult.getHtmlFeatures().getFirst().body_text_language_2);
            } catch (Exception e) {
                logger.error("Error crawling domain: " + visitRequest.getDomainName(), e);
            }
            long currentMemoryUsed = logMemoryUsage();
            memoryAmounts.add(currentMemoryUsed);
        }
        long totalMemoryUsed = memoryAmounts.stream().mapToLong(Long::longValue).sum();
        long averageMemoryUsed = memoryAmounts.isEmpty() ? 0 : totalMemoryUsed / memoryAmounts.size();

        logger.info("Average memory used per crawl: {} MB", averageMemoryUsed);

    }
    private long logMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        logger.info("Memory Usage: Used={}MB, Total={}MB, Max={}MB", usedMemory, totalMemory, maxMemory);
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }

}