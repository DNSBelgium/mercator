package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.dns.domain.DnsCrawlResult;
import be.dnsbelgium.mercator.dns.dto.RecordType;
import be.dnsbelgium.mercator.dns.persistence.Request;
import be.dnsbelgium.mercator.dns.persistence.Response;
import be.dnsbelgium.mercator.dns.persistence.ResponseGeoIp;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//import static be.dnsbelgium.mercator.persistence.Repository.getLong;
//import static be.dnsbelgium.mercator.persistence.Repository.zonedDateTime;
//import org.apache.commons.lang3.tuple.Pair;
//
public class DnsResultsRowCallbackHandler { //implements RowCallbackHandler {
//
//    private String prev_response_id = null;
//    private String prev_request_id = null;
//    private Request currentRequest = null;
//    private Response currentResponse = null;
//
//    private final List<Request> requests = new ArrayList<>();
//    private List<Response> responses = new ArrayList<>();
//
//    private static final Logger logger = LoggerFactory.getLogger(DnsResultsRowCallbackHandler.class);
//
//    public Optional<DnsCrawlResult> getDnsCrawlResult() {
//        if (requests.isEmpty()) {
//           return Optional.empty();
//        }
//        var result = DnsCrawlResult.of(requests);
//        return Optional.of(result);
//    }
//
//    @Override
//    public void processRow(@NotNull ResultSet rs) throws SQLException {
//        String response_id = rs.getString("response_id");
//        String request_id = rs.getString("request_id");
//      logger.info("request_id = {}, response_id = {}", request_id, response_id);
//        String record_data	= rs.getString("record_data");
//        Long ttl = getLong(rs, "ttl");
//        String rtype = rs.getString("record_type");
//        Timestamp ts = rs.getTimestamp("crawl_timestamp");
//        ZonedDateTime crawl_timestamp = zonedDateTime(ts, ZoneId.systemDefault());
//
//        ResponseGeoIp geoIp = null;
//        Long asn = getLong(rs, "asn");
//        if (asn != null) {
//            String asn_org = rs.getString("asn_organisation");
//            String ip = rs.getString("ip");
//            String country = rs.getString("country");
//            int ip_version = rs.getInt("ip_version");
//            geoIp = new ResponseGeoIp(Pair.of(asn, asn_org), country, ip_version, ip);
//        }
//        boolean isNewRequest = request_id != null && !request_id.equals(prev_request_id);
//        if (isNewRequest) {
//            currentRequest = Request.builder()
//                    .id(request_id)
//                    .domainName(rs.getString("domain_name"))
//                    .rcode(rs.getInt("rcode"))
//                    .problem(rs.getString("problem"))
//                    .prefix(rs.getString("prefix"))
//                    .recordType(RecordType.valueOf(rtype))
//                    .crawlTimestamp(crawl_timestamp)
//                    .build();
//            requests.add(currentRequest);
//            responses = new ArrayList<>();
//            prev_request_id = request_id;
//        }
//        if (response_id == null) {
//            prev_response_id = null;
//        }
//        boolean isNewResponse = response_id != null && !response_id.equals(prev_response_id);
//        if (isNewResponse) {
//            currentResponse = Response.builder()
//                    .id(response_id)
//                    .ttl(ttl)
//                    .recordData(record_data)
//                    .build();
//            responses.add(currentResponse);
//            prev_response_id = response_id;
//            logger.info("NEW currentResponse = {}", currentResponse);
//        }
//        if (geoIp != null) {
//            currentResponse.getResponseGeoIps().add(geoIp);
//        }
//        if (isNewResponse) {
//            currentRequest.setResponses(responses);
//        }
//    }
}
