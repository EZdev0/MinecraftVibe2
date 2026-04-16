package com.EZdev.mc2;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;

public class SaveManagerTest {

    // A subclass of SaveManager that allows us to mock getBaseDir and avoid Log dependency
    private static class TestSaveManager extends SaveManager {
        private File fakeBaseDir;
        private boolean logCalled = false;

        public TestSaveManager(File fakeBaseDir) {
            super(null);
            this.fakeBaseDir = fakeBaseDir;
        }

        @Override
        protected File getBaseDir() {
            return fakeBaseDir;
        }

        // We can't easily override Log.e because it's static, but our SaveManager uses it.
        // In local unit tests, android.util.Log needs to be stubbed or we must avoid its execution.
        // For the purpose of this test, we assume the environment (like our custom stubs or Robolectric)
        // handles Log.e.
    }

    @Test
    public void testSaveChunkWithNullChunk() {
        SaveManager saveManager = new TestSaveManager(null);
        try {
            saveManager.saveChunk(null);
        } catch (Exception e) {
            fail("Exception should have been caught inside saveChunk: " + e.getMessage());
        }
    }

    @Test
    public void testSaveChunkWithNullBaseDir() {
        SaveManager saveManager = new TestSaveManager(null);
        Chunk chunk = new Chunk(null, 0, 0);
        try {
            saveManager.saveChunk(chunk);
        } catch (Exception e) {
            fail("Exception should have been caught inside saveChunk: " + e.getMessage());
        }
    }

    @Test
    public void testLoadChunkWithNullChunk() {
        SaveManager saveManager = new TestSaveManager(null);
        try {
            boolean result = saveManager.loadChunk(null);
            assertFalse("loadChunk should return false for null chunk", result);
        } catch (Exception e) {
            fail("Exception should have been caught inside loadChunk: " + e.getMessage());
        }
    }

    @Test
    public void testSaveChunkIOException() {
        // Use a directory that definitely doesn't exist and can't be created
        SaveManager saveManager = new TestSaveManager(new File("/proc/restricted_non_existent"));
        Chunk chunk = new Chunk(null, 0, 0);

        try {
            saveManager.saveChunk(chunk);
        } catch (Exception e) {
            fail("Exception should have been caught inside saveChunk: " + e.getMessage());
        }
    }
}
