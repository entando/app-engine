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

import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jacms.aps.system.services.content.model.ContentRecordVO;
import org.entando.entando.ent.exception.EntException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VersioningManagerUnitTest {

    @Mock
    private IVersioningDAO versioningDAO;

    @Mock
    private IContentManager contentManager;

    @Mock
    private ConfigInterface configManager;

    @Spy
    @InjectMocks
    private VersioningManager versioningManager;

    private ContentRecordVO mockContentRecord;

    @BeforeEach
    void setUp() {
        mockContentRecord = new ContentRecordVO();
        mockContentRecord.setId("ART123");
        mockContentRecord.setTypeCode("ART");
        mockContentRecord.setDescription("Test Content");
        mockContentRecord.setStatus(Content.STATUS_DRAFT);
        mockContentRecord.setXmlWork("<content>test</content>");
        mockContentRecord.setModify(new Date());
        mockContentRecord.setVersion("1.0");
        mockContentRecord.setLastEditor("admin");
    }

    @Test
    void testSaveContentVersion_ThrowsExceptionForNonDuplicateKeyError() throws Exception {
        // Setup
        when(contentManager.loadContentVO("ART123")).thenReturn(mockContentRecord);
        when(versioningDAO.getVersion(anyString(), anyString())).thenReturn(null);

        // Simulate a generic RuntimeException (not duplicate key)
        RuntimeException genericException = new RuntimeException("Database connection failed");
        doThrow(genericException).when(versioningDAO).addContentVersion(any(ContentVersion.class));

        // Execute and verify - should throw EntException wrapping the original exception
        EntException thrown = assertThrows(EntException.class,
            () -> versioningManager.saveContentVersion("ART123"));

        assertEquals("Error saving version for contentART123", thrown.getMessage());
    }

    @Test
    void testSaveContentVersion_HandlesUniqueConstraintViolationGracefully() throws Exception {
        // Setup
        when(contentManager.loadContentVO("ART123")).thenReturn(mockContentRecord);
        when(versioningDAO.getVersion(anyString(), anyString())).thenReturn(null);

        // Simulate a duplicate key exception (PostgreSQL/Derby SQLState 23505)
        SQLException sqlException = new SQLException("unique constraint violation", "23505");
        RuntimeException duplicateKeyException = new RuntimeException("Error adding version record", sqlException);
        doThrow(duplicateKeyException).when(versioningDAO).addContentVersion(any(ContentVersion.class));

        // Execute - should NOT throw exception for duplicate key
        assertDoesNotThrow(() -> versioningManager.saveContentVersion("ART123"));

        // Verify addContentVersion was called
        verify(versioningDAO).addContentVersion(any(ContentVersion.class));
    }

    @Test
    void testSaveContentVersion_HandlesMySQLDuplicateKeyGracefully() throws Exception {
        // Setup
        when(contentManager.loadContentVO("ART123")).thenReturn(mockContentRecord);
        when(versioningDAO.getVersion(anyString(), anyString())).thenReturn(null);

        // Simulate a MySQL duplicate key exception (SQLState 23000, error code 1062)
        SQLException sqlException = new SQLException("Duplicate entry for key 'PRIMARY'", "23000", 1062);
        RuntimeException duplicateKeyException = new RuntimeException("Error adding version record", sqlException);
        doThrow(duplicateKeyException).when(versioningDAO).addContentVersion(any(ContentVersion.class));

        // Execute - should NOT throw exception
        assertDoesNotThrow(() -> versioningManager.saveContentVersion("ART123"));
    }

    @Test
    void testSaveContentVersion_HandlesOracleDuplicateKeyGracefully() throws Exception {
        // Setup
        when(contentManager.loadContentVO("ART123")).thenReturn(mockContentRecord);
        when(versioningDAO.getVersion(anyString(), anyString())).thenReturn(null);

        // Simulate an Oracle duplicate key exception (SQLState 23000, error code 1)
        SQLException sqlException = new SQLException("ORA-00001: unique constraint violated", "23000", 1);
        RuntimeException primaryKeyException = new RuntimeException("Error adding version record", sqlException);
        doThrow(primaryKeyException).when(versioningDAO).addContentVersion(any(ContentVersion.class));

        // Execute - should NOT throw exception
        assertDoesNotThrow(() -> versioningManager.saveContentVersion("ART123"));
    }

    @Test
    void testSaveContentVersion_ThrowsForNullPointerException() throws Exception {
        // Setup
        when(contentManager.loadContentVO("ART123")).thenReturn(mockContentRecord);
        when(versioningDAO.getVersion(anyString(), anyString())).thenReturn(null);

        // Simulate a NullPointerException (should NOT be treated as duplicate key)
        NullPointerException npe = new NullPointerException("Some null value");
        doThrow(npe).when(versioningDAO).addContentVersion(any(ContentVersion.class));

        // Execute and verify - should throw EntException
        assertThrows(EntException.class, () -> versioningManager.saveContentVersion("ART123"));
    }

    @Test
    void testSaveContentVersion_ThrowsForIllegalArgumentException() throws Exception {
        // Setup
        when(contentManager.loadContentVO("ART123")).thenReturn(mockContentRecord);
        when(versioningDAO.getVersion(anyString(), anyString())).thenReturn(null);

        // Simulate an IllegalArgumentException (should NOT be treated as duplicate key)
        IllegalArgumentException iae = new IllegalArgumentException("Invalid argument");
        doThrow(iae).when(versioningDAO).addContentVersion(any(ContentVersion.class));

        // Execute and verify - should throw EntException
        assertThrows(EntException.class, () -> versioningManager.saveContentVersion("ART123"));
    }

    @Test
    void testSaveContentVersion_SkipsInsertWhenVersionAlreadyExists() throws Exception {
        // Setup
        when(contentManager.loadContentVO("ART123")).thenReturn(mockContentRecord);

        // Return existing version (not null) - simulates version already exists
        ContentVersion existingVersion = new ContentVersion();
        existingVersion.setId(1L);
        when(versioningDAO.getVersion("ART123", "1.0")).thenReturn(existingVersion);

        // Execute
        assertDoesNotThrow(() -> versioningManager.saveContentVersion("ART123"));

        // Verify addContentVersion was NEVER called since version already exists
        verify(versioningDAO, never()).addContentVersion(any(ContentVersion.class));
    }

    @Test
    void testSaveContentVersion_HandlesNestedDuplicateKeyException() throws Exception {
        // Setup
        when(contentManager.loadContentVO("ART123")).thenReturn(mockContentRecord);
        when(versioningDAO.getVersion(anyString(), anyString())).thenReturn(null);

        // Simulate a deeply nested exception where the root cause is a SQLException
        SQLException sqlException = new SQLException("duplicate key value violates unique constraint", "23505");
        RuntimeException cause = new RuntimeException("Database error", sqlException);
        RuntimeException wrapper = new RuntimeException("Insert failed", cause);
        doThrow(wrapper).when(versioningDAO).addContentVersion(any(ContentVersion.class));

        // Execute - should NOT throw exception because nested cause has duplicate key
        assertDoesNotThrow(() -> versioningManager.saveContentVersion("ART123"));
    }

    @Test
    void testSaveContentVersion_ThrowsForNestedNonDuplicateException() throws Exception {
        // Setup
        when(contentManager.loadContentVO("ART123")).thenReturn(mockContentRecord);
        when(versioningDAO.getVersion(anyString(), anyString())).thenReturn(null);

        // Simulate a nested exception without duplicate key indicators
        RuntimeException cause = new RuntimeException("Connection timeout");
        RuntimeException wrapper = new RuntimeException("Database error", cause);
        doThrow(wrapper).when(versioningDAO).addContentVersion(any(ContentVersion.class));

        // Execute and verify - should throw EntException
        assertThrows(EntException.class, () -> versioningManager.saveContentVersion("ART123"));
    }
}