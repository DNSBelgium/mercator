package be.dnsbelgium.mercator.api.dns;

import be.dnsbelgium.mercator.dns.persistence.*;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DnsRepoService {
    private final Logger logger = LoggerFactory.getLogger(DnsRepoService.class);

    private final RequestRepository requestRepository;
    private final ResponseRepository responseRepository;
    private final ResponseGeoIpRepository responseGeoIpRepository;

    @Autowired
    public DnsRepoService(RequestRepository requestRepository, ResponseRepository responseRepository, ResponseGeoIpRepository responseGeoIpRepository) {
        this.requestRepository = requestRepository;
        this.responseRepository = responseRepository;
        this.responseGeoIpRepository = responseGeoIpRepository;
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
        Request singleRequest = requests.get(0);

        List<Response> responses = responseRepository.findAllByRequestVisitId(visitId);
        if (responses.isEmpty()) throw new NotFoundException(String.format("No responses by visitId %s found.", visitId));

        List<ResponseGeoIp> responseGeoIps = responseGeoIpRepository.findAllByResponseRequestVisitId(visitId);

        // Setting simple variables.
        DnsCrawlDTO dnsCrawlDTO = new DnsCrawlDTO();
        dnsCrawlDTO.setId(singleRequest.getId());
        dnsCrawlDTO.setOk(singleRequest.isOk());
        dnsCrawlDTO.setProblem(singleRequest.getProblem());
        dnsCrawlDTO.setCrawlTimestamp(singleRequest.getCrawlTimestamp());
        dnsCrawlDTO.setGeoIps(responseGeoIps);

        // Requests have multiple (duplicate) prefixes.
        // Requests have an rcode and recordType allocated to a prefix.
        // Responses have multiple recordData allocated to one recordType.

        // Creating a Map where the key is the prefix, and the value is a List of RecordWrapper.
        // RecordWrapper has an rcode, recordType and a List of recordData.
        Map<String, List<RecordWrapper>> prefixAndDataMap = new HashMap<>();
        for (String prefix: distinctPrefixes(requests)) {

            List<RecordWrapper> wrappedRecords = new ArrayList<>();
            for (Request req: requests) {

                if (!req.getPrefix().equals(prefix)) continue;

                    RecordWrapper wrapper = new RecordWrapper();

                    wrapper.setRcode(req.getRcode());
                    wrapper.setRecordType(req.getRecordType());

                    Map<String, Integer> recordDataAndTtlMap = new HashMap<>();
                    for (Response resp: responses) {

                        if (!resp.getRequest().equals(req)) continue;

                        recordDataAndTtlMap.put(resp.getRecordData(), resp.getTtl());
                    }
                    wrapper.setRecordDataAndTtl(recordDataAndTtlMap);

                    if (!wrapper.getRecordDataAndTtl().isEmpty()) wrappedRecords.add(wrapper);
            }

            if (!wrappedRecords.isEmpty()) prefixAndDataMap.put(prefix, wrappedRecords);
        }

        dnsCrawlDTO.setPrefixAndData(prefixAndDataMap);

        return dnsCrawlDTO;
    }

    /**
     * Checks a list of requests for distinct prefixes.
     * @param requests List of Requests to be checked.
     * @return List of unique prefixes.
     */
    private List<String> distinctPrefixes(List<Request> requests) {
        List<String> prefixes = new ArrayList<>();

        List<String> checkList = new ArrayList<>();
        for(Request req: requests) {
            String prefix = req.getPrefix();

            if(!checkList.contains(prefix)) {
                prefixes.add(prefix);

                checkList.add(prefix);
            }
        }

        return prefixes;
    }
}
