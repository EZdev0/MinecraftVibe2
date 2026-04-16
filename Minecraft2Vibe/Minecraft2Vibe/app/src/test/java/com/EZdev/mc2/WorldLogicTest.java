package com.EZdev.mc2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class WorldLogicTest {

    @Test
    public void testGetBlockBoundary() {
        WorldLogic worldLogic = new WorldLogic();
        assertNotNull("WorldLogic instance should not be null", worldLogic);

        // Test lower boundary
        assertEquals("Block at y = -1 should be 0", 0, worldLogic.getBlock(0, -1, 0));
        assertEquals("Block at y = -100 should be 0", 0, worldLogic.getBlock(10, -100, 10));

        // Test upper boundary
        assertEquals("Block at y = 128 should be 0", 0, worldLogic.getBlock(0, 128, 0));
        assertEquals("Block at y = 256 should be 0", 0, worldLogic.getBlock(5, 256, 5));

        // Test some valid coordinates (should return 0 because no chunks are loaded/generated in this test context yet)
        assertEquals("Block at y = 0 without chunks should be 0", 0, worldLogic.getBlock(0, 0, 0));
        assertEquals("Block at y = 64 without chunks should be 0", 0, worldLogic.getBlock(0, 64, 0));
        assertEquals("Block at y = 127 without chunks should be 0", 0, worldLogic.getBlock(0, 127, 0));
    }

    @Test
    public void testSetBlockSuccess() {
        WorldLogic worldLogic = new WorldLogic();
        // Force generate chunks around (0,0). updateChunks only generates ONE chunk per call.
        // The first chunk generated with player at (0,0) and renderDistance=2
        // will be the one at (-2, -2) if the loop starts from -renderDistance.
        // To be sure (0,0) is generated, we call it many times.
        for (int i = 0; i < 25; i++) {
            worldLogic.updateChunks(0, 0);
        }

        // Now check (0,0)
        assertEquals("Block at y=0 should be bedrock", 9, worldLogic.getBlock(0, 0, 0));

        // At y=64, it might be air or stone depending on noise, but let's set it to something specific
        worldLogic.setBlock(0, 64, 0, (byte)5); // TNT
        assertEquals("Block at (0,64,0) should be 5", 5, worldLogic.getBlock(0, 64, 0));

        worldLogic.setBlock(0, 64, 0, (byte)2); // Stone
        assertEquals("Block at (0,64,0) should be 2", 2, worldLogic.getBlock(0, 64, 0));
    }

    @Test
    public void testSetBlockOutOfBounds() {
        WorldLogic worldLogic = new WorldLogic();
        for (int i = 0; i < 25; i++) {
            worldLogic.updateChunks(0, 0);
        }

        worldLogic.setBlock(0, -1, 0, (byte)1);
        assertEquals("Setting block at y=-1 should be ignored", 0, worldLogic.getBlock(0, -1, 0));

        worldLogic.setBlock(0, 128, 0, (byte)1);
        assertEquals("Setting block at y=128 should be ignored", 0, worldLogic.getBlock(0, 128, 0));
    }

    @Test
    public void testSetBlockBedrockProtection() {
        WorldLogic worldLogic = new WorldLogic();
        for (int i = 0; i < 25; i++) {
            worldLogic.updateChunks(0, 0);
        }

        // y=0 is always bedrock (9) in generateTerrain
        assertEquals("Block at y=0 should be bedrock", 9, worldLogic.getBlock(0, 0, 0));

        // Try to break it (set to air)
        worldLogic.setBlock(0, 0, 0, (byte)0);
        assertEquals("Bedrock should NOT be breakable with setBlock", 9, worldLogic.getBlock(0, 0, 0));

        // But replacing bedrock with something else (not air) is actually NOT blocked by the code!
        worldLogic.setBlock(0, 0, 0, (byte)2);
        assertEquals("Bedrock SHOULD be replaceable with non-air block according to current logic", 2, worldLogic.getBlock(0, 0, 0));
    }

    @Test
    public void testSetBlockNoChunk() {
        WorldLogic worldLogic = new WorldLogic();
        // Coordinate with no chunk
        int x = 1000, y = 64, z = 1000;
        worldLogic.setBlock(x, y, z, (byte)1);
        assertEquals("Setting block in non-existent chunk should do nothing", 0, worldLogic.getBlock(x, y, z));
    }
}
