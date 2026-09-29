package be.dnsbelgium.mercator.pipeline.simulation;

import be.dnsbelgium.mercator.pipeline.service.ItemSource;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link ItemSource} that synthesizes a fixed number of {@code String} work items
 * ({@code "P{producerId}-Item-{index}"}). Reproduces the pipeline's original in-code
 * producer so the generic engine can be exercised without an external data source.
 */
public class GeneratingItemSource implements ItemSource<String> {

    private final int producerId;
    private final int count;
    private boolean done = false;

    public GeneratingItemSource(int producerId, int count) {
        this.producerId = producerId;
        this.count = count;
    }

    @Override
    public List<String> getItems() {
        List<String> items = new ArrayList<>(count);
        for (int j = 0; j < count; j++) {
            items.add("P" + producerId + "-Item-" + j);
        }
        this.done = true;
        return items;
    }

    @Override
    public boolean isDone() {
        return this.done;
    }
}

