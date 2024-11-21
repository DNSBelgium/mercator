package be.dnsbelgium.mercator.smtp.domain.crawler;

import lombok.Getter;
import org.xbill.DNS.MXRecord;

import java.util.ArrayList;
import java.util.List;

@Getter
public class MxLookupResult {

  private final List<MXRecord> mxRecords = new ArrayList<>();
  private final Status status;

  private MxLookupResult(Status status) {
    this.status = status;
  }

  private MxLookupResult(Status status, List<MXRecord> mxRecords) {
    this.status = status;
    this.mxRecords.addAll(mxRecords);
  }

  public static MxLookupResult ok(List<MXRecord> mxRecords) {
    return new MxLookupResult(Status.OK, mxRecords);
  }

  public static MxLookupResult invalidHostName() {
    return new MxLookupResult(Status.INVALID_HOSTNAME);
  }

  public static MxLookupResult noMxRecordsFound() {
    return new MxLookupResult(Status.NO_MX_RECORDS_FOUND);
  }

  public static MxLookupResult queryFailed() {
    return new MxLookupResult(Status.QUERY_FAILED);
  }

  public enum Status {
    OK,
    INVALID_HOSTNAME,
    NO_MX_RECORDS_FOUND,
    QUERY_FAILED
  }
}
