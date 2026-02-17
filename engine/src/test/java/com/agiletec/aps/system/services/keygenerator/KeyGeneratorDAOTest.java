package com.agiletec.aps.system.services.keygenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KeyGeneratorDAOTest {

    @Mock
    private DataSource dataSource;
    @Mock
    private Connection connection;
    @Mock
    private Statement statement;
    @Mock
    private PreparedStatement preparedStatement;
    @Mock
    private ResultSet resultSet;

    private KeyGeneratorDAO dao;

    @BeforeEach
    void setUp() throws Exception {
        dao = new KeyGeneratorDAO();
        dao.setDataSource(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
    }

    @Test
    void getNextUniqueKey_shouldBeTransactional() throws Exception {
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(10);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        int result = dao.getNextUniqueKey();

        assertEquals(11, result);

        InOrder inOrder = inOrder(connection);
        inOrder.verify(connection).setAutoCommit(false);
        inOrder.verify(connection).commit();
    }

    @Test
    void getNextUniqueKey_shouldRollbackOnError() throws Exception {
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenThrow(new SQLException("DB error"));

        assertThrows(RuntimeException.class, () -> dao.getNextUniqueKey());

        verify(connection).setAutoCommit(false);
        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    @Test
    void getNextUniqueKey_shouldCloseConnection() throws Exception {
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(5);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        dao.getNextUniqueKey();

        verify(connection).close();
    }

    @Test
    void getNextUniqueKey_shouldCloseConnectionOnError() throws Exception {
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenThrow(new SQLException("DB error"));

        assertThrows(RuntimeException.class, () -> dao.getNextUniqueKey());

        verify(connection).close();
    }

    @Test
    void getNextUniqueKey_shouldIncrementKeyByOne() throws Exception {
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(42);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        int result = dao.getNextUniqueKey();

        assertEquals(43, result);
        verify(preparedStatement).setInt(1, 43);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void getNextUniqueKey_shouldReturnOneWhenTableEmpty() throws Exception {
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        int result = dao.getNextUniqueKey();

        assertEquals(1, result);
    }

    @Test
    void getUniqueKey_shouldReturnCurrentKey() throws Exception {
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(99);

        int result = dao.getUniqueKey();

        assertEquals(99, result);
        verify(connection).close();
    }

    @Test
    void updateKey_shouldUpdateAndCloseConnection() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        dao.updateKey(55);

        verify(preparedStatement).setInt(1, 55);
        verify(preparedStatement).executeUpdate();
        verify(connection).close();
    }
}