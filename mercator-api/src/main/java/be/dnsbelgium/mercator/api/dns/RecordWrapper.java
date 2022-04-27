package be.dnsbelgium.mercator.api.dns;

import lombok.Data;

import java.util.List;

@Data
public class RecordWrapper { // To Quentin: IGNORE THIS CLASS it is a WIP.

    private int rcode;
    private String recordType;
    private List<String> recordData;

    public RecordWrapper(int rcode, String recordType, List<String> recordData) {
        this.rcode = rcode;
        this.recordType = recordType;
        this.recordData = recordData;
    }
}
