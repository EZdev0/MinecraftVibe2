package com.EZdev.mc2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class NoiseTest {

    @Test
    public void testSimplex3Consistency() {
        float x = 0.123f;
        float y = 0.456f;
        float z = 0.789f;
        float result1 = Noise.simplex3(x, y, z);
        float result2 = Noise.simplex3(x, y, z);
        assertEquals("simplex3 should be consistent", result1, result2, 0.0f);
    }

    @Test
    public void testSimplex3Range() {
        for (float x = -10f; x <= 10f; x += 0.5f) {
            for (float y = -10f; y <= 10f; y += 0.5f) {
                for (float z = -10f; z <= 10f; z += 0.5f) {
                    float val = Noise.simplex3(x, y, z);
                    assertTrue("Value " + val + " at (" + x + "," + y + "," + z + ") should be >= -1", val >= -1.0f);
                    assertTrue("Value " + val + " at (" + x + "," + y + "," + z + ") should be <= 1", val <= 1.0f);
                }
            }
        }
    }

    @Test
    public void testSimplex2Relationship() {
        float x = 0.5f;
        float y = 0.5f;
        float s2 = Noise.simplex2(x, y);
        float s3 = Noise.simplex3(x, y, 0);
        assertEquals("simplex2(x, y) should be equal to simplex3(x, y, 0)", s2, s3, 0.0f);
    }

    @Test
    public void testSimplex3IntegerBoundaries() {
        // Just ensure it doesn't crash and returns something reasonable
        float val = Noise.simplex3(1.0f, 2.0f, 3.0f);
        assertTrue(val >= -1.0f && val <= 1.0f);
    }
}
