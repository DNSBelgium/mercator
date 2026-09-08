package be.dnsbelgium.mercator.pipeline.service;

import be.dnsbelgium.mercator.common.VisitRequest;

import be.dnsbelgium.mercator.pipeline.config.PipelineProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static be.dnsbelgium.mercator.pipeline.testsupport.TestSupport.duckDbClient;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("SqlNoDataSourceInspection")
class CsvItemSourceFactoryTest {

    @Test
    void createsCsvItemSourceThatReadsConfiguredFile(@TempDir Path dir) throws IOException {
        Path csv = dir.resolve("input.csv");
        Files.writeString(csv, """
                domain_name,visit_id
                example0.be,v0
                example1.be,v1
                """);

        PipelineProperties properties = new PipelineProperties();
        properties.setInputCsv(csv.toString());

        CsvItemSourceFactory factory = new CsvItemSourceFactory(duckDbClient(), properties);

        ItemSource<VisitRequest> source = factory.create("web");

        assertThat(source).isInstanceOf(CsvItemSource.class);
        assertThat(source.isDone()).isFalse();
        assertThat(source.sleepBetweenPolls()).isFalse();

        var items = source.getItems();
        assertThat(items).hasSize(2);
        assertThat(items).extracting(VisitRequest::getDomainName)
                .containsExactly("example0.be", "example1.be");
        assertThat(items).extracting(VisitRequest::getVisitId)
                .containsExactly("v0", "v1");
        assertThat(source.isDone()).isTrue();
    }

    @Test
    void ignoresModuleNameArgument(@TempDir Path dir) throws IOException {
        Path csv = dir.resolve("input.csv");
        Files.writeString(csv, "domain_name,visit_id\nexample.be,v0\n");

        PipelineProperties properties = new PipelineProperties();
        properties.setInputCsv(csv.toString());

        CsvItemSourceFactory factory = new CsvItemSourceFactory(duckDbClient(), properties);

        try (ItemSource<VisitRequest> forWeb = factory.create("web");
             ItemSource<VisitRequest> forDns = factory.create("dns")) {
            assertThat(forWeb.getItems()).hasSize(1);
            assertThat(forDns.getItems()).hasSize(1);
        }
    }
}
