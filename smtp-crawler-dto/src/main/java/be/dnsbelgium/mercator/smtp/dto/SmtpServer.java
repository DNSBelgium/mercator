package be.dnsbelgium.mercator.smtp.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SmtpServer {

    private final String hostName;
    private final int priority;
    private final List<SmtpHostIp> hosts = new ArrayList<>();

    @JsonCreator
    public SmtpServer(@JsonProperty("hostName") String hostName, @JsonProperty("priority") int priority) {
        this.hostName = hostName;
        this.priority = priority;
    }

    public SmtpServer(String hostName) {
        this.hostName = hostName;
        this.priority = 0;
    }

    public void addHost(SmtpHostIp hostIp) {
        hosts.add(hostIp);
    }

}
