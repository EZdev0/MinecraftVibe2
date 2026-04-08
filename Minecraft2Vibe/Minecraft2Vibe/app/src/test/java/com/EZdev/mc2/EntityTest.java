package com.EZdev.mc2;

import static org.junit.Assert.*;
import org.junit.Test;

public class EntityTest {

    @Test
    public void testInitialization() {
        Entity entity = new Entity(10.0f, 20.0f, 30.0f);
        assertEquals(10.0f, entity.x, 0.001f);
        assertEquals(20.0f, entity.y, 0.001f);
        assertEquals(30.0f, entity.z, 0.001f);
        assertEquals(10.0f, entity.targetX, 0.001f);
        assertEquals(30.0f, entity.targetZ, 0.001f);
        assertFalse(entity.onGround);
    }

    @Test
    public void testGravity() {
        Entity entity = new Entity(0, 10, 0);
        WorldLogic world = new WorldLogic() {
            @Override
            public byte getBlock(int x, int y, int z) {
                return 0; // Air
            }
        };

        float dt = 0.1f;
        entity.update(dt, world);

        assertTrue("Entity should fall due to gravity", entity.y < 10);
        assertFalse("Entity should not be on ground while falling", entity.onGround);
        assertEquals(-20.0f * dt, entity.vy, 0.001f);
    }

    @Test
    public void testLandingOnGround() {
        Entity entity = new Entity(0, 0.5f, 0);
        entity.vy = -1.0f;

        WorldLogic world = new WorldLogic() {
            @Override
            public byte getBlock(int x, int y, int z) {
                if (y < 0) return 1; // Solid ground at y < 0
                return 0;
            }
        };

        // Update such that it would fall below 0
        entity.update(1.0f, world);

        assertEquals(0.5f, entity.y, 0.001f); // Should not have moved down if nextY ( -0.5) is colliding
        assertTrue("Entity should be on ground", entity.onGround);
        assertEquals(0f, entity.vy, 0.001f);
    }

    @Test
    public void testHorizontalMovement() {
        Entity entity = new Entity(0, 10, 0);
        entity.targetX = 10;
        entity.targetZ = 0;
        entity.changeDirTimer = 5.0f; // Prevent wandering from changing target immediately

        WorldLogic world = new WorldLogic() {
            @Override
            public byte getBlock(int x, int y, int z) { return 0; }
        };

        entity.update(0.1f, world);

        assertTrue("Entity should move towards targetX", entity.x > 0);
        assertEquals(0, entity.z, 0.001f);
    }

    @Test
    public void testAutoJump() {
        // Entity at (0.9, 0, 0.5) wanting to move to (2.0, 0, 0.5)
        Entity entity = new Entity(0.9f, 0, 0.5f);
        entity.onGround = true;
        entity.targetX = 2.0f;
        entity.targetZ = 0.5f;
        entity.changeDirTimer = 5.0f;

        WorldLogic world = new WorldLogic() {
            @Override
            public byte getBlock(int x, int y, int z) {
                // Floor
                if (y < 0) return 1;
                // Obstacle at (1, 0, 0)
                if (x == 1 && y == 0 && z == 0) return 1;
                return 0;
            }
        };

        // Next update:
        // vy becomes -2.0. nextY = -0.2. floor(-0.2)=-1. getBlock(0,-1,0)=1.
        // nextY is colliding, so y stays 0, vy=0, onGround=true.
        // nextX = 0.9 + 1.5 * 0.1 = 1.05. floor(1.05)=1. floor(z)=0.
        // getBlock(1,0,0) = 1. Collision!
        // Should jump to y = 1.1.

        entity.update(0.1f, world);

        assertEquals(1.1f, entity.y, 0.001f); // Jumped up
    }

    @Test
    public void testWandering() {
        Entity entity = new Entity(0, 0, 0);
        entity.changeDirTimer = 0.01f;

        WorldLogic world = new WorldLogic() {
            @Override
            public byte getBlock(int x, int y, int z) { return 0; }
        };

        float initialTargetX = entity.targetX;
        float initialTargetZ = entity.targetZ;

        entity.update(0.1f, world);

        assertNotEquals("Target should have changed", initialTargetX, entity.targetX, 0.0001f);
        assertTrue("changeDirTimer should be reset", entity.changeDirTimer > 0);
    }
}
