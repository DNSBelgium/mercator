package be.dnsbelgium.mercator.dns.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Response {

    private String id;
    private String recordData;
    private Long ttl;

    @Builder.Default
    private List<ResponseGeoIp> responseGeoIps = new ArrayList<>();

}
