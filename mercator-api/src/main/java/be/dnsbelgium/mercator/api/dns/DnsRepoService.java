package be.dnsbelgium.mercator.api.dns;

import be.dnsbelgium.mercator.dns.dto.RecordType;
import be.dnsbelgium.mercator.dns.persistence.*;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DnsRepoService {
    private final Logger logger = LoggerFactory.getLogger(DnsRepoService.class);

    private final RequestRepository requestRepository;

    @Autowired
    public DnsRepoService(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    /**
     * Gets all info gathered by the DNS Crawler by VisitId.
     * Info comes from three separate repositories - Request, Response and ResponseGeoIp.
     * @param visitId to request info for from the DNS Crawler.
     * @return DTO with all necessary info packaged into one object.
     * @throws NotFoundException when the requested VisitId is not found in the Request or Response repositories.
     */
    public DnsCrawlDTO getInfoByVisitId(UUID visitId) throws NotFoundException {
        logger.info("Searching for visitId: {}", visitId);

        List<Request> requests = requestRepository.findRequestsByVisitId(visitId);
        if (requests.isEmpty()) throw new NotFoundException(String.format("No requests by visitId %s found.", visitId));

        // Grabbing the first request to use for DTO.
//        Request singleRequest = requests.get(0);

//        List<Response> responses = responseRepository.findAllByRequestVisitId(visitId);
//        if (responses.isEmpty()) throw new NotFoundException(String.format("No responses by visitId %s found.", visitId));

//        List<ResponseGeoIp> responseGeoIps = responseGeoIpRepository.findAllByResponseRequestVisitId(visitId);

        // Setting simple variables.
        DnsCrawlDTO dnsCrawlDTO = new DnsCrawlDTO();
        dnsCrawlDTO.setOk(requests.stream().anyMatch(Request::isOk));
        dnsCrawlDTO.setProblem(requests.stream().map(Request::getProblem).filter(Objects::nonNull).findFirst().orElse(null));
        dnsCrawlDTO.setCrawlTimestamp(requests.get(0).getCrawlTimestamp());
//        dnsCrawlDTO.setGeoIps(requests.stream().flatMap(Request::getResponses).flatMap(Response::getResponseGeoIps).collect(Collectors.toList()));
//
//        Map<String, List<RecordWrapper>> prefixAndData;
//
//        Map<String, Map<RecordType, List<Request>>> collect = requests.stream().collect(Collectors.groupingBy(Request::getPrefix, Collectors.groupingBy(Request::getRecordType)));
//
//
//        requests.stream().collect(Collectors.toMap(Request::getPrefix, ))
//
//        requests.stream().collect(Collectors.groupingBy(Request::getPrefix, Collectors.groupingBy(Request::getRecordType)))
//
//
//
//        for (String prefix : requests.stream().map(Request::getPrefix).collect(Collectors.toSet())) {
//            for (RecordType recordType : )
//
//            prefixAndData.put(prefix, recordsWrapper)
//        }
//        dnsCrawlDTO.setPrefixAndData();
//
//        // Requests have multiple (duplicate) prefixes.
//        // Requests have a rcode and a recordType allocated to a prefix.
//        // Responses have multiple recordData allocated to one recordType.
//
//        // Creating a Map where the key is the prefix, and the value is a List of RecordWrapper.
//        // RecordWrapper has an rcode, recordType and a List of recordData.
//        Map<String, List<RecordWrapper>> prefixAndDataMap = new HashMap<>();
//
//
//
//        for (String prefix: distinctPrefixes(requests)) {
//
//            List<RecordWrapper> wrappedRecords = new ArrayList<>();
//            for (Request req: requests) {
//
//                if (!req.getPrefix().equals(prefix)) continue;
//
//                    RecordWrapper wrapper = new RecordWrapper();
//
//                    wrapper.setRcode(req.getRcode());
//                    wrapper.setRecordType(req.getRecordType().toString());
//
//                    Map<String, Integer> recordDataAndTtlMap = new HashMap<>();
//                    for (Response resp: responses) {
//
//                        if (!resp.getRequest().equals(req)) continue;
//
//                        recordDataAndTtlMap.put(resp.getRecordData(), resp.getTtl());
//                    }
//                    wrapper.setRecordDataAndTtl(recordDataAndTtlMap);
//
//                    if (!wrapper.getRecordDataAndTtl().isEmpty()) wrappedRecords.add(wrapper);
//            }
//
//            if (!wrappedRecords.isEmpty()) prefixAndDataMap.put(prefix, wrappedRecords);
//        }
//
//        dnsCrawlDTO.setPrefixAndData(prefixAndDataMap);

        return dnsCrawlDTO;
    }

    /**
     * Checks a list of requests for distinct prefixes.
     * @param requests List of Requests to be checked.
     * @return List of unique prefixes.
     */
    private Set<String> distinctPrefixes(List<Request> requests) {
//        List<String> prefixes = new ArrayList<>();
//        for(Request req: requests) {
//            String prefix = req.getPrefix();
//
//            if(!prefixes.contains(prefix)) {
//                prefixes.add(prefix);
//            }
//        }
//
//        return prefixes;

        return requests.stream().map(Request::getPrefix).collect(Collectors.toSet());
    }
}
