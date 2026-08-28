package com.agiletec.aps.system.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueryLimitResolverTest {

    @Mock
    private BasicDataSource dataSource;

    @Test
    void testDerbyDriver() throws Exception {
        testCreateLimitBlock("org.apache.derby.jdbc.EmbeddedDriver",
                " OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY ");
    }

    @Test
    void testPostgresDriver() throws Exception {
        testCreateLimitBlock("org.postgresql.Driver",
                " OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY ");
    }

    @Test
    void testDeprecatedMySQLDriver() throws Exception {
        testCreateLimitBlock("com.mysql.jdbc.Driver",
                " LIMIT 1 OFFSET 0 ");
    }

    @Test
    void testMySQLDriver() throws Exception {
        testCreateLimitBlock("com.mysql.cj.jdbc.Driver",
                " LIMIT 1 OFFSET 0 ");
    }

    @Test
    void testDeprecatedOracleDriver() throws Exception {
        testCreateLimitBlock("oracle.jdbc.driver.OracleDriver",
                " OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY ");
    }

    @Test
    void testOracleDriver() throws Exception {
        testCreateLimitBlock("oracle.jdbc.OracleDriver",
                " OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY ");
    }

    /**
     * A vendor the resolver does not know fails on the first paginated request, at runtime, with no
     * compile-time signal. Pinned as current behaviour, not as desired behaviour.
     */
    @Test
    void testUnknownDriver() {
        String driverClassName = "com.example.UnknownDriver";
        when(dataSource.getDriverClassName()).thenReturn(driverClassName);

        FieldSearchFilter filter = new FieldSearchFilter();
        filter.setLimit(1);
        filter.setOffset(0);

        assertThrows(UnsupportedOperationException.class,
                () -> QueryLimitResolver.createLimitBlock(filter, dataSource, driverClassName));
    }

    private void testCreateLimitBlock(String driverClassName, String expected) throws  Exception {
        when(dataSource.getDriverClassName()).thenReturn(driverClassName);

        FieldSearchFilter filter = new FieldSearchFilter();
        filter.setLimit(1);
        filter.setOffset(0);

        assertEquals(expected, QueryLimitResolver.createLimitBlock(filter, dataSource, driverClassName));
    }
}
