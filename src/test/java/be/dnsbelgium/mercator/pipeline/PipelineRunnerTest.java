package be.dnsbelgium.mercator.pipeline;

import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PipelineRunnerTest {

    @Test
    void keepsContextOpenByDefaultAfterStatelessCompletion() {
        PipelineApplication application = mock(PipelineApplication.class);
        when(application.run()).thenReturn(true);
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);

        new PipelineRunner(application, new PipelineProperties(), context).onApplicationReady();

        verify(context, never()).close();
    }

    @Test
    void keepsContextOpenForContinuousQueueRunner() {
        PipelineApplication application = mock(PipelineApplication.class);
        when(application.run()).thenReturn(false);
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);

        new PipelineRunner(application, new PipelineProperties(), context).onApplicationReady();

        verify(context, never()).close();
    }

    @Test
    void closesContextAfterStatelessCompletionWhenConfigured() {
        PipelineApplication application = mock(PipelineApplication.class);
        when(application.run()).thenReturn(true);
        PipelineProperties properties = new PipelineProperties();
        properties.setExitOnCompletion(true);
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);

        new PipelineRunner(application, properties, context).onApplicationReady();

        verify(context).close();
    }
}
