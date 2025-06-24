package be.dnsbelgium.mercator.dns.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Request {

    private String domainName;

    private String prefix;

    private RecordType recordType;

    private Integer rcode;

    @Builder.Default
    private Instant crawlTimestamp = Instant.now();

    private boolean ok;

    private String problem;

    @Builder.Default
    private List<Response> responses = new ArrayList<>();

    public int getNumOfResponses() {
        return this.responses.size();
    }

}