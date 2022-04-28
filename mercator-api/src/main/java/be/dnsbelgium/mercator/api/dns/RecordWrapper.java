package be.dnsbelgium.mercator.api.dns;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class RecordWrapper {

    private int rcode;
    private String recordType;
    private Map<String, Integer> recordDataAndTtl;

}
