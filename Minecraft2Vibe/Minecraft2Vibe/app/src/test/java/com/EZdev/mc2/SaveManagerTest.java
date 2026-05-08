package com.EZdev.mc2;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;

public class SaveManagerTest {

    // A mock WorldLogic to avoid NullPointerException in Chunk constructor
    private static class TestWorldLogic extends WorldLogic {
        public TestWorldLogic() {
            this.saveManager = new SaveManager(null);
        }
    }

    // A subclass of SaveManager that allows us to mock getBaseDir and avoid Log dependency
    private static class TestSaveManager extends SaveManager {
        private File fakeBaseDir;

        public TestSaveManager(File fakeBaseDir) {
            super(null);
            this.fakeBaseDir = fakeBaseDir;
        }

        @Override
        protected File getBaseDir() {
            return fakeBaseDir;
        }
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
        Chunk chunk = new Chunk(new TestWorldLogic(), 0, 0);
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
        Chunk chunk = new Chunk(new TestWorldLogic(), 0, 0);

        try {
            saveManager.saveChunk(chunk);
        } catch (Exception e) {
            fail("Exception should have been caught inside saveChunk: " + e.getMessage());
        }
    }

    @Test
    public void testLoadChunkIOException() {
        // Use a directory instead of a file to trigger IOException on FileInputStream
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "test_world");
        tempDir.mkdirs();
        File worldDir = new File(tempDir, "world1");
        worldDir.mkdirs();
        File chunkFile = new File(worldDir, "chunk_0_0.dat");
        chunkFile.mkdir(); // chunkFile is now a directory

        SaveManager saveManager = new TestSaveManager(tempDir);
        Chunk chunk = new Chunk(new TestWorldLogic(), 0, 0);
        boolean result = saveManager.loadChunk(chunk);
        assertFalse("loadChunk should return false on IOException", result);

        // Cleanup
        chunkFile.delete();
        worldDir.delete();
        tempDir.delete();
    }

    @Test
    public void testLoadChunkGenericException() {
        // Use a subclass that throws a RuntimeException from getBaseDir
        SaveManager saveManager = new TestSaveManager(null) {
            @Override
            protected File getBaseDir() {
                throw new RuntimeException("Simulated generic exception");
            }
        };

        Chunk chunk = new Chunk(new TestWorldLogic(), 0, 0);
        boolean result = saveManager.loadChunk(chunk);
        assertFalse("loadChunk should return false on generic Exception", result);
    }

    @Test
    public void testLoadChunkIncompleteFile() throws Exception {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "test_world_incomplete");
        tempDir.mkdirs();
        File worldDir = new File(tempDir, "world1");
        worldDir.mkdirs();
        File chunkFile = new File(worldDir, "chunk_0_0.dat");

        // Create a truncated file (e.g., only 10 bytes instead of 16*128*16)
        java.io.FileOutputStream fos = new java.io.FileOutputStream(chunkFile);
        fos.write(new byte[10]);
        fos.close();

        SaveManager saveManager = new TestSaveManager(tempDir);
        Chunk chunk = new Chunk(new TestWorldLogic(), 0, 0);
        boolean result = saveManager.loadChunk(chunk);

        assertFalse("loadChunk should return false for incomplete file", result);

        // Cleanup
        chunkFile.delete();
        worldDir.delete();
        tempDir.delete();
    }
}
