package be.dnsbelgium.mercator.pipeline.simulation;

import be.dnsbelgium.mercator.pipeline.service.ItemProcessor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

/**
 * A placeholder {@link ItemProcessor} that reproduces the pipeline's original
 * {@code simulateWork()} behaviour: a short random sleep plus a little CPU work. Useful for
 * benchmarking the engine and as the default until a real module processor (web/DNS/SMTP)
 * is wired in.
 */
@Slf4j
public class SimulatedProcessor implements ItemProcessor<String, String> {

    @Override
    public String processItem(@NonNull String item) {
        simulateWork();
        return Thread.currentThread().getName() + "-processed-" + item;
    }

    private void simulateWork() {
        int randomSleep = (int) (Math.random() * 50);
        try {
            Thread.sleep(randomSleep);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        double total = 0;
        for (int i = 0; i < randomSleep; i++) {
            total = total + Math.sqrt(i); // Simulate some CPU work
        }
        log.debug("total = {}", total);
    }
}

