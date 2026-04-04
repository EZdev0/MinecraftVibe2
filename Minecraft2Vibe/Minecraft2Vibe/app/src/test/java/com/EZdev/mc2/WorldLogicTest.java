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
}
