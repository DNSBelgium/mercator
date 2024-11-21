package be.dnsbelgium.mercator;

import be.dnsbelgium.mercator.persistence.DuckDataSource;
import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;
import org.duckdb.DuckDBConnection;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.sql.DriverManager;
import java.sql.SQLException;
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

    @Test
    public void insertUlid() {
        Ulid ulid = Ulid.from("01JD6W2CAB5QAH7WVXE4YPTPSW");
        long id = -50000L;
        String unsignedString = Long.toUnsignedString(id);
        logger.info("unsignedString = {}", unsignedString);
        var ds = DuckDataSource.memory();
        logger.info("ds = {}", ds);
        //var ds = new DuckDataSource("jdbc:duckdb:tst_ulid.db");
        JdbcClient client = JdbcClient.create(ds);
        logger.info("client = {}", client);
        client.sql("install ulid from community").update();
        logger.info("ulid extension installed");
        client.sql("load ulid").update();
        logger.info("ulid extension loaded");
        client.sql("create table a (id uhugeint, ulid ulid)").update();
        logger.info("table created");
        client.sql("insert into a (id, ulid) values(?,?)")
                .param(Long.toUnsignedString(id))
                .param(ulid.toString())
                .update();
        logger.info("insert done");
        var data = client.sql("select * from a").query().listOfRows();
        data.forEach(s -> logger.info("row: = {}", s));
        assertThat( data.getFirst().get("id").toString()).isEqualTo("18446744073709501616");
        assertThat( data.getFirst().get("ulid")).isEqualTo("01JD6W2CAB5QAH7WVXE4YPTPSW");
    }

    @Test
    public void getConnection() throws SQLException {
        String url = "jdbc:duckdb:";
        logger.info("url = {}", url);
        // the next statement takes over 2 seconds :-(
        DuckDBConnection connection = (DuckDBConnection) DriverManager.getConnection(url);
        logger.info("connection = {}", connection);
    }
}
