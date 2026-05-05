package com.EZdev.mc2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.Test;

public class WorldLogicTest {

    @Test
    public void testGetBlockBoundary() {
        WorldLogic worldLogic = new WorldLogic();
        assertNotNull("WorldLogic instance should not be null", worldLogic);

        // Test lower boundary
        assertEquals("Block at y = -1 should be AIR", Blocks.AIR, worldLogic.getBlock(0, -1, 0));
        assertEquals("Block at y = -100 should be AIR", Blocks.AIR, worldLogic.getBlock(10, -100, 10));

        // Test upper boundary
        assertEquals("Block at y = 128 should be AIR", Blocks.AIR, worldLogic.getBlock(0, 128, 0));
        assertEquals("Block at y = 256 should be AIR", Blocks.AIR, worldLogic.getBlock(5, 256, 5));

        // Test some valid coordinates (should return AIR because no chunks are loaded/generated in this test context yet)
        assertEquals("Block at y = 0 without chunks should be AIR", Blocks.AIR, worldLogic.getBlock(0, 0, 0));
        assertEquals("Block at y = 64 without chunks should be AIR", Blocks.AIR, worldLogic.getBlock(0, 64, 0));
        assertEquals("Block at y = 127 without chunks should be AIR", Blocks.AIR, worldLogic.getBlock(0, 127, 0));
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
        assertEquals("Block at y=0 should be bedrock", Blocks.BEDROCK, worldLogic.getBlock(0, 0, 0));

        // At y=64, it might be air or stone depending on noise, but let's set it to something specific
        worldLogic.setBlock(0, 64, 0, Blocks.TNT); // TNT
        assertEquals("Block at (0,64,0) should be TNT", Blocks.TNT, worldLogic.getBlock(0, 64, 0));

        worldLogic.setBlock(0, 64, 0, Blocks.STONE); // Stone
        assertEquals("Block at (0,64,0) should be STONE", Blocks.STONE, worldLogic.getBlock(0, 64, 0));
    }

    @Test
    public void testSetBlockOutOfBounds() {
        WorldLogic worldLogic = new WorldLogic();
        for (int i = 0; i < 25; i++) {
            worldLogic.updateChunks(0, 0);
        }

        worldLogic.setBlock(0, -1, 0, Blocks.GRASS);
        assertEquals("Setting block at y=-1 should be ignored", Blocks.AIR, worldLogic.getBlock(0, -1, 0));

        worldLogic.setBlock(0, 128, 0, Blocks.GRASS);
        assertEquals("Setting block at y=128 should be ignored", Blocks.AIR, worldLogic.getBlock(0, 128, 0));
    }

    @Test
    public void testSetBlockBedrockProtection() {
        WorldLogic worldLogic = new WorldLogic();
        for (int i = 0; i < 25; i++) {
            worldLogic.updateChunks(0, 0);
        }

        // y=0 is always bedrock in generateTerrain
        assertEquals("Block at y=0 should be bedrock", Blocks.BEDROCK, worldLogic.getBlock(0, 0, 0));

        // Try to break it (set to air)
        worldLogic.setBlock(0, 0, 0, Blocks.AIR);
        assertEquals("Bedrock should NOT be breakable with setBlock", Blocks.BEDROCK, worldLogic.getBlock(0, 0, 0));

        // But replacing bedrock with something else (not air) is actually NOT blocked by the code!
        worldLogic.setBlock(0, 0, 0, Blocks.STONE);
        assertEquals("Bedrock SHOULD be replaceable with non-air block according to current logic", Blocks.STONE, worldLogic.getBlock(0, 0, 0));
    }

    @Test
    public void testSetBlockNoChunk() {
        WorldLogic worldLogic = new WorldLogic();
        // Coordinate with no chunk
        int x = 1000, y = 64, z = 1000;
        worldLogic.setBlock(x, y, z, Blocks.GRASS);
        assertEquals("Setting block in non-existent chunk should do nothing", Blocks.AIR, worldLogic.getBlock(x, y, z));
    }

    @Test
    public void testSpawnItemEntityNegativeCount() {
        WorldLogic worldLogic = new WorldLogic();
        worldLogic.spawnItemEntity(10.0f, 64.0f, 10.0f, Blocks.GRASS, -5);

        assertEquals("One item entity should have been spawned", 1, worldLogic.droppedItems.size());
        assertEquals("Negative count should be clamped to 0", 0, worldLogic.droppedItems.get(0).count);
    }

    @Test
    public void testExplodeRemovesBlocks() {
        WorldLogic worldLogic = new WorldLogic();
        for (int i = 0; i < 25; i++) {
            worldLogic.updateChunks(0, 0);
        }

        // Set some blocks to be exploded
        worldLogic.setBlock(0, 64, 0, Blocks.STONE);
        worldLogic.setBlock(1, 64, 0, Blocks.STONE);
        worldLogic.setBlock(0, 65, 0, Blocks.STONE);

        assertEquals("Block should be stone before explosion", Blocks.STONE, worldLogic.getBlock(0, 64, 0));

        // Explode at (0.5, 64.5, 0.5) with radius 2.0
        worldLogic.explode(0.5f, 64.5f, 0.5f, 2.0f);

        assertEquals("Block at center should be AIR after explosion", Blocks.AIR, worldLogic.getBlock(0, 64, 0));
        assertEquals("Block at (1,64,0) should be AIR after explosion", Blocks.AIR, worldLogic.getBlock(1, 64, 0));
        assertEquals("Block at (0,65,0) should be AIR after explosion", Blocks.AIR, worldLogic.getBlock(0, 65, 0));
    }

    @Test
    public void testExplodeBedrockImmunity() {
        WorldLogic worldLogic = new WorldLogic();
        for (int i = 0; i < 25; i++) {
            worldLogic.updateChunks(0, 0);
        }

        // y=0 is bedrock
        assertEquals("Block at y=0 should be bedrock", Blocks.BEDROCK, worldLogic.getBlock(0, 0, 0));

        // Explode at (0.5, 0.5, 0.5) with radius 2.0
        worldLogic.explode(0.5f, 0.5f, 0.5f, 2.0f);

        assertEquals("Bedrock should persist after explosion", Blocks.BEDROCK, worldLogic.getBlock(0, 0, 0));
    }

    @Test
    public void testExplodeWithGameplay() throws Exception {
        WorldLogic worldLogic = new WorldLogic();
        for (int i = 0; i < 25; i++) {
            worldLogic.updateChunks(0, 0);
        }

        Gameplay gameplay = new Gameplay();
        // We use reflection to set gameplayRef to avoid calling render() which triggers GLES20 (stubbed in tests)
        java.lang.reflect.Field field = WorldLogic.class.getDeclaredField("gameplayRef");
        field.setAccessible(true);
        field.set(worldLogic, gameplay);

        // Setup some blocks and TNT
        worldLogic.setBlock(0, 64, 0, Blocks.TNT);

        worldLogic.explode(5f, 64f, 5f, 10f);

        // Check if particles were added to gameplay
        // addExplosionParticles adds 25 particles
        // addBlockParticles adds 8 particles per block
        // Since we exploded (5,64,5) and TNT was at (0,64,0), and radius was 10.
        // Distance is ~7.07, radius 10. TNT should be affected.

        assertTrue("Explosion particles should be added", gameplay.fireParticles.size() >= 25);
        // The TNT block should be removed and a ticking TNT should be added to gameplay
        assertEquals("TNT block should be removed", Blocks.AIR, worldLogic.getBlock(0, 64, 0));
        assertFalse("Ticking TNT should be added to gameplay", gameplay.tickingTNTs.isEmpty());
    }

    @Test
    public void testCheckIgnition() {
        WorldLogic worldLogic = new WorldLogic();
        for (int i = 0; i < 25; i++) {
            worldLogic.updateChunks(0, 0);
        }
        Gameplay gameplay = new Gameplay();

        // Place TNT next to the target coordinates (0, 64, 0)
        worldLogic.setBlock(1, 64, 0, Blocks.TNT);
        worldLogic.setBlock(-1, 64, 0, Blocks.TNT);
        worldLogic.setBlock(0, 65, 0, Blocks.TNT);
        worldLogic.setBlock(0, 63, 0, Blocks.TNT);
        worldLogic.setBlock(0, 64, 1, Blocks.TNT);
        worldLogic.setBlock(0, 64, -1, Blocks.TNT);

        // Call checkIgnition as if fire was placed at (0, 64, 0)
        worldLogic.checkIgnition(0, 64, 0, gameplay);

        // Verify all 6 adjacent TNT blocks were removed and added to tickingTNTs
        assertEquals("All 6 adjacent TNT blocks should be removed", 6, gameplay.tickingTNTs.size());
        assertEquals("Block at (1,64,0) should be AIR", Blocks.AIR, worldLogic.getBlock(1, 64, 0));
        assertEquals("Block at (-1,64,0) should be AIR", Blocks.AIR, worldLogic.getBlock(-1, 64, 0));
        assertEquals("Block at (0,65,0) should be AIR", Blocks.AIR, worldLogic.getBlock(0, 65, 0));
        assertEquals("Block at (0,63,0) should be AIR", Blocks.AIR, worldLogic.getBlock(0, 63, 0));
        assertEquals("Block at (0,64,1) should be AIR", Blocks.AIR, worldLogic.getBlock(0, 64, 1));
        assertEquals("Block at (0,64,-1) should be AIR", Blocks.AIR, worldLogic.getBlock(0, 64, -1));
    }

    @Test
    public void testCheckIgnitionIfTntPlaced() throws Exception {
        WorldLogic worldLogic = new WorldLogic();
        for (int i = 0; i < 25; i++) {
            worldLogic.updateChunks(0, 0);
        }
        Gameplay gameplay = new Gameplay();

        // Place fire next to where TNT will be "placed" (0, 64, 0)
        worldLogic.setBlock(1, 64, 0, Blocks.FIRE);
        // Set the TNT block
        worldLogic.setBlock(0, 64, 0, Blocks.TNT);

        // Use reflection to call private checkIgnitionIfTntPlaced
        java.lang.reflect.Method method = WorldLogic.class.getDeclaredMethod("checkIgnitionIfTntPlaced", int.class, int.class, int.class, Gameplay.class);
        method.setAccessible(true);
        method.invoke(worldLogic, 0, 64, 0, gameplay);

        // Verify TNT block was removed and added to tickingTNTs
        assertEquals("Ticking TNT should be added", 1, gameplay.tickingTNTs.size());
        assertEquals("TNT block should be removed", Blocks.AIR, worldLogic.getBlock(0, 64, 0));
    }
}
