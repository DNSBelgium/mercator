package be.dnsbelgium.mercator.dns.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class Records implements Serializable {

  private final Map<RecordType, List<String>> records;

  public Records() {
    this.records = new HashMap<>();
  }

  public Records(Map<RecordType, List<String>> records) {
    this.records = new HashMap<>(records);
  }

  public List<String> get(RecordType type) {
    return records.getOrDefault(type, Collections.emptyList());
  }

  public Map<RecordType, List<String>> get(List<RecordType> types) {
    return types.stream()
        .collect(Collectors.toMap((type) -> type, this::get));
  }

  private Records add(Map<RecordType, List<String>> toAdd) {
    toAdd.forEach((key, value) -> records.merge(key, value, (v1, v2) -> Stream.concat(v1.stream(), v2.stream()).collect(Collectors.toList())));
    return this;
  }

  /**
   * Add records to the current records map. Note that we don't prevent adding duplicate records.
   *
   * @param toAdd Records to add to the current Records
   * @return this, allowing to chain add methods.
   */
  public Records add(Records toAdd) {
    return add(toAdd.getRecords());
  }
}
