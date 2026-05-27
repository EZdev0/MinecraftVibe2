package com.EZdev.mc2;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class GameplayTest {

    @Test
    public void testAddBlockParticles() {
        Gameplay gameplay = new Gameplay();
        float x = 10.0f;
        float y = 20.0f;
        float z = 30.0f;
        byte blockType = 1;

        gameplay.addBlockParticles((int)x, (int)y, (int)z, blockType);

        assertEquals("Should add 8 block particles", 8, gameplay.blockParticles.size());
        for (Gameplay.ActiveFireParticle p : gameplay.blockParticles) {
            assertEquals("X coordinate should have 0.5f offset", (int)x + 0.5f, p.x, 0.001f);
            assertEquals("Y coordinate should have 0.5f offset", (int)y + 0.5f, p.y, 0.001f);
            assertEquals("Z coordinate should have 0.5f offset", (int)z + 0.5f, p.z, 0.001f);
            assertEquals("Particle type should match block type", blockType, p.type);
        }
    }

    @Test
    public void testAddExplosionParticles() {
        Gameplay gameplay = new Gameplay();
        float x = 50.0f;
        float y = 60.0f;
        float z = 70.0f;

        gameplay.addExplosionParticles(x, y, z);

        assertEquals("Should add 25 explosion particles", 25, gameplay.fireParticles.size());
        for (Gameplay.ActiveFireParticle p : gameplay.fireParticles) {
            assertEquals("X coordinate should match", x, p.x, 0.001f);
            assertEquals("Y coordinate should match", y, p.y, 0.001f);
            assertEquals("Z coordinate should match", z, p.z, 0.001f);
            assertEquals("Explosion particle type should be FIRE", Blocks.FIRE, p.type);
        }
    }
}
