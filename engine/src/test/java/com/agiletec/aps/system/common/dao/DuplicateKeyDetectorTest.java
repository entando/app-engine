/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package com.agiletec.aps.system.common.dao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class DuplicateKeyDetectorTest {

    @Test
    void shouldDetectPostgreSqlDuplicateKey() {
        SQLException ex = new SQLException("duplicate key value violates unique constraint", "23505");
        assertTrue(DuplicateKeyDetector.isDuplicateKey(ex));
    }

    @Test
    void shouldDetectDerbyDuplicateKey() {
        SQLException ex = new SQLException("duplicate key value in unique index", "23505");
        assertTrue(DuplicateKeyDetector.isDuplicateKey(ex));
    }

    @Test
    void shouldDetectMySqlDuplicateKey() {
        SQLException ex = new SQLException("Duplicate entry", "23000", 1062);
        assertTrue(DuplicateKeyDetector.isDuplicateKey(ex));
    }

    @Test
    void shouldDetectMySqlDuplicateKeyWithKeyName() {
        SQLException ex = new SQLException("Duplicate entry with key name", "23000", 1586);
        assertTrue(DuplicateKeyDetector.isDuplicateKey(ex));
    }

    @Test
    void shouldDetectOracleDuplicateKey() {
        SQLException ex = new SQLException("ORA-00001: unique constraint violated", "23000", 1);
        assertTrue(DuplicateKeyDetector.isDuplicateKey(ex));
    }

    @Test
    void shouldDetectSqlServerDuplicateKey2627() {
        SQLException ex = new SQLException("Violation of PRIMARY KEY constraint", "23000", 2627);
        assertTrue(DuplicateKeyDetector.isDuplicateKey(ex));
    }

    @Test
    void shouldDetectSqlServerDuplicateKey2601() {
        SQLException ex = new SQLException("Cannot insert duplicate key row", "23000", 2601);
        assertTrue(DuplicateKeyDetector.isDuplicateKey(ex));
    }

    @Test
    void shouldNotTreatPostgreSqlForeignKeyViolationAsDuplicateKey() {
        SQLException ex = new SQLException("foreign key violation", "23503");
        assertFalse(DuplicateKeyDetector.isDuplicateKey(ex));
    }

    @Test
    void shouldNotTreatNotNullViolationAsDuplicateKey() {
        SQLException ex = new SQLException("null value in column violates not-null constraint", "23502");
        assertFalse(DuplicateKeyDetector.isDuplicateKey(ex));
    }

    @Test
    void shouldInspectNestedCauses() {
        SQLException sql = new SQLException("duplicate key", "23505");
        RuntimeException wrapped = new RuntimeException("DAO failure", sql);
        assertTrue(DuplicateKeyDetector.isDuplicateKey(wrapped));
    }

    @Test
    void shouldReturnFalseForNullOrNonSqlException() {
        assertFalse(DuplicateKeyDetector.isDuplicateKey((Throwable) null));
        assertFalse(DuplicateKeyDetector.isDuplicateKey((SQLException) null));
        assertFalse(DuplicateKeyDetector.isDuplicateKey(new RuntimeException("general error")));
    }
}
