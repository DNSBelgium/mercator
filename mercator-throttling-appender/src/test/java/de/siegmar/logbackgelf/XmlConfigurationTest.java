/*
 * Logback GELF - zero dependencies Logback GELF appender library.
 * Copyright (C) 2021 Oliver Siegmar
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package de.siegmar.logbackgelf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import ch.qos.logback.core.status.WarnStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.Status;

public class XmlConfigurationTest {

    private LoggerContext context;

    @BeforeEach
    public void init() {
        context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getStatusManager().clear();
        context.reset();
    }

    @Test
    public void basicConfiguration() throws IOException, JoranException {
        configure("/basic-config.xml");
        assertEquals(Collections.emptyList(), filterWarningsErrors());
    }

    @Test
    public void defaultConfiguration() throws IOException, JoranException {
        configure("/default-config.xml");
        assertEquals(Collections.emptyList(), filterWarningsErrors());
    }

    @Test
    public void fullConfiguration() throws IOException, JoranException {
        configure("/full-config.xml");
        assertEquals(Collections.emptyList(), filterWarningsErrors());
    }

    // @wards tmp workaround
    @Test
    @Disabled
    public void tooManyAppenders() throws IOException, JoranException {
        configure("/too-many-appenders.xml");
        var expected = new WarnStatus(
            "One and only one appender may be attached to be.dnsbelgium.logback.ThrottlingAppender",
            "be.dnsbelgium.logback.ThrottlingAppender[Throttle]");
        assertThat(filterWarningsErrors()).contains(expected);
    }

    private void configure(final String name) throws IOException, JoranException {
        final JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);

        try (InputStream config = XmlConfigurationTest.class.getResourceAsStream(name)) {
            configurator.doConfigure(config);
        }
        printStatusMessages();
    }

    private void printStatusMessages() {
        context.getStatusManager().getCopyOfStatusList().forEach(System.out::println);
    }

    private List<Status> filterWarningsErrors() {
        return context.getStatusManager().getCopyOfStatusList().stream()
            .filter(s -> s.getLevel() > Status.INFO)
            .collect(Collectors.toList());
    }

}
