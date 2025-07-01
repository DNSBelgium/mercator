package be.dnsbelgium.mercator;

import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ULIDTest {

    private static final Logger logger = LoggerFactory.getLogger(ULIDTest.class);

    @Test
    public void test() {
        List<Ulid> ulids = new ArrayList<>();
        long start = System.currentTimeMillis();
        for (int i=0; i<5_000_000; i++) {
            Ulid ulid = UlidCreator.getMonotonicUlid();
            ulids.add(ulid);
        }
        long end = System.currentTimeMillis();
        long millis = end - start;
        logger.info("millis = {}", millis);
        logger.info("ulids.size() = {}", ulids.size());
        assertThat(millis).isLessThan(2000);
        assertThat(ulids.size()).isEqualTo(5_000_000);
    }

}
