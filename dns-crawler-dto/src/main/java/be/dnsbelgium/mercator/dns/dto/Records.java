package be.dnsbelgium.mercator.dns.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class Records implements Serializable {

  private final Map<RecordType, RRSet> records;

  public Records() {
    this.records = new HashMap<>();
  }

  public Records(Map<RecordType, RRSet> records) {
    this.records = new HashMap<>(records);
  }

  public Records(RecordType recordType, RRSet rrSet) {
    this(Map.of(recordType, rrSet));
  }

  public RRSet get(RecordType type) {
    return records.getOrDefault(type, null);
  }

//  public Map<RecordType, Set<RRecord>> get(List<RecordType> types) {
//    return types.stream()
//        .collect(Collectors.toMap((type) -> type, this::get));
//  }

  public Records add(Map<RecordType, RRSet> toAdd) {
    toAdd.forEach((key, value) -> records.merge(key, value, (v1, v2) -> {
      if (v1.rcode() != v2.rcode()) {
        throw new IllegalArgumentException("Rcodes cannot be different in order to merge RRSets");
      }
      return new RRSet(Stream.concat(v1.records().stream(), v2.records().stream()).collect(Collectors.toSet()), v1.rcode());
    }));
    return this;
  }

  public Records add(Records toAdd) {
    add(toAdd.records);
    return this;
  }

  public static Records merge(Records r1, Records r2) {
    Records records = new Records();
    records.add(r1);
    records.add(r2);
    return records;
  }
}
