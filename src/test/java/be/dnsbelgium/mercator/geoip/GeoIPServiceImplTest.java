package be.dnsbelgium.mercator.geoip;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named="MAXMIND_DOWNLOADS_ENABLED", matches = ".*")
class GeoIPServiceImplTest {

    @TempDir
    private File maxmindFolder;

    @Test
    @EnabledIfEnvironmentVariable(named="MAXMIND_LICENSE_KEY", matches = ".*")
    public void init() {
        MaxMindConfig config = MaxMindConfig.free(
                Duration.ofDays(1),
                System.getenv("MAXMIND_LICENSE_KEY"),
                maxmindFolder.getAbsolutePath()
                );
        GeoIPServiceImpl geoIPService = new GeoIPServiceImpl(config);
        var asn  = geoIPService.lookupASN("8.8.8.8");
        System.out.println("asn = " + asn);
        assertThat(asn).isPresent();
        assertThat(asn.get().getKey()).isEqualTo(15169L);
        assertThat(asn.get().getValue()).isEqualTo("GOOGLE");

    }

}