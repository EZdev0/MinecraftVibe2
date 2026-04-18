package com.EZdev.mc2;

import org.junit.Test;

public class PerformanceBenchmarkTest {

    @Test
    public void benchmarkSlotSearch() {
        byte[] blockIds = {1, 2, 3, 4, 5, 6};
        byte activeBlock = 6; // Worst case: last element
        int iterations = 10000000;

        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            int slot = -1;
            for (int s = 0; s < blockIds.length; s++) {
                if (blockIds[s] == activeBlock) {
                    slot = s;
                }
            }
        }
        long endTime = System.nanoTime();
        System.out.println("Baseline Search Time: " + (endTime - startTime) / 1000000.0 + " ms");
    }

    @Test
    public void benchmarkDirectLookup() {
        int activeSlot = 5;
        int iterations = 10000000;

        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            int slot = activeSlot;
        }
        long endTime = System.nanoTime();
        System.out.println("Optimized Lookup Time: " + (endTime - startTime) / 1000000.0 + " ms");
    }
}
