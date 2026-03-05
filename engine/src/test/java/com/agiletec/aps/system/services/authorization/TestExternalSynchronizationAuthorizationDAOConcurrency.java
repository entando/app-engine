package com.agiletec.aps.system.services.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.agiletec.aps.BaseTestCase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TestExternalSynchronizationAuthorizationDAOConcurrency extends BaseTestCase {

    private static final Logger logger = LoggerFactory.getLogger(TestExternalSynchronizationAuthorizationDAOConcurrency.class);

    private AuthorizationDAO authorizationDAO;

    @BeforeEach
    void setUpMethod() throws Exception {
        DataSource dataSource = (DataSource) getApplicationContext().getBean("servDataSource");
        authorizationDAO = new AuthorizationDAO();
        authorizationDAO.setDataSource(dataSource);
        this.cleanSyncTable();
    }

    private void cleanSyncTable() throws Exception {
        DataSource dataSource = (DataSource) this.getApplicationContext().getBean("servDataSource");
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stat = conn.prepareStatement("DELETE FROM authusersextsync")) {
                stat.executeUpdate();
            }
        }
    }

    @Test
    void testExternalAuthSyncConcurrency_SameUser() throws Exception {
        final String username = "concurrentUser";
        final int numThreads = 10;
        final long baseIat = 1000L;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            final long iat = baseIat + i;
            tasks.add(() -> {
                try {
                    // force a race condition
                    authorizationDAO.externalAuthSync(username, iat, null, null);
                } catch (Exception e) {
                    logger.error("Error during concurrent sync for iat {}", iat, e);
                    throw e;
                }
                return null;
            });
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        boolean finished = executor.awaitTermination(30, TimeUnit.SECONDS);
        assertTrue(finished, "Threads did not finish in time");

        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                fail("One of the sync operations failed: " + e.getMessage());
            }
        }

        DataSource dataSource = (DataSource) getApplicationContext().getBean("servDataSource");
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stat = conn.prepareStatement("SELECT iat FROM authusersextsync WHERE username = ?")) {
                stat.setString(1, username);
                try (ResultSet rs = stat.executeQuery()) {
                    assertTrue(rs.next(), "User should be present in sync table");
                    long finalIat = rs.getLong("iat");
                    logger.info("Final iat for user {} is {}", username, finalIat);
                    assertEquals(baseIat + numThreads - 1, finalIat, "Final IAT should be the maximum");
                }
            }
        }
    }

    @Test
    void testExternalAuthSyncConcurrency_RandomOrder() throws Exception {
        final String username = "concurrentUserRandom";
        final int numThreads = 20;
        final List<Long> iats = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            iats.add(1000L + i);
        }
        java.util.Collections.shuffle(iats);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Callable<Void>> tasks = new ArrayList<>();

        for (Long iat : iats) {
            tasks.add(() -> {
                authorizationDAO.externalAuthSync(username, iat, null, null);
                return null;
            });
        }

        executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        DataSource dataSource = (DataSource) getApplicationContext().getBean("servDataSource");
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stat = conn.prepareStatement("SELECT iat FROM authusersextsync WHERE username = ?")) {
                stat.setString(1, username);
                try (ResultSet rs = stat.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(1000L + numThreads - 1, rs.getLong("iat"), "Final IAT should be the maximum even with random arrival");
                }
            }
        }
    }

    @Test
    void testExternalAuthSyncConcurrency_SameUserSameIat() throws Exception {
        final String username = "concurrentUserSameIat";
        final int numThreads = 10;
        final long iat = 1000L;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            tasks.add(() -> {
                authorizationDAO.externalAuthSync(username, iat, null, null);
                return null;
            });
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        for (Future<Void> future : futures) {
            future.get(); // Should not throw exception
        }

        DataSource dataSource = (DataSource) getApplicationContext().getBean("servDataSource");
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stat = conn.prepareStatement("SELECT iat FROM authusersextsync WHERE username = ?")) {
                stat.setString(1, username);
                try (ResultSet rs = stat.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(iat, rs.getLong("iat"));
                }
            }
        }
    }
}
