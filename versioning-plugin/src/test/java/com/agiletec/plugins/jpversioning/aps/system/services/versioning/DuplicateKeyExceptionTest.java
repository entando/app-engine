/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.agiletec.plugins.jpversioning.aps.system.services.versioning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for duplicate key exception detection across different JDBC databases.
 */
@ExtendWith(MockitoExtension.class)
class DuplicateKeyExceptionTest {

    private VersioningManager versioningManager;
    private Method isDuplicateKeyExceptionMethod;

    @BeforeEach
    void setUp() throws Exception {
        versioningManager = new VersioningManager();
        // Access the private method via reflection
        isDuplicateKeyExceptionMethod = VersioningManager.class.getDeclaredMethod("isDuplicateKeyException", Throwable.class);
        isDuplicateKeyExceptionMethod.setAccessible(true);
    }

    private boolean invokeIsDuplicateKeyException(Throwable e) throws Exception {
        return (Boolean) isDuplicateKeyExceptionMethod.invoke(versioningManager, e);
    }

    // ==================== PostgreSQL Tests ====================

    @Test
    void testPostgreSQLDuplicateKey() throws Exception {
        // PostgreSQL uses SQLState 23505 for unique_violation
        SQLException sqlEx = new SQLException("duplicate key value violates unique constraint", "23505");
        assertTrue(invokeIsDuplicateKeyException(sqlEx), "PostgreSQL duplicate key should be detected");
    }

    @Test
    void testPostgreSQLDuplicateKeyWrappedInRuntimeException() throws Exception {
        SQLException sqlEx = new SQLException("duplicate key value violates unique constraint", "23505");
        RuntimeException wrapped = new RuntimeException("Error adding version record", sqlEx);
        assertTrue(invokeIsDuplicateKeyException(wrapped), "Wrapped PostgreSQL duplicate key should be detected");
    }

    // ==================== Derby Tests ====================

    @Test
    void testDerbyDuplicateKey() throws Exception {
        // Derby also uses SQLState 23505 for duplicate key
        SQLException sqlEx = new SQLException("The statement was aborted because it would have caused a duplicate key value", "23505");
        assertTrue(invokeIsDuplicateKeyException(sqlEx), "Derby duplicate key should be detected");
    }

    @Test
    void testDerbyDuplicateKeyWrappedInRuntimeException() throws Exception {
        SQLException sqlEx = new SQLException("The statement was aborted because it would have caused a duplicate key value", "23505");
        RuntimeException wrapped = new RuntimeException("Error adding version record", sqlEx);
        assertTrue(invokeIsDuplicateKeyException(wrapped), "Wrapped Derby duplicate key should be detected");
    }

    // ==================== MySQL Tests ====================

    @Test
    void testMySQLDuplicateKey_ErrorCode1062() throws Exception {
        // MySQL uses SQLState 23000 with error code 1062 (ER_DUP_ENTRY)
        SQLException sqlEx = new SQLException("Duplicate entry 'value' for key 'PRIMARY'", "23000", 1062);
        assertTrue(invokeIsDuplicateKeyException(sqlEx), "MySQL duplicate key (1062) should be detected");
    }

    @Test
    void testMySQLDuplicateKey_ErrorCode1586() throws Exception {
        // MySQL uses SQLState 23000 with error code 1586 (ER_DUP_ENTRY_WITH_KEY_NAME)
        SQLException sqlEx = new SQLException("Duplicate entry 'value' for key 'key_name'", "23000", 1586);
        assertTrue(invokeIsDuplicateKeyException(sqlEx), "MySQL duplicate key (1586) should be detected");
    }

    @Test
    void testMySQLDuplicateKeyWrappedInRuntimeException() throws Exception {
        SQLException sqlEx = new SQLException("Duplicate entry 'value' for key 'PRIMARY'", "23000", 1062);
        RuntimeException wrapped = new RuntimeException("Error adding version record", sqlEx);
        assertTrue(invokeIsDuplicateKeyException(wrapped), "Wrapped MySQL duplicate key should be detected");
    }

    // ==================== Oracle Tests ====================

    @Test
    void testOracleDuplicateKey() throws Exception {
        // Oracle uses SQLState 23000 with error code 1 (ORA-00001: unique constraint violated)
        SQLException sqlEx = new SQLException("ORA-00001: unique constraint (SCHEMA.CONSTRAINT_NAME) violated", "23000", 1);
        assertTrue(invokeIsDuplicateKeyException(sqlEx), "Oracle duplicate key should be detected");
    }

    @Test
    void testOracleDuplicateKeyWrappedInRuntimeException() throws Exception {
        SQLException sqlEx = new SQLException("ORA-00001: unique constraint (SCHEMA.CONSTRAINT_NAME) violated", "23000", 1);
        RuntimeException wrapped = new RuntimeException("Error adding version record", sqlEx);
        assertTrue(invokeIsDuplicateKeyException(wrapped), "Wrapped Oracle duplicate key should be detected");
    }

    // ==================== Negative Tests ====================

    @Test
    void testNonDuplicateKeyException() throws Exception {
        // A general SQL exception that is not a duplicate key
        SQLException sqlEx = new SQLException("Connection refused", "08001");
        assertFalse(invokeIsDuplicateKeyException(sqlEx), "Non-duplicate key exception should not be detected");
    }

    @Test
    void testForeignKeyViolation_MySQL() throws Exception {
        // MySQL foreign key violation: SQLState 23000 with error code 1452
        SQLException sqlEx = new SQLException("Cannot add or update a child row: a foreign key constraint fails", "23000", 1452);
        assertFalse(invokeIsDuplicateKeyException(sqlEx), "MySQL foreign key violation should not be detected as duplicate key");
    }

    @Test
    void testForeignKeyViolation_PostgreSQL() throws Exception {
        // PostgreSQL foreign key violation: SQLState 23503
        SQLException sqlEx = new SQLException("insert or update on table violates foreign key constraint", "23503");
        assertFalse(invokeIsDuplicateKeyException(sqlEx), "PostgreSQL foreign key violation should not be detected as duplicate key");
    }

    @Test
    void testNullException() throws Exception {
        assertFalse(invokeIsDuplicateKeyException(null), "Null exception should return false");
    }

    @Test
    void testNullSQLState() throws Exception {
        SQLException sqlEx = new SQLException("Some error", (String) null);
        assertFalse(invokeIsDuplicateKeyException(sqlEx), "Null SQL state should return false");
    }

    @Test
    void testNonSQLException() throws Exception {
        RuntimeException ex = new RuntimeException("Some error");
        assertFalse(invokeIsDuplicateKeyException(ex), "Non-SQLException without cause should return false");
    }

    @Test
    void testDeeplyNestedSQLException() throws Exception {
        SQLException sqlEx = new SQLException("duplicate key", "23505");
        RuntimeException level1 = new RuntimeException("Level 1", sqlEx);
        RuntimeException level2 = new RuntimeException("Level 2", level1);
        RuntimeException level3 = new RuntimeException("Level 3", level2);
        assertTrue(invokeIsDuplicateKeyException(level3), "Deeply nested SQLException should be detected");
    }

    @Test
    void testIntegrityConstraint23000WithoutMatchingErrorCode() throws Exception {
        // SQLState 23000 but with an error code that doesn't match duplicate key
        SQLException sqlEx = new SQLException("Some other constraint violation", "23000", 9999);
        assertFalse(invokeIsDuplicateKeyException(sqlEx), "SQLState 23000 with non-matching error code should return false");
    }
}