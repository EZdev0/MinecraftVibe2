package com.EZdev.mc2;

import java.util.ArrayList;
import java.util.Random;

public class StandaloneBenchmark {

    static class ActiveTNT {
        public float x, y, z;
        public float vx = 0f, vy = 0f, vz = 0f;
        public float timer = 3.0f;
        public ActiveTNT(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }

        public void reset(float x, float y, float z) {
            this.x = x; this.y = y; this.z = z;
            this.vx = 0f; this.vy = 0f; this.vz = 0f;
            this.timer = 3.0f;
        }
    }

    public static void main(String[] args) {
        int iterations = 100000;
        int spawnsPerIteration = 10;
        float dt = 0.05f;

        // Warm up
        for(int i=0; i<10; i++) {
            benchmarkBaseline(1000, 10, dt, false);
            benchmarkOptimized(1000, 10, dt, false);
        }

        benchmarkBaseline(iterations, spawnsPerIteration, dt, true);
        benchmarkOptimized(iterations, spawnsPerIteration, dt, true);
    }

    private static void benchmarkBaseline(int iterations, int spawnsPerIteration, float dt, boolean print) {
        ArrayList<ActiveTNT> tickingTNTs = new ArrayList<>();
        Random random = new Random(42);

        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            // Spawn
            for (int j = 0; j < spawnsPerIteration; j++) {
                tickingTNTs.add(new ActiveTNT(10, 100, 10));
            }

            // Update
            for (int k = tickingTNTs.size() - 1; k >= 0; k--) {
                ActiveTNT tnt = tickingTNTs.get(k);
                if (tnt == null) {
                    int lastIdx = tickingTNTs.size() - 1;
                    if (k < lastIdx) tickingTNTs.set(k, tickingTNTs.get(lastIdx));
                    tickingTNTs.remove(lastIdx);
                    continue;
                }
                tnt.timer -= dt;
                tnt.vy -= 10.0f * dt;
                tnt.y += tnt.vy * dt;

                if (tnt.timer <= 0) {
                    int lastIdx = tickingTNTs.size() - 1;
                    if (k < lastIdx) tickingTNTs.set(k, tickingTNTs.get(lastIdx));
                    tickingTNTs.remove(lastIdx);
                }
            }
        }
        long endTime = System.nanoTime();
        if (print) System.out.println("Baseline: " + (endTime - startTime) / 1000000.0 + " ms");
    }

    private static void benchmarkOptimized(int iterations, int spawnsPerIteration, float dt, boolean print) {
        ArrayList<ActiveTNT> tickingTNTs = new ArrayList<>();
        ArrayList<ActiveTNT> pool = new ArrayList<>();
        Random random = new Random(42);

        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            // Spawn from pool
            for (int j = 0; j < spawnsPerIteration; j++) {
                ActiveTNT tnt;
                if (!pool.isEmpty()) {
                    tnt = pool.remove(pool.size() - 1);
                    tnt.reset(10, 100, 10);
                } else {
                    tnt = new ActiveTNT(10, 100, 10);
                }
                tickingTNTs.add(tnt);
            }

            // Update
            for (int k = tickingTNTs.size() - 1; k >= 0; k--) {
                ActiveTNT tnt = tickingTNTs.get(k);
                // No null check
                tnt.timer -= dt;
                tnt.vy -= 10.0f * dt;
                tnt.y += tnt.vy * dt;

                if (tnt.timer <= 0) {
                    int lastIdx = tickingTNTs.size() - 1;
                    ActiveTNT last = tickingTNTs.get(lastIdx);
                    if (k < lastIdx) {
                        tickingTNTs.set(k, last);
                    }
                    tickingTNTs.remove(lastIdx);
                    pool.add(tnt); // Return to pool
                }
            }
        }
        long endTime = System.nanoTime();
        if (print) System.out.println("Optimized: " + (endTime - startTime) / 1000000.0 + " ms");
    }
}
