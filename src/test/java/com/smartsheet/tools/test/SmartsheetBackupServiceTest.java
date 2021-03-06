/**
   Copyright 2013 Smartsheet.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

**/
package com.smartsheet.tools.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.smartsheet.exceptions.ServiceUnavailableException;
import com.smartsheet.restapi.service.RetryingSmartsheetService;
import com.smartsheet.tools.ParallelDownloadService;
import com.smartsheet.tools.SmartsheetBackupService;

/**
 * Note these are <i>integration</i> tests of the {@link SmartsheetBackupService}
 * since they involve the file system, the Internet, and time.
 */
public class SmartsheetBackupServiceTest {

    // test fixture
    private final static int DOWNLOAD_THREADS = 4;
    private final static int ALL_DOWNLOADS_DONE_TIMEOUT_MINUTES = 2;
    private ParallelDownloadService parallelDownloadService;

    /**
     * set up test fixture
     */
    @Before
    public void setUp() {
        parallelDownloadService = new ParallelDownloadService(
            DOWNLOAD_THREADS, ALL_DOWNLOADS_DONE_TIMEOUT_MINUTES);
    }

    /**
     * tear down test fixture
     */
    @After
    public void tearDown() {
        // assert all downloads done after wait
        assertTrue(parallelDownloadService.waitTillAllDownloadJobsDone());
    }

    @Test
    public void backsUpSmartsheetHierarchyToLocalDir() throws Exception {
        printTestHeader("backsUpSmartsheetHierarchyToLocalDir");

        SmartsheetBackupService backupService = new SmartsheetBackupService(
            new RetryingSmartsheetService(new StubSmartsheetService()),
            parallelDownloadService);

        backupToTempDir(backupService);
    }

    @Test
    public void backsUpSmartsheetHierarchyToLocalDirRecoveringFromServiceUnavailable() throws Exception {
        printTestHeader("backsUpSmartsheetHierarchyToLocalDirRecoveringFromServiceUnavailable");

        SmartsheetBackupService backupService = new SmartsheetBackupService(
            new RetryingSmartsheetService(new StubRecoveringServiceUnavailableSmartsheetService()),
            parallelDownloadService);

        backupToTempDir(backupService);
    }

    @Test(expected = ServiceUnavailableException.class)
    public void retriesOnServiceUnavailableExceptionsUntilMaxRetries() throws Exception {
        printTestHeader("retriesOnServiceUnavailableExceptionsUntilMaxRetries");

        SmartsheetBackupService backupService = new SmartsheetBackupService(
            new RetryingSmartsheetService(new StubServiceUnavailableSmartsheetService()),
            parallelDownloadService);

        backupToTempDir(backupService);
    }

    // helpers

    private static void printTestHeader(String testName) {
        System.out.println("-------------------- TEST: " + testName  + " --------------------");
    }

    private void backupToTempDir(SmartsheetBackupService backupService) throws Exception {
        Date timeBeforeBackup = new Date();

        File backupFolder = new File(
            System.getProperty("java.io.tmpdir"),
            getClass().getSimpleName() + "-" + System.currentTimeMillis());

        backupService.backupOrgTo(backupFolder);

        assertTrue(backupFolder.exists());
        assertTrue(backupFolder.lastModified() > timeBeforeBackup.getTime());
    }
}
