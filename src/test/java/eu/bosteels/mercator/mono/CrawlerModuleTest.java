package eu.bosteels.mercator.mono;

import be.dnsbelgium.mercator.common.VisitRequest;
import org.junit.jupiter.api.Test;

import java.util.*;

class CrawlerModuleTest {

  public interface VisitResult {
    default void saveWith(Crawler<?> crawler) {

      System.out.println("this.getClass() = " + this.getClass());

      crawler.save(this);
    }
  }

  public interface Crawler <T extends VisitResult> {
    List<T> collectData(VisitRequest request);
    List<T> find(String visitId);
    void save(VisitResult visitResult);
    void saveT(T t);
    default void saveItem(VisitResult visitResult) {
      try {
        @SuppressWarnings("unchecked")
        T t = (T) visitResult;
        save(t);
      } catch (ClassCastException e) {
        e.printStackTrace();
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

    public void save(VisitResult visitResult) {
      System.out.println("SAVED visitResult = " + visitResult);
      if (visitResult instanceof MyVisitResult myVisitResult) {
        save(myVisitResult);
      }
    }

    public void save(MyVisitResult myVisitResult) {
      System.out.println("saved myVisitResult = " + myVisitResult);
    }


    @Override
    public void saveT(MyVisitResult myVisitResult) {
      System.out.println("saveT myVisitResult = " + myVisitResult);
    }

    @Override
    public void afterSave(Object t) {
      System.out.println("afterSave myVisitResult = " + t);
    }
  }

  @Test
  public void test() {
    MyCrawler crawler = new MyCrawler();
    visit(List.of(crawler), new VisitRequest("abc.be"));
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