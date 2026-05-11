package com.EZdev.mc2;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import java.util.HashSet;
import java.util.Set;

public class BlocksTest {

    @Test
    public void testBlockConstants() {
        assertEquals(0, Blocks.AIR);
        assertEquals(1, Blocks.GRASS);
        assertEquals(10, Blocks.DIRT);
        assertEquals(2, Blocks.STONE);
        assertEquals(3, Blocks.WOOD);
        assertEquals(4, Blocks.LEAVES);
        assertEquals(5, Blocks.TNT);
        assertEquals(6, Blocks.FIRE);
        assertEquals(7, Blocks.WATER);
        assertEquals(8, Blocks.SAND);
        assertEquals(9, Blocks.BEDROCK);
        assertEquals(99, Blocks.SMOKE);
        assertEquals(100, Blocks.ENTITY_PIG);
    }

    @Test
    public void testUniqueness() {
        byte[] allBlocks = {
            Blocks.AIR, Blocks.GRASS, Blocks.DIRT, Blocks.STONE, Blocks.WOOD,
            Blocks.LEAVES, Blocks.TNT, Blocks.FIRE, Blocks.WATER, Blocks.SAND,
            Blocks.BEDROCK, Blocks.SMOKE, Blocks.ENTITY_PIG
        };
        Set<Byte> set = new HashSet<>();
        for (byte b : allBlocks) {
            set.add(b);
        }
        assertEquals("Block IDs should be unique", allBlocks.length, set.size());
    }
}
