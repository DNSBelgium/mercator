package be.dnsbelgium.mercator.content.ports.async;

import be.dnsbelgium.mercator.content.dto.Resolution;
import be.dnsbelgium.mercator.content.ports.async.model.ResponseMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public interface ContentResolutionListener<T extends ResponseMessage> {

  void contentResolved(T response) throws JsonProcessingException;

  Resolution toContentResolution(T response, UUID visitId) throws JsonProcessingException;

}
