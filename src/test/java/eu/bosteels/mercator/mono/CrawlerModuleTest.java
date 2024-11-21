package eu.bosteels.mercator.mono;

import be.dnsbelgium.mercator.common.VisitRequest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

class CrawlerModuleTest {

  private static final Logger logger = LoggerFactory.getLogger(CrawlerModuleTest.class);

  public interface VisitResult {
    default void saveWith(Crawler<?> crawler) {
      logger.info("this.getClass() = {}", this.getClass());
      crawler.saveItem(this);
    }
  }

  public interface Crawler <T extends VisitResult> {

    List<T> collectData(VisitRequest request);

    List<MyVisitResult> find(String visitId);

    void save(T t);

    default void saveItem(VisitResult visitResult) {
      try {
        @SuppressWarnings("unchecked")
        T t = (T) visitResult;
        save(t);
      } catch (ClassCastException e) {
        logger.error(e.getMessage(), e);
      }
    }

    default void save(List<? extends VisitResult> list) {
      for (VisitResult visitResult : list) {
        saveItem(visitResult);
      }
    }

    void afterSave(Object t);

  }

  public void visit(List<Crawler<?>> crawlers, VisitRequest visitRequest) {

    Map<Crawler<?>, List<? extends VisitResult>> dataPerCrawler = new HashMap<>();

    for (Crawler<?> crawler : crawlers) {
      List<? extends VisitResult> data = crawler.collectData(visitRequest);
      dataPerCrawler.put(crawler, data);
    }

    for (Crawler<?> crawler : dataPerCrawler.keySet()) {
      List<? extends VisitResult> data = dataPerCrawler.get(crawler);
      for (VisitResult item : data) {
        crawler.saveItem(item);
      }
    }

    for (Crawler<?> crawler : dataPerCrawler.keySet()) {
      List<?> data = dataPerCrawler.get(crawler);
      for (Object item : data) {
        crawler.afterSave(item);
      }
      crawler.find("some-visit-id");
    }
  }

  public static class MyVisitResult implements VisitResult {
    private final String data;

    public MyVisitResult(String data) {
      this.data = data;
    }

    @Override
    public String toString() {
      return "MyVisitResult{" +
              "data='" + data + '\'' +
              '}';
    }
  }

  public static class MyCrawler implements Crawler<MyVisitResult> {

    @Override
    public List<MyVisitResult> collectData(VisitRequest request) {
      return List.of(new MyVisitResult("abc"));
    }

    @Override
    public List<MyVisitResult> find(String visitId) {
      return List.of(new MyVisitResult("abc"));
    }

    @Override
    public void save(MyVisitResult myVisitResult) {
      logger.info("saved myVisitResult = " + myVisitResult);
    }

    @Override
    public void afterSave(Object t) {
      logger.info("afterSave myVisitResult = {}", t);
    }
  }

  @Test
  public void test() {
    MyCrawler crawler = new MyCrawler();
    visit(List.of(crawler), new VisitRequest("abc.be"));

    List<MyVisitResult> list = crawler.find("abc-578e7-45");
    logger.info("list = {}", list);
  }

  @Test
  public void saveWith() {
    MyCrawler crawler = new MyCrawler();
    List<MyVisitResult> data = crawler.collectData(new VisitRequest("abc"));

    for (MyVisitResult myVisitResult : data) {
      myVisitResult.saveWith(crawler);
    }
  }


}