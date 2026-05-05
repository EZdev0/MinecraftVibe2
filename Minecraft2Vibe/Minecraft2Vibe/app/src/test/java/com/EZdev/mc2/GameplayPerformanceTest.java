package com.EZdev.mc2;

import org.junit.Test;
import java.util.ArrayList;

public class GameplayPerformanceTest {

    @Test
    public void benchmarkTntAndParticles() {
        Gameplay gameplay = new Gameplay();
        // Mock WorldLogic to avoid expensive chunk generation/loading
        WorldLogic world = new WorldLogic() {
            @Override
            public byte getBlock(int x, int y, int z) { return 0; } // AIR
            @Override
            public void setBlock(int x, int y, int z, byte block) {}
            @Override
            public void explode(float x, float y, float z, float radius) {}
            @Override
            public int[] raycastBlock(Gameplay g) { return null; }
        };

        int iterations = 1000;
        float dt = 0.05f;

        // Warm up
        for (int i = 0; i < 100; i++) {
            gameplay.update(dt, world);
        }

        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            // Spawn some TNT every few frames
            if (i % 10 == 0) {
                for (int j = 0; j < 5; j++) {
                    gameplay.tickingTNTs.add(gameplay.new ActiveTNT(10, 100, 10));
                }
            }
            // Spawn some particles every frame
            gameplay.addBlockParticles(10, 100, 10, (byte)1);
            gameplay.addExplosionParticles(10, 100, 10);

            gameplay.update(dt, world);
        }
        long endTime = System.nanoTime();

        double durationMs = (endTime - startTime) / 1000000.0;
        System.out.println("Execution Time (Baseline): " + durationMs + " ms");
        System.out.println("Average per frame: " + (durationMs / iterations) + " ms");
    }
}
