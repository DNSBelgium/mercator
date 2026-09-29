package be.dnsbelgium.mercator.pipeline.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PipelinePropertiesTest {

    @Test
    void exitsOnCompletionByDefault() {
        assertThat(new PipelineProperties().isExitOnCompletion()).isTrue();
    }

    @Test
    void usesModuleConcurrencyOverridesWithGlobalFallback() {
        PipelineProperties properties = new PipelineProperties();
        properties.setNumConsumers(50);
        properties.setMaxConcurrentRequests(40);

        properties.getModuleSettings().getDns().setNumConsumers(500);
        properties.getModuleSettings().getDns().setMaxConcurrentRequests(450);

        assertThat(properties.numConsumers("dns")).isEqualTo(500);
        assertThat(properties.maxConcurrentRequests("dns")).isEqualTo(450);
        assertThat(properties.numConsumers("web")).isEqualTo(50);
        assertThat(properties.maxConcurrentRequests("web")).isEqualTo(40);
    }

    @Test
    void bindsModuleSettingsFromExternalConfiguration() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "pipeline.module-settings.dns.num-consumers", "500",
                "pipeline.module-settings.dns.max-concurrent-requests", "500",
                "pipeline.module-settings.tls.num-consumers", "1000",
                "pipeline.module-settings.tls.max-concurrent-requests", "1000"
        ));

        PipelineProperties properties = new Binder(source)
                .bind("pipeline", PipelineProperties.class)
                .orElseThrow(() -> new AssertionError("pipeline properties were not bound"));

        assertThat(properties.numConsumers("dns")).isEqualTo(500);
        assertThat(properties.maxConcurrentRequests("dns")).isEqualTo(500);
        assertThat(properties.numConsumers("tls")).isEqualTo(1000);
        assertThat(properties.maxConcurrentRequests("tls")).isEqualTo(1000);
    }
}
