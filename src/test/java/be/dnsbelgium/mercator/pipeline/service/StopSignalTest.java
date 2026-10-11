package be.dnsbelgium.mercator.pipeline.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StopSignalTest {

    @Test
    void isNotRequestedUntilTriggered_andThenStaysRequested() {
        StopSignal signal = new StopSignal();
        assertThat(signal.isStopRequested()).isFalse();

        signal.requestStop();
        assertThat(signal.isStopRequested()).isTrue();

        signal.requestStop(); // idempotent
        assertThat(signal.isStopRequested()).isTrue();
    }

    @Test
    void never_isNotTriggeredAndCannotBeTriggered() {
        StopSignal never = StopSignal.never();

        assertThat(never.isStopRequested()).isFalse();
        assertThat(StopSignal.never()).as("shared instance").isSameAs(never);
        assertThatThrownBy(never::requestStop).isInstanceOf(UnsupportedOperationException.class);
        assertThat(never.isStopRequested()).isFalse();
    }
}

