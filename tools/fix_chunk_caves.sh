#!/bin/bash
cat << 'INNER_EOF' > Minecraft2Vibe/Minecraft2Vibe/app/src/main/java/com/EZdev/mc2/Chunk.java
package com.EZdev.mc2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Chunk {
    public int chunkX, chunkZ;
    public byte[][][] blocks = new byte[16][128][16];
    private WorldLogic world;
    public FloatBuffer vertexBuffer, colorBuffer;
    public int vertexCount = 0;

    private int bufferCapacity = 0;

    public Chunk(WorldLogic world, int cx, int cz) {
        this.world = world; this.chunkX = cx; this.chunkZ = cz;
        // Check if we have loaded data first (handled by SaveManager later)
        generateTerrain(); addDecorations(); buildMesh();
    }

    private void generateTerrain() {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int gx = (chunkX * 16) + x, gz = (chunkZ * 16) + z;
                float noiseVal = Noise.simplex2(gx * 0.015f, gz * 0.015f);
                int height = 55 + (int)(noiseVal * 25f);
                for (int y = 0; y <= height; y++) {
                    if (y == 0) { blocks[x][y][z] = 2; continue; } // Bedrock

                    // Feature 4: Better Cave Generation using 3D Noise Carvers
                    // Real Minecraft uses a mix of "cheese" noise (large open caves) and "spaghetti" noise (tunnels).
                    float worm1 = Noise.simplex3(gx * 0.04f, y * 0.04f, gz * 0.04f);
                    float worm2 = Noise.simplex3(gx * 0.05f + 100, y * 0.05f + 100, gz * 0.05f + 100);
                    float thickness = 0.03f + ((128f - y) / 128f) * 0.05f; // Thicker deeper down

                    // Spaghetti tunnels: intersection of two 3D noise planes
                    boolean isTunnel = Math.abs(worm1) < thickness && Math.abs(worm2) < thickness;

                    // Cheese caves (large rooms): Single low-frequency noise threshold
                    float room = Noise.simplex3(gx * 0.015f, y * 0.02f, gz * 0.015f);
                    boolean isRoom = room > 0.6f;

                    // Don't generate caves too close to the surface
                    if (! (y > height - 5) && (isTunnel || isRoom)) {
                        blocks[x][y][z] = 0;
                    } else {
                        if (y == height) blocks[x][y][z] = 1;
                        else if (y > height - 4) blocks[x][y][z] = 1;
                        else blocks[x][y][z] = 2;
                    }
                }

                for (int y = 50; y > height; y--) {
                    blocks[x][y][z] = 7;
                }
                if (height < 50 && blocks[x][height][z] == 1) {
                    blocks[x][height][z] = 1;
                }
            }
        }
    }

    private void addDecorations() {
        for (int x = 2; x < 14; x++) {
            for (int z = 2; z < 14; z++) {
                for (int y = 100; y > 50; y--) {
                    if (blocks[x][y][z] == 1 && blocks[x][y+1][z] == 0) {
                        if (Math.random() < 0.02) {
                            for(int h=1; h<=4; h++) blocks[x][y+h][z] = 3;
                            for(int lx=x-2; lx<=x+2; lx++) for(int ly=y+3; ly<=y+5; ly++) for(int lz=z-2; lz<=z+2; lz++)
                                if (blocks[lx][ly][lz] == 0 && Math.random() < 0.8) blocks[lx][ly][lz] = 4;
                        }
                        break;
                    }
                }
            }
        }
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 5; y < 40; y++)
            if (blocks[x][y][z] == 0 && blocks[x][y+1][z] == 2 && Math.random() < 0.05) blocks[x][y][z] = 2;
    }

    private byte getBlockWorldAware(int lx, int ly, int lz) {
        if (ly < 0 || ly >= 128) return 0;
        if (lx >= 0 && lx < 16 && lz >= 0 && lz < 16) return blocks[lx][ly][lz];
        return world.getBlock((chunkX * 16) + lx, ly, (chunkZ * 16) + lz);
    }

    private boolean isTransparent(int x, int y, int z, byte sourceBlockType) {
        byte b = getBlockWorldAware(x, y, z);
        if (b == 0 || b == 6) return true;
        if (sourceBlockType != 7 && b == 7) return true;
        return false;
    }

    public void buildMesh() {
        int MAX_POSSIBLE_VERTS = 16*128*16 * 12;
        float[] vData = new float[MAX_POSSIBLE_VERTS * 3];
        float[] cData = new float[MAX_POSSIBLE_VERTS * 4];
        vertexCount = 0;

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 128; y++) {
                for (int z = 0; z < 16; z++) {
                    byte type = blocks[x][y][z];
                    if (type == 0) continue;

                    float r = 1f, g = 1f, b = 1f, a = 1f;
                    if (type == 1) { r = 0.3f; g = 0.7f; b = 0.2f; }
                    else if (type == 2) { r = 0.4f; g = 0.4f; b = 0.4f; }
                    else if (type == 3) { r = 0.4f; g = 0.25f; b = 0.1f; }
                    else if (type == 4) { r = 0.1f; g = 0.5f; b = 0.1f; }
                    else if (type == 5) { r = 0.9f; g = 0.2f; b = 0.2f; }
                    else if (type == 7) { r = 0.2f; g = 0.4f; b = 0.9f; a = 0.7f; }

                    if (type == 1 && y < 127 && blocks[x][y+1][z] != 0 && blocks[x][y+1][z] != 7) {
                        r = 0.4f; g = 0.25f; b = 0.1f;
                    }

                    if (type == 6) { addFireCross(vData, cData, x, y, z, 0.9f, 0.5f, 0.1f); continue; }

                    if (isTransparent(x, y+1, z, type)) addFace(vData, cData, x, y, z, 0, r, g, b, a);
                    if (isTransparent(x, y-1, z, type)) addFace(vData, cData, x, y, z, 1, r*0.5f, g*0.5f, b*0.5f, a);
                    if (isTransparent(x-1, y, z, type)) addFace(vData, cData, x, y, z, 2, r*0.8f, g*0.8f, b*0.8f, a);
                    if (isTransparent(x+1, y, z, type)) addFace(vData, cData, x, y, z, 3, r*0.8f, g*0.8f, b*0.8f, a);
                    if (isTransparent(x, y, z-1, type)) addFace(vData, cData, x, y, z, 4, r*0.9f, g*0.9f, b*0.9f, a);
                    if (isTransparent(x, y, z+1, type)) addFace(vData, cData, x, y, z, 5, r*0.9f, g*0.9f, b*0.9f, a);

                    if (type == 5) {
                        if (isTransparent(x, y, z-1, type)) drawLetterT(vData, cData, x, y, z, 4);
                        if (isTransparent(x, y, z+1, type)) drawLetterT(vData, cData, x, y, z, 5);
                        if (isTransparent(x-1, y, z, type)) drawLetterT(vData, cData, x, y, z, 2);
                        if (isTransparent(x+1, y, z, type)) drawLetterT(vData, cData, x, y, z, 3);
                    }
                }
            }
        }

        if (vertexBuffer == null || vertexCount > bufferCapacity) {
            bufferCapacity = vertexCount + 1000;
            vertexBuffer = ByteBuffer.allocateDirect(bufferCapacity * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            colorBuffer = ByteBuffer.allocateDirect(bufferCapacity * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        }

        vertexBuffer.clear();
        vertexBuffer.put(vData, 0, vertexCount * 3).position(0);

        colorBuffer.clear();
        colorBuffer.put(cData, 0, vertexCount * 4).position(0);
    }

    private void addFace(float[] v, float[] c, float x, float y, float z, int s, float r, float g, float b, float a) {
        if(vertexCount >= (v.length/3) - 6) return;
        float[] fs = null;
        switch(s) {
            case 0: fs = new float[]{x,y+1,z, x,y+1,z+1, x+1,y+1,z+1, x,y+1,z, x+1,y+1,z+1, x+1,y+1,z}; break;
            case 1: fs = new float[]{x,y,z+1, x,y,z, x+1,y,z, x,y,z+1, x+1,y,z, x+1,y,z+1}; break;
            case 2: fs = new float[]{x,y+1,z, x,y,z, x,y,z+1, x,y+1,z, x,y,z+1, x,y+1,z+1}; break;
            case 3: fs = new float[]{x+1,y+1,z+1, x+1,y,z+1, x+1,y,z, x+1,y+1,z+1, x+1,y,z, x+1,y+1,z}; break;
            case 4: fs = new float[]{x+1,y+1,z, x+1,y,z, x,y,z, x+1,y+1,z, x,y,z, x,y+1,z}; break;
            case 5: fs = new float[]{x,y+1,z+1, x,y,z+1, x+1,y,z+1, x,y+1,z+1, x+1,y,z+1, x+1,y+1,z+1}; break;
        }
        for(int i=0; i<18; i++) v[vertexCount*3 + i] = fs[i];
        for(int i=0; i<6; i++) { c[vertexCount*4 + i*4] = r; c[vertexCount*4 + i*4+1] = g; c[vertexCount*4 + i*4+2] = b; c[vertexCount*4 + i*4+3] = a; }
        vertexCount += 6;
    }

    private void addFireCross(float[] v, float[] c, float x, float y, float z, float r, float g, float b) {
        float[][] ps = {{x,y,z,1f}, {x+1,y,z+1,1f}, {x+1,y+1,z+1,0f}, {x,y+1,z,0f}, {x+1,y,z,1f}, {x,y,z+1,1f}, {x,y+1,z+1,0f}, {x+1,y+1,z,0f}};
        addQuad(v, c, ps[0], ps[1], ps[2], ps[3], r, g, b); addQuad(v, c, ps[1], ps[0], ps[3], ps[2], r, g, b);
        addQuad(v, c, ps[4], ps[5], ps[6], ps[7], r, g, b); addQuad(v, c, ps[5], ps[4], ps[7], ps[6], r, g, b);
    }

    private void addQuad(float[] v, float[] c, float[] v1, float[] v2, float[] v3, float[] v4, float r, float g, float b) {
        if(vertexCount >= (v.length/3) - 6) return;
        float[][] ts = {v1, v2, v3, v1, v3, v4};
        for (int i=0; i<6; i++) {
            v[vertexCount*3 + i*3] = ts[i][0]; v[vertexCount*3 + i*3+1] = ts[i][1]; v[vertexCount*3 + i*3+2] = ts[i][2];
            c[vertexCount*4 + i*4] = r; c[vertexCount*4 + i*4+1] = g; c[vertexCount*4 + i*4+2] = b; c[vertexCount*4 + i*4+3] = ts[i][3];
        }
        vertexCount += 6;
    }

    private void drawLetterT(float[] v, float[] c, float x, float y, float z, int s) {
        if(vertexCount >= (v.length/3) - 12) return;
        float e = 0.01f; float[][] rs = new float[2][18];
        if (s == 4) { rs[0] = new float[]{x+0.8f,y+0.8f,z-e, x+0.8f,y+0.65f,z-e, x+0.2f,y+0.65f,z-e, x+0.8f,y+0.8f,z-e, x+0.2f,y+0.65f,z-e, x+0.2f,y+0.8f,z-e};
                     rs[1] = new float[]{x+0.6f,y+0.65f,z-e, x+0.6f,y+0.2f,z-e, x+0.4f,y+0.2f,z-e, x+0.6f,y+0.65f,z-e, x+0.4f,y+0.2f,z-e, x+0.4f,y+0.65f,z-e}; }
        else if (s == 5) { rs[0] = new float[]{x+0.2f,y+0.8f,z+1+e, x+0.2f,y+0.65f,z+1+e, x+0.8f,y+0.65f,z+1+e, x+0.2f,y+0.8f,z+1+e, x+0.8f,y+0.65f,z+1+e, x+0.8f,y+0.8f,z+1+e};
                         rs[1] = new float[]{x+0.4f,y+0.65f,z+1+e, x+0.4f,y+0.2f,z+1+e, x+0.6f,y+0.2f,z+1+e, x+0.4f,y+0.65f,z+1+e, x+0.6f,y+0.2f,z+1+e, x+0.6f,y+0.65f,z+1+e}; }
        else if (s == 2) { rs[0] = new float[]{x-e,y+0.8f,z+0.2f, x-e,y+0.65f,z+0.2f, x-e,y+0.65f,z+0.8f, x-e,y+0.8f,z+0.2f, x-e,y+0.65f,z+0.8f, x-e,y+0.8f,z+0.8f};
                         rs[1] = new float[]{x-e,y+0.65f,z+0.4f, x-e,y+0.2f,z+0.4f, x-e,y+0.2f,z+0.6f, x-e,y+0.65f,z+0.4f, x-e,y+0.2f,z+0.6f, x-e,y+0.65f,z+0.6f}; }
        else if (s == 3) { rs[0] = new float[]{x+1+e,y+0.8f,z+0.8f, x+1+e,y+0.65f,z+0.8f, x+1+e,y+0.65f,z+0.2f, x+1+e,y+0.8f,z+0.8f, x+1+e,y+0.65f,z+0.2f, x+1+e,y+0.8f,z+0.2f};
                         rs[1] = new float[]{x+1+e,y+0.65f,z+0.6f, x+1+e,y+0.2f,z+0.6f, x+1+e,y+0.2f,z+0.4f, x+1+e,y+0.65f,z+0.6f, x+1+e,y+0.2f,z+0.4f, x+1+e,y+0.65f,z+0.4f}; }
        for (int r = 0; r < 2; r++) { if (rs[r] == null) continue; for(int i=0; i<18; i++) v[vertexCount*3 + i] = rs[r][i];
            for(int i=0; i<6; i++) { c[vertexCount*4 + i*4] = 0f; c[vertexCount*4 + i*4+1] = 0f; c[vertexCount*4 + i*4+2] = 0f; c[vertexCount*4 + i*4+3] = 1.0f; } vertexCount += 6; }
    }
}
INNER_EOF
