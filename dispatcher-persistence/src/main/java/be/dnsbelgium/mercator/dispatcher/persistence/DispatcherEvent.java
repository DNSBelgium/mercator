package be.dnsbelgium.mercator.dispatcher.persistence;

import be.dnsbelgium.mercator.common.messaging.ack.CrawlerModule;
import be.dnsbelgium.mercator.common.messaging.dto.DispatcherRequest;
import be.dnsbelgium.mercator.common.messaging.idn.ULabelConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "dispatcher_event")
public class DispatcherEvent implements Persistable<UUID> {

  @Id
  @Column(name = "visit_id")
  UUID visitId;

  @Column(name = "domain_name")
  @Convert(converter = ULabelConverter.class)
  String domainName;

  @ElementCollection
  @CollectionTable(name = "dispatcher_event_labels", joinColumns = @JoinColumn(name = "visit_id"))
  @Column(name = "labels")
  List<String> labels;

  @Column(name = "request_timestamp")
  ZonedDateTime requestTimestamp;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "dispatcher_event_acks", joinColumns = @JoinColumn(name = "visit_id"))
  @MapKeyEnumerated(EnumType.STRING)
  @Column(name = "acks")
  Map<CrawlerModule, ZonedDateTime> acks;

  public static DispatcherEvent from(UUID visitId, DispatcherRequest dispatcherRequest) {
    return new DispatcherEvent(visitId, dispatcherRequest.getDomainName(), dispatcherRequest.getLabels());
  }

  public DispatcherEvent(UUID visitId, String domainName, List<String> labels) {
    this.visitId = visitId;
    this.domainName = domainName;
    this.labels = labels;
    this.requestTimestamp = ZonedDateTime.now();
    this.acks = new HashMap<>();
  }

  public void ack(CrawlerModule crawlerModule) {
    acks.put(crawlerModule, ZonedDateTime.now());
  }

  @Override
  public UUID getId() {
    return visitId;
  }

  @Override
  public boolean isNew() {
    // We only want to update an existing DispatcherEvent to add acks.
    // As long as there are no acks:
    // if visitId == null --> insert a new row and let JPA generate a UUID
    // if visitId != null --> insert a new row and fail when event with this visitId already exists

    // If two dispatcher events are, at the same time saved to the database, both will try to do an insert
    // since acks are empty. One of the two will then trigger a DuplicateKeyViolation.
    // We want that behavior in order to not update existing DispatcherEvent.
    return acks.isEmpty();
  }
}
