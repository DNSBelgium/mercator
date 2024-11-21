package eu.bosteels.mercator.mono.visits;

import be.dnsbelgium.mercator.common.VisitRequest;

import java.util.List;

public interface CrawlerModule <T> {

  List<T> collectData(VisitRequest visitRequest);

  void save(List<?> collectedData);

  void saveItem(T item);

  void afterSave(List<?> collectedData);

  List<T> find(String visitId);

  void createTables();


  default void saveAll(List<T> collectedData) {
    for (T t : collectedData) {
      saveItem(t);
    }
  }

  default String key() {
    return getClass().getName();
  }

}
