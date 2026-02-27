package com.agiletec.aps.system.services.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.aps.BaseTestCase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestExternalSynchronizationAuthorizationDAO extends BaseTestCase {

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
    void testExternalAuthSync_Check_NoUser() {
        String username = "testUser";
        Long iat = 1000L;
        // Se l'utente non esiste, deve restituire false
        assertFalse(authorizationDAO.externalAuthSyncCheck(username, iat));
    }

    @Test
    void testExternalAuthSync_Check_OldIat() throws Exception {
        String username = "testUser";
        Long currentIat = 1000L;
        Long newIat = 2000L;

        // Inserisco manualmente un record
        DataSource dataSource = (DataSource) this.getApplicationContext().getBean("servDataSource");
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stat = conn.prepareStatement("INSERT INTO authusersextsync (username, iat) VALUES (?, ?)")) {
                stat.setString(1, username);
                stat.setLong(2, currentIat);
                stat.executeUpdate();
            }
        }

        // Se l'IAT fornito è maggiore di quello salvato, deve restituire false
        assertFalse(authorizationDAO.externalAuthSyncCheck(username, newIat));
    }

    @Test
    void testExternalAuthSync_Check_SameIat() throws Exception {
        String username = "testUser";
        Long currentIat = 1000L;

        // Inserisco manualmente un record
        DataSource dataSource = (DataSource) this.getApplicationContext().getBean("servDataSource");
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stat = conn.prepareStatement("INSERT INTO authusersextsync (username, iat) VALUES (?, ?)")) {
                stat.setString(1, username);
                stat.setLong(2, currentIat);
                stat.executeUpdate();
            }
        }

        // Se l'IAT è uguale, deve restituire true (sincronizzato)
        assertTrue(authorizationDAO.externalAuthSyncCheck(username, currentIat));
    }
    
    @Test
    void testExternalAuthSync_Check_NewerIat() throws Exception {
        String username = "testUser";
        Long currentIat = 2000L;
        Long oldIat = 1000L;

        // Inserisco manualmente un record
        DataSource dataSource = (DataSource) this.getApplicationContext().getBean("servDataSource");
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stat = conn.prepareStatement("INSERT INTO authusersextsync (username, iat) VALUES (?, ?)")) {
                stat.setString(1, username);
                stat.setLong(2, currentIat);
                stat.executeUpdate();
            }
        }

        // Se l'IAT fornito è minore di quello salvato, deve restituire true (abbiamo già dati più recenti)
        assertTrue(authorizationDAO.externalAuthSyncCheck(username, oldIat));
    }

    @Test
    void testExternalAuthSync_Insert() throws Exception {
        String username = "testSync";
        Long iat = 3000L;

        authorizationDAO.externalAuthSync(username, iat, null, null);

        // Verifico che sia stato inserito
        DataSource dataSource = (DataSource) getApplicationContext().getBean("servDataSource");
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stat = conn.prepareStatement("SELECT iat FROM authusersextsync WHERE username = ?")) {
                stat.setString(1, username);
                try (var rs = stat.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(iat, rs.getLong("iat"));
                }
            }
        }
    }

    @Test
    void testExternalAuthSync_Update() throws Exception {
        String username = "testSync";
        Long initialIat = 3000L;
        Long updatedIat = 4000L;

        // Inserimento iniziale
        authorizationDAO.externalAuthSync(username, initialIat, null, null);

        // Aggiornamento
        authorizationDAO.externalAuthSync(username, updatedIat, null, null);

        // Verifico che sia stato aggiornato
        DataSource dataSource = (DataSource) getApplicationContext().getBean("servDataSource");
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stat = conn.prepareStatement("SELECT iat FROM authusersextsync WHERE username = ?")) {
                stat.setString(1, username);
                try (var rs = stat.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(updatedIat, rs.getLong("iat"));
                    assertFalse(rs.next());
                }
            }
        }
    }
}
