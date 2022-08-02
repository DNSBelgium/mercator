package be.dnsbelgium.mercator.common.messaging.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

public class DefaultTypeJackson2MessageConverter<T> implements MessageConverter {
  private static final Logger LOG = LoggerFactory.getLogger(DefaultTypeJackson2MessageConverter.class);
  private final Class<T> defaultType;
  private final ObjectMapper objectMapper;

  public DefaultTypeJackson2MessageConverter(Class<T> defaultType) {
    this.defaultType = defaultType;
    this.objectMapper = JsonMapper.builder().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build();

    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL); // Ignore null values
  }

  @Override
  public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
    String payload = "";
    try {
      payload = objectMapper.writeValueAsString(object);
    } catch (Exception e) {
      LOG.warn("Cannot convert object to message for object : {}", object);
    }
    return session.createTextMessage(payload);
  }

  @Override
  public Object fromMessage(Message message) throws JMSException, MessageConversionException {
    try {
      TextMessage textMessage = (TextMessage) message;
      String payload = textMessage.getText();
      LOG.debug("inbound json='{}'", payload);

      return objectMapper.readValue(payload, defaultType);
    } catch (Exception e) {
      LOG.warn("Cannot convert message to object", e);
      if (message instanceof TextMessage) {
        LOG.warn("Content of message that could not be converted : [{}]", ((TextMessage) message).getText());
      }
      return null;
    }
  }
}
