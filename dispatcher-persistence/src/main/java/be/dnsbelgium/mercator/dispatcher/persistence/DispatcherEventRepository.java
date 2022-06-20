package be.dnsbelgium.mercator.dispatcher.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DispatcherEventRepository extends PagingAndSortingRepository<DispatcherEvent, UUID> {

  // Method is used by the User Interface via Mercator REST API
  @SuppressWarnings("unused")
  List<DispatcherEvent> findDispatcherEventByDomainName(@Param("domainName") String domainName);

  // Returns a Page with DispatcherEvents dependent on the given parameters.
  Page<DispatcherEvent> findDispatcherEventByDomainName(@Param("domainName") String domainName, Pageable p);

  static boolean exceptionContains(Exception exception, String substring) {
    Throwable cause = exception;
    boolean found = false;
    while (cause != null && !found) {
      found = (cause.getMessage().contains(substring));
      if (cause.getCause() == cause) {
        // avoid endless loops
        break;
      }
      cause = cause.getCause();
    }
    return found;
  }

}
