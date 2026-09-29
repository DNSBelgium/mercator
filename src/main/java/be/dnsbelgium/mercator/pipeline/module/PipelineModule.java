package be.dnsbelgium.mercator.pipeline.module;

import be.dnsbelgium.mercator.pipeline.service.ItemProcessor;
import be.dnsbelgium.mercator.pipeline.service.ItemSource;
import be.dnsbelgium.mercator.pipeline.service.ItemWriter;
import be.dnsbelgium.mercator.pipeline.service.PipelineService;

/**
 * A runnable crawling module (web, DNS, SMTP, …). Each module wires a
 * {@link ItemSource}, {@link ItemProcessor}
 * and {@link ItemWriter} into a
 * {@link PipelineService} and runs it to completion.
 *
 * <p>Modules are registered as Spring beans and selected by {@link #name()} via the
 * {@code pipeline.modules} property.
 */
public interface PipelineModule {

    /** Unique, lower-case module name used to select it (e.g. {@code "web"}). */
    String name();

    /**
     * Builds and runs this module's pipeline for one pass and returns the number of items
     * produced. For a stateless (one-shot) source this is the whole input; for a stateful
     * bounded-pass source it is the items leased this pass (used by the sequential runner to
     * detect an idle cycle).
     */
    long run();
}

