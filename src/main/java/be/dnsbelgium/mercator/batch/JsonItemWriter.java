package be.dnsbelgium.mercator.batch;

import be.dnsbelgium.mercator.persistence.BaseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class JsonItemWriter<T> implements ItemWriter<T> , ItemStream {

  private static final Logger logger = LoggerFactory.getLogger(JsonItemWriter.class);

  private final BaseRepository<T> repository;
  private final ObjectMapper objectMapper;
  private final Lock lock = new ReentrantLock();
  private boolean closed = false;
  private final AtomicInteger writeCount = new AtomicInteger(0);
  private final Path outputDirectory;
  private final String name;


  @SneakyThrows
  public JsonItemWriter(BaseRepository<T> repository, ObjectMapper objectMapper, Path outputDirectory, Class<T> clazz) {
    this.name = clazz.getSimpleName();
    this.repository = repository;
    this.objectMapper = objectMapper;
    this.outputDirectory = outputDirectory;
    logger.info("outputDirectory = {}", outputDirectory);
    Files.createDirectories(outputDirectory);
  }

  @Override
  public void write(Chunk<? extends T> chunk) throws Exception {
    setMDC();
    try {
      Path output = outputDirectory.resolve(UUID.randomUUID() + ".json");
      objectMapper.writeValue(output.toFile(), chunk.iterator());
      logger.debug("Written {} items to {}", chunk.size(), output);
      writeCount.addAndGet(chunk.size());
    } finally {
      resetMDC();
    }
  }

  @SneakyThrows
  @Override
  public void close() throws ItemStreamException {
    setMDC();
    lock.lock();
    try {
      if (closed) {
        // I noticed that Spring Batch sometimes calls the close method more than once (from different threads).
        // This happened when the job was hanging because the throttle limit was not lower
        // than the max core count of the ThreadPoolTaskExecutor and I interrupted the application.
        logger.info("name={}. Already closed => not closing again. thread=[{}]", name, Thread.currentThread().getName());
        return;
      }
      if (writeCount.get() > 0) {
        logger.debug("repository.storeResults for {} items", writeCount.get());
        String jsonResultsLocation = outputDirectory + "/*.json";
        logger.info("jsonResultsLocation = {}", jsonResultsLocation);
        repository.storeResults(jsonResultsLocation);
      } else {
        logger.info("Writer {}: No items to write. (writeCount == 0)", name);
      }
      closed = true;
      logger.info("Writer {} closed by thread [{}]", name, Thread.currentThread().getName());
    } finally {
      lock.unlock();
      resetMDC();
    }
  }

  private void setMDC() {
    MDC.put("name", name);
  }

  private void resetMDC() {
    MDC.remove("name");
  }

}
