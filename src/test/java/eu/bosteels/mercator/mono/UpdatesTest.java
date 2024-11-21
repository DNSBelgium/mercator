package eu.bosteels.mercator.mono;

import be.dnsbelgium.mercator.persistence.DuckDataSource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class UpdatesTest {

    DuckDataSource dataSource = DuckDataSource.memory();

    private static final Logger logger = LoggerFactory.getLogger(UpdatesTest.class);

    @Test
    /*
        The transaction that commits first will succeed.
        The other transactions will NOT block but fail with a SQLException AT COMMIT TIME !!
        => TransactionContext Error: Failed to commit: PRIMARY KEY or UNIQUE constraint violated: duplicate key "1""
     */
    public void concurrentUpdates() throws ExecutionException, InterruptedException {
        System.out.println("starting ...");
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute(
                "create or replace table test_locks(id int primary key, thread varchar)");
        var threadPool = Executors.newFixedThreadPool(2);
        final CountDownLatch t1_insert_done_done_signal = new CountDownLatch(1);
        final CountDownLatch t2_commit_done_signal = new CountDownLatch(1);

        var f1 = threadPool.submit(() -> {
            try {
                var c = dataSource.getConnection();
                c.setAutoCommit(false);
                var s = c.prepareStatement("insert into test_locks(id, thread) values(1, 't1')");
                s.execute();
                System.out.println("t1 has inserted");
                t1_insert_done_done_signal.countDown();
                t2_commit_done_signal.await();
                System.out.println("t1 about to commit, this will fail");
                s.execute("commit");
                System.out.println("t1 commit done");

            } catch (SQLException | InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

        });

        var f2 = threadPool.submit(() -> {
            try {
                var c = dataSource.getConnection();
                c.setAutoCommit(false);
                t1_insert_done_done_signal.await();
                var s = c.prepareStatement("insert into test_locks(id, thread) values(1, 't2')");
                s.execute();
                System.out.println("t2 has inserted");
                System.out.println("t2 about to commit");
                s.execute("commit");
                System.out.println("t2 commit done");
                t2_commit_done_signal.countDown();

            } catch (SQLException | InterruptedException e) {
                logger.atError().setCause(e).log();
                throw new RuntimeException(e);
            }
        });
        f2.get();
        assertThrows(ExecutionException.class, f1::get);
    }

}
