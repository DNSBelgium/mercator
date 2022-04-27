package be.dnsbelgium.mercator.api.dns;

import be.dnsbelgium.mercator.dns.persistence.*;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DnsRepoService { // To Quentin: IGNORE THIS CLASS it is a WIP.
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

    // TODO: JAVADOC
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

        List<Integer> rcodes = new ArrayList<>();
        // Creating a maps for prefix, recordType and recordData.
//        Map<String, Map<String, String>> records = new HashMap<>();

        Map<String, List<RecordWrapper>> prefixAndData = new HashMap<>();
        List<RecordWrapper> wrappedRecords = new ArrayList<>();

        Map<Long, String> prefixes = new HashMap<>();
        Map<Long, String> recordTypes = new HashMap<>();
        for(Request req: requests) {
            prefixes.put(req.getId(), req.getPrefix());
            recordTypes.put(req.getId(), req.getRecordType());
        }

        for(Response resp: responses) {

            long reqId = resp.getRequest().getId();


        }








//        for(Request req: requests) {
//            if(!prefixes. .contains(req.getPrefix())) {
//                prefixes.add(req.getPrefix());
//            }
//            if (!recordTypes.contains(req.getRecordType())) {
//                recordTypes.add(req.getRecordType());
//            }
//        }






        for(Response resp: responses) {

            String recordData = resp.getRecordData();

            for(Request req: requests) {
                if(resp.getRequest().equals(req)) {
                    int rcode = req.getRcode();
                    String recordType = req.getRecordType();

//                    wrappedRecords.add(new RecordWrapper(rcode, recordType, recordData));
                }
            }
        }
        for(String prefix: distinctPrefixes(requests)) {

        }

        for(Request req: requests) {

            int rcode = req.getRcode();
            String recordType = req.getRecordType();

            String recordData = responses.stream()
                    .filter(resp -> resp.getRequest().equals(req))
                    .map(Response::getRecordData)
                    .findFirst()
                    .get();
        }


        for (String prefix: distinctPrefixes(requests)) {

            for(Request req: requests) {
                if (req.getPrefix().equals(prefix)) {

                }
            }
        }



        Map<String, String> prefixAndRecordType = new HashMap<>();
        Map<String, String> recordTypeAndData = new HashMap<>();
        for(Request req: requests) {
            rcodes.add(req.getRcode());

            prefixAndRecordType.put(req.getPrefix(), req.getRecordType());

            String type = req.getRecordType();

            for(Response resp: responses) {
                if (resp.getRequest().equals(req)) {
                    String data = resp.getRecordData();

                    recordTypeAndData.put(type, data);
                }
            }
        }

        dnsCrawlDTO.setRcode(rcodes);
        dnsCrawlDTO.setPrefixAndRecordType(prefixAndRecordType);
        dnsCrawlDTO.setRecordTypeAndData(recordTypeAndData);

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
