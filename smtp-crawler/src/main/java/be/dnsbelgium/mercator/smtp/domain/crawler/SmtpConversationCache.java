package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class SmtpConversationCache {

  private final ConcurrentHashMap<String, SmtpConversation> cache = new ConcurrentHashMap<>();

  public SmtpConversationCache(){}

  public void add(String ip, SmtpConversation conversation){
    cache.put(ip, conversation);
  }

  public SmtpConversation get(String ip){
    return cache.get(ip);
  }

  public int size(){
    return cache.size();
  }
}
