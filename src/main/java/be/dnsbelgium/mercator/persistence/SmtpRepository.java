package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.smtp.dto.SmtpVisit;
import com.fasterxml.jackson.databind.ObjectMapper;


import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Repository;


@Repository
public class SmtpRepository extends BaseRepository<SmtpVisit> {

    @SneakyThrows
    public SmtpRepository(ObjectMapper objectMapper, @Value("${mercator.data.location:mercator/data/}/smtp") String baseLocation) {
        super(objectMapper, baseLocation, SmtpVisit.class);
    }

    @Override
    public String timestampField(){
        return "timestamp";
    }

}