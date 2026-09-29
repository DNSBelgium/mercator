package be.dnsbelgium.mercator.pipeline;

import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import be.dnsbelgium.mercator.pipeline.module.PipelineModule;
import be.dnsbelgium.mercator.pipeline.queue.QueueModuleRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PipelineApplicationTest {

    @Test
    void reportsCompletionAfterStatelessModulesFinish() {
        PipelineModule module = module("dns");
        PipelineProperties properties = propertiesFor("dns");
        ObjectProvider<QueueModuleRunner> queueRunnerProvider = provider(null);
        PipelineApplication application = new PipelineApplication(List.of(module), properties, queueRunnerProvider);

        assertThat(application.run()).isTrue();
        verify(module).run();
    }

    @Test
    void reportsNotCompletedAfterStartingContinuousQueueRunner() {
        PipelineModule module = module("dns");
        PipelineProperties properties = propertiesFor("dns");
        QueueModuleRunner queueRunner = mock(QueueModuleRunner.class);
        ObjectProvider<QueueModuleRunner> queueRunnerProvider = provider(queueRunner);
        PipelineApplication application = new PipelineApplication(List.of(module), properties, queueRunnerProvider);

        assertThat(application.run()).isFalse();
        verify(queueRunner).start(List.of(module));
    }

    private static PipelineModule module(String name) {
        PipelineModule module = mock(PipelineModule.class);
        when(module.name()).thenReturn(name);
        return module;
    }

    private static PipelineProperties propertiesFor(String module) {
        PipelineProperties properties = new PipelineProperties();
        properties.setModules(List.of(module));
        return properties;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<QueueModuleRunner> provider(QueueModuleRunner runner) {
        ObjectProvider<QueueModuleRunner> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(runner);
        return provider;
    }
}
