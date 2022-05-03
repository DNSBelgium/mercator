package be.dnsbelgium.mercator.api.dns;

import be.dnsbelgium.mercator.dns.dto.RRecord;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.util.Pair;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class RecordWrapper {

    private int rcode;
    private String recordType;
    private List<RRecord> recordDataAndTtl;

}
