/*
 * Copyright 2013-Present S.r.l. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.apsadmin.filebrowser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.apsadmin.ApsAdminBaseTestCase;
import com.opensymphony.xwork2.Action;
import org.entando.entando.aps.system.services.storage.*;

import java.io.*;
import java.util.*;

import org.entando.entando.ent.exception.EntRuntimeException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestFileBrowserAction extends ApsAdminBaseTestCase {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(TestFileBrowserAction.class);

    private IStorageManager localStorageManager;

    @Test
    void testBrowseFileSystemWithUserNotAllowed() throws Throwable {
        String result = this.executeList("developersConf", null, null);
        assertEquals("apslogin", result);
    }

    @Test
    void testBrowseFileSystem_1() throws Throwable {
        String result = this.executeList("admin", null, null);
        assertEquals(Action.SUCCESS, result);
        FileBrowserAction action = (FileBrowserAction) super.getAction();
        BasicFileAttributeView[] fileAttributes = action.getFilesAttributes();
        assertNotNull(fileAttributes);
        assertEquals(2, fileAttributes.length);
        for (int i = 0; i < fileAttributes.length; i++) {
            BasicFileAttributeView bfav = fileAttributes[i];
            assertTrue(bfav instanceof RootFolderAttributeView);
            assertTrue(bfav.isDirectory());
            if (i == 0) {
                assertEquals("public", bfav.getName());
            } else {
                assertEquals("protected", bfav.getName());
            }
        }
    }

    @Test
    void testBrowseFileSystem_2() throws Throwable {
        String result = this.executeList("admin", null, false);
        assertEquals(Action.SUCCESS, result);
        FileBrowserAction action = (FileBrowserAction) super.getAction();
        BasicFileAttributeView[] fileAttributes = action.getFilesAttributes();
        assertNotNull(fileAttributes);
        boolean containsConf = false;
        boolean prevDirectory = true;
        String prevName = null;
        for (BasicFileAttributeView bfav : fileAttributes) {
            if (!prevDirectory && bfav.isDirectory()) {
                fail();
            }
            if (bfav.isDirectory() && bfav.getName().equals("conf")) {
                containsConf = true;
            }
            if ((bfav.isDirectory() == prevDirectory) && null != prevName) {
                assertTrue(bfav.getName().compareTo(prevName) > 0);
            }
            prevName = bfav.getName();
            prevDirectory = bfav.isDirectory();
        }
        assertTrue(containsConf);
    }

    @Test
    void testBrowseFileSystem_3() throws Throwable {
        String result = this.executeList("admin", "conf/", false);
        assertEquals(Action.SUCCESS, result);
        FileBrowserAction action = (FileBrowserAction) super.getAction();
        BasicFileAttributeView[] fileAttributes = action.getFilesAttributes();
        assertEquals(2, fileAttributes.length);
        int dirCounter = 0;
        int fileCounter = 0;
        for (BasicFileAttributeView bfav : fileAttributes) {
            if (bfav.isDirectory()) {
                dirCounter++;
            } else {
                fileCounter++;
            }
        }
        assertEquals(0, dirCounter);
        assertEquals(2, fileCounter);
    }

    @Test
    void testValidateAddTextFile() throws Throwable {
        String path = "conf/";
        try {
            String result = this.executeAddTextFile("developersConf", path, "filename", "css", "content", false);
            assertEquals("apslogin", result);

            result = this.executeAddTextFile("admin", path, "", "", "content", false);
            assertEquals(Action.INPUT, result);
            assertEquals(2, this.getAction().getFieldErrors().size());

            result = this.executeAddTextFile("admin", path, "filename", "", "", false);
            assertEquals(Action.INPUT, result);
            assertEquals(1, this.getAction().getFieldErrors().size());

            result = this.executeAddTextFile("admin", path, "filename", "exe", "content", false);
            assertEquals(Action.INPUT, result);
            assertEquals(1, this.getAction().getFieldErrors().size());

        } catch (Throwable t) {
            throw t;
        }
    }

    @Test
    void testAddTextFile() throws Throwable {
        String path = "conf/";
        String filename = "test_filename_1";
        String extension = "css";
        String fullPath = path + filename + "." + extension;
        String text = "This is the content";
        try {
            String result = this.executeAddTextFile("admin", path, filename, extension, text, false);
            //FileBrowserAction action = (FileBrowserAction) this.getAction();
            assertEquals(Action.SUCCESS, result);
            assertTrue(this.localStorageManager.exists(fullPath, false));

            result = this.executeAddTextFile("admin", path, filename, extension, text, false);
            assertEquals(Action.INPUT, result);
            assertEquals(1, this.getAction().getFieldErrors().size());
            assertEquals(1, this.getAction().getFieldErrors().get("filename").size());

            String extractedText = this.localStorageManager.readFile(fullPath, false);
            assertEquals(text, extractedText);
            this.localStorageManager.deleteFile(fullPath, false);
            assertFalse(this.localStorageManager.exists(fullPath, false));
        } catch (Throwable t) {
            this.localStorageManager.deleteFile(fullPath, false);
            throw t;
        }
    }

    @Test
    void testAddTextFileWithErrors() throws Throwable {
        String path = "conf/";
        String filename = "test_filename_1";
        String extension = "css";
        String fullPath = path + filename + "." + extension;
        String text = "This is the content";
        try {
            String filename2 = "../" + filename;
            fullPath = path + filename2 + "." + extension;
            String result = this.executeAddTextFile("admin", path, filename2, extension, text, false);
            assertEquals(Action.INPUT, result);
            FileBrowserAction action = (FileBrowserAction) this.getAction();
            Collection<String> actionErrors = action.getActionErrors();
            assertEquals(1, actionErrors.size());
            this.localStorageManager.deleteFile(fullPath, false);

            path = "../" + path;
            fullPath = path + filename + "." + extension;
            result = this.executeAddTextFile("admin", path, filename, extension, text, false);
            assertEquals(Action.INPUT, result);
            action = (FileBrowserAction) this.getAction();
            actionErrors = action.getActionErrors();
            assertEquals(1, actionErrors.size());
            try {
                this.localStorageManager.deleteFile(fullPath, false);
            } catch (EntRuntimeException ignore) {
            }
        } catch (Throwable t) {
            try {
                this.localStorageManager.deleteFile(fullPath, false);
            } catch (EntRuntimeException ignore) {
            }
            throw t;
        }
    }

    @Test
    void testDeleteFile() throws Throwable {
        String path = "conf/";
        String filename = "test_filename_2";
        String extension = "css";
        String fullFilename = filename + "." + extension;
        String fullPath = path + fullFilename;
        String text = "This is the content";
        try {
            assertFalse(this.localStorageManager.exists(fullPath, false));
            this.localStorageManager.saveFile(fullPath, false, new ByteArrayInputStream(text.getBytes()));
            assertTrue(this.localStorageManager.exists(fullPath, false));
            String result = this.executeDeleteFile("admin", path, fullFilename, true, false);
            assertEquals(Action.SUCCESS, result);
            assertFalse(this.localStorageManager.exists(fullPath, false));
        } catch (Throwable t) {
            this.localStorageManager.deleteFile(fullPath, false);
            throw t;
        }
    }

    @Test
    void testTrash() throws Throwable {
        String path = "conf/";
        String filename = "test_filename_2";
        String extension = "css";
        String fullFilename = filename + "." + extension;
        String fullPath = path + fullFilename;
        String text = "This is the content";
        this.localStorageManager.deleteFile(fullPath, false);
        try {
            assertFalse(this.localStorageManager.exists(fullPath, false));
            this.localStorageManager.saveFile(fullPath, false, new ByteArrayInputStream(text.getBytes()));
            assertTrue(this.localStorageManager.exists(fullPath, false));

            String result = this.executeTrashFile("admin", path, fullFilename, false);
            assertEquals(Action.SUCCESS, result);

            String path2 = path + "../../";
            result = this.executeTrashFile("admin", path2, fullFilename, false);
            assertEquals(Action.INPUT, result);
            FileBrowserAction action = (FileBrowserAction) this.getAction();
            Collection<String> actionErrors = action.getActionErrors();
            assertEquals(1, actionErrors.size());

            fullFilename = "../conf/" + fullFilename;
            result = this.executeTrashFile("admin", path, fullFilename, false);
            assertEquals(Action.INPUT, result);
            action = (FileBrowserAction) this.getAction();
            actionErrors = action.getActionErrors();
            assertEquals(1, actionErrors.size());

        } catch (Throwable t) {
            this.localStorageManager.deleteFile(fullPath, false);
            throw t;
        }

        this.localStorageManager.deleteFile(fullPath, false);
    }

    private String executeList(String currentUser, String path, Boolean isProtected) throws Throwable {
        this.setUserOnSession(currentUser);
        this.initAction("/do/FileBrowser", "list");
        this.addParameter("currentPath", path);
        if (null != isProtected) {
            this.addParameter("protectedFolder", isProtected.toString());
        }
        return this.executeAction();
    }

    private String executeAddTextFile(String currentUser, String currentPath,
            String filename, String extension, String content, Boolean isProtected) throws Throwable {
        this.setUserOnSession(currentUser);
        this.initAction("/do/FileBrowser", "save");
        this.addParameter("currentPath", currentPath);
        this.addParameter("filename", filename);
        this.addParameter("textFileExtension", extension);
        this.addParameter("fileText", content);
        this.addParameter("strutsAction", FileBrowserAction.ADD_NEW_FILE);
        if (null != isProtected) {
            this.addParameter("protectedFolder", isProtected.toString());
        }
        return this.executeAction();
    }

    private String executeDeleteFile(String currentUser, String currentPath,
            String filename, boolean deleteFile, Boolean isProtected) throws Throwable {
        this.setUserOnSession(currentUser);
        this.initAction("/do/FileBrowser", "delete");
        this.addParameter("currentPath", currentPath);
        this.addParameter("filename", filename);
        this.addParameter("deleteFile", Boolean.toString(deleteFile));
        if (null != isProtected) {
            this.addParameter("protectedFolder", isProtected.toString());
        }
        return this.executeAction();
    }

    private String executeTrashFile(String currentUser, String currentPath, String filename, Boolean isProtected) throws Throwable {
        this.setUserOnSession(currentUser);
        this.initAction("/do/FileBrowser", "trash");
        this.addParameter("currentPath", currentPath);
        this.addParameter("filename", filename);
        if (null != isProtected) {
            this.addParameter("protectedFolder", isProtected.toString());
        }
        return this.executeAction();
    }

    @Test
    void testUploadFile() throws Throwable {
        String filepath = "test/";
        String filename1 = "test-upload1.txt";
        String filename2 = "test-upload2.txt";

        String filenameWithPath1 = filepath + filename1;
        String filenameWithPath2 = filepath + filename2;

        File fileResource1 = new File(filenameWithPath1);
        File fileResource2 = new File(filenameWithPath2);

        InputStream inputStream1 = new ByteArrayInputStream(filename1.getBytes());
        InputStream inputStream2 = new ByteArrayInputStream(filename2.getBytes());

        List<File> files = new ArrayList<>();
        files.add(fileResource1);
        files.add(fileResource2);
        List<String> uploadFileNames = new ArrayList<>();
        uploadFileNames.add(filename1);
        uploadFileNames.add(filename2);
        List<InputStream> uploadInputStreams = new ArrayList<>();
        uploadInputStreams.add(inputStream1);
        uploadInputStreams.add(inputStream2);

        initAction("/do/FileBrowser", "upload");

        FileBrowserAction action = (FileBrowserAction) super.getAction();

        boolean protectedFolder = false;
        String result = action.uploadFiles(files, uploadFileNames, uploadInputStreams, filepath, protectedFolder);

        assertEquals(Action.SUCCESS, result);
        assertTrue(localStorageManager.exists(filenameWithPath1, protectedFolder));
        assertTrue(localStorageManager.exists(filenameWithPath2, protectedFolder));

        result = action.uploadFiles(files, uploadFileNames, uploadInputStreams, filepath, protectedFolder);
        assertEquals(Action.INPUT, result);

        localStorageManager.deleteFile(filenameWithPath1, protectedFolder);
        localStorageManager.deleteFile(filenameWithPath2, protectedFolder);

        assertFalse(localStorageManager.exists(filenameWithPath1, protectedFolder));
        assertFalse(localStorageManager.exists(filenameWithPath2, protectedFolder));
    }

    @BeforeEach
    private void init() {
        try {
            this.localStorageManager = (IStorageManager) this.getApplicationContext().getBean(SystemConstants.STORAGE_MANAGER);
        } catch (Throwable t) {
            logger.error("error on init", t);
        }
    }

}

