package be.dnsbelgium.mercator.smtp;

import org.slf4j.Logger;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.slf4j.LoggerFactory.getLogger;

public class TxLogger {

  private static final Logger logger = getLogger(TxLogger.class);

  public static void log(Class<?> clazz, String method) {
    boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
    logger.info("{}.{} tx active: {}", clazz, method, txActive);
  }

}
