package be.dnsbelgium.mercator.visits;

import be.dnsbelgium.mercator.metrics.Threads;
import be.dnsbelgium.mercator.persistence.VisitRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class VisitService {

//  private final ReadWriteLock readWriteLock;
//  private final AtomicInteger transactionCount = new AtomicInteger(0);
//  private final AtomicInteger transactionsBusy = new AtomicInteger(0);
//  private final VisitRepository visitRepository;
//  private final MeterRegistry meterRegistry;
//  private static final Logger logger = LoggerFactory.getLogger(VisitService.class);

//  @Value("${visits.max.transactions.per_db:5000}")
//  int maxTransactionsPerDatabase;
//
//  public VisitService(VisitRepository visitRepository, MeterRegistry meterRegistry) {
//    this.visitRepository = visitRepository;
//    this.meterRegistry = meterRegistry;
//    this.readWriteLock = new ReentrantReadWriteLock();
//    meterRegistry.gauge("VisitService.transactionsBusy", transactionsBusy);
//  }

//  @PostConstruct
//  public void init() {
//    visitRepository.init();
//  }

//  @PreDestroy
//  public void close() {
//    logger.info("close => exporting database");
//    exportDatabase(false);
//  }

//  public void save(VisitResult visitResult) {
//    Threads.SAVE.incrementAndGet();
//    Timer.Sample sample = Timer.start(meterRegistry);
//    try {
//      int count = transactionCount.getAndIncrement();
//      logger.debug("transactionCount: {}", count);
//      if (count == maxTransactionsPerDatabase) {
//        logger.info("current database had {} transactions => exporting and then starting new db", count);
//        exportDatabase(true);
//      }
//      doSave(visitResult);
//    } finally {
//      Threads.SAVE.decrementAndGet();
//      sample.stop(meterRegistry.timer("visit.service.save"));
//    }
//  }

//  private void doSave(VisitResult visitResult) {
//    readWriteLock.readLock().lock();
//    transactionsBusy.incrementAndGet();
//    try {
//      visitRepository.save(visitResult);
//    } finally {
//      readWriteLock.readLock().unlock();
//      transactionsBusy.decrementAndGet();
//    }
//  }

//  private void exportDatabase(boolean attachNewDatabase) {
//    var start = Instant.now();
//    readWriteLock.writeLock().lock();
//    try {
//      var end = Instant.now();
//      logger.info("We now have the write-lock after waiting {}", Duration.between(start, end));
//      visitRepository.exportDatabase(attachNewDatabase);
//      transactionCount.set(0);
//    } finally {
//      readWriteLock.writeLock().unlock();
//      logger.info("We now have released the write-lock after {}", Duration.between(start, Instant.now()));
//    }
//  }


}
