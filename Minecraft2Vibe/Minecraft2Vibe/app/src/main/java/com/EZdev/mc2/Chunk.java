package com.EZdev.mc2;

import java.util.Random;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Chunk {
    private final Random random;
    public int chunkX, chunkZ;
    public byte[][][] blocks = new byte[16][128][16];
    private WorldLogic world;
    public FloatBuffer vertexBuffer, colorBuffer;
    public int vertexCount = 0;

    private static final int MAX_POSSIBLE_VERTS = 16 * 128 * 16 * 12;
    private static final float[] sharedVData = new float[MAX_POSSIBLE_VERTS * 3];
    private static final float[] sharedCData = new float[MAX_POSSIBLE_VERTS * 4];
    private static final Object meshLock = new Object();

    private int bufferCapacity = 0;

    public Chunk(WorldLogic world, int cx, int cz) {
        this.world = world; this.chunkX = cx; this.chunkZ = cz;
        long chunkSeed = world.worldSeed ^ ((long) cx << 32) ^ (long) cz;
        this.random = new Random(chunkSeed);

        boolean loaded = false;
        if (world.saveManager != null) {
            loaded = world.saveManager.loadChunk(this);
        }

        if (!loaded) {
            generateTerrain();
            addDecorations();
            if(world.saveManager != null) world.saveManager.saveChunk(this);
        }
        buildMesh();
    }


    private void generateTerrain() {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int gx = (chunkX * 16) + x, gz = (chunkZ * 16) + z;
                // Base terrain height with FBM for smoother hills
                float noiseVal = Noise.fbm2(gx * 0.005f, gz * 0.005f, 4);
                // Normalized FBM is between -1 and 1, adjust to height
                int height = 70 + (int)(noiseVal * 30f);

                for (int y = 0; y <= height; y++) {
                    if (y == 0) { blocks[x][y][z] = Blocks.BEDROCK; continue; } // Bedrock
                    if (y <= 2 && random.nextDouble() < 0.5) { blocks[x][y][z] = Blocks.BEDROCK; continue; } // Bedrock noise

                    // Base block types
                    if (y == height) blocks[x][y][z] = Blocks.GRASS; // Grass
                    else if (y > height - 4) blocks[x][y][z] = Blocks.DIRT; // Dirt
                    else blocks[x][y][z] = Blocks.STONE; // Stone
                    if (y < 20 && random.nextFloat() < 0.005f) blocks[x][y][z] = Blocks.TNT; // TNT

                    // Cave generation (only dig below surface)
                    if (y < height - 5 && y > 2) {
                        float caveX = gx * 0.015f;
                        float caveY = y * 0.015f;
                        float caveZ = gz * 0.015f;

                        // 1. Cheese Caves (Large hollow areas)
                        float cheeseNoise = Noise.fbm3(caveX, caveY, caveZ, 2);
                        boolean isCheese = cheeseNoise > 0.45f;

                        // 2. Spaghetti Caves (Long, winding tunnels)
                        // Use intersection of two FBMs
                        float spaghetti1 = Noise.fbm3(caveX * 1.5f, caveY * 1.5f, caveZ * 1.5f, 2);
                        float spaghetti2 = Noise.fbm3(caveX * 1.5f + 100f, caveY * 1.5f + 100f, caveZ * 1.5f + 100f, 2);
                        boolean isSpaghetti = Math.abs(spaghetti1) < 0.06f && Math.abs(spaghetti2) < 0.06f;

                        // 3. Noodle Caves (Smaller, more frequent tunnels)
                        float noodle1 = Noise.fbm3(caveX * 3.0f, caveY * 3.0f, caveZ * 3.0f, 1);
                        float noodle2 = Noise.fbm3(caveX * 3.0f + 50f, caveY * 3.0f + 50f, caveZ * 3.0f + 50f, 1);
                        boolean isNoodle = Math.abs(noodle1) < 0.04f && Math.abs(noodle2) < 0.04f;

                        // 4. Ravines (Tall, narrow vertical cuts)
                        // Squash Y axis heavily so the noise stretches vertically
                        float ravineNoise = Noise.fbm3(gx * 0.01f, y * 0.002f, gz * 0.01f, 2);
                        // Mask the ravine so it doesn't appear everywhere
                        float ravineMask = Noise.simplex2(gx * 0.005f + 1000f, gz * 0.005f + 1000f);
                        boolean isRavine = Math.abs(ravineNoise) < 0.05f && ravineMask > 0.3f;

                        if (isCheese || isSpaghetti || isNoodle || isRavine) {
                            blocks[x][y][z] = Blocks.AIR; // Dig air

                            // Fill bottom of caves with water/lava
                            if (y < 12) blocks[x][y][z] = Blocks.WATER; // Water
                        }
                    }
                }

                // Water fill for oceans/lakes
                for (int y = 60; y > height; y--) {
                    blocks[x][y][z] = Blocks.WATER;
                }
                // Convert grass under water to dirt
                if (height < 60 && blocks[x][height][z] == Blocks.GRASS) {
                    blocks[x][height][z] = Blocks.DIRT; // It's already 1, but this logic was in original
                }
            }
        }
    }
private void addDecorations() {
        for (int x = 2; x < 14; x++) {
            for (int z = 2; z < 14; z++) {
                for (int y = 100; y > 50; y--) {
                    if (blocks[x][y][z] == Blocks.GRASS && blocks[x][y+1][z] == Blocks.AIR) {
                        if (random.nextDouble() < 0.02) {
                            for(int h=1; h<=4; h++) blocks[x][y+h][z] = Blocks.WOOD;
                            for(int lx=x-2; lx<=x+2; lx++) for(int ly=y+3; ly<=y+5; ly++) for(int lz=z-2; lz<=z+2; lz++)
                                if (blocks[lx][ly][lz] == Blocks.AIR && random.nextDouble() < 0.8) blocks[lx][ly][lz] = Blocks.LEAVES;
                        }
                        break;
                    }
                }
            }
        }
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 5; y < 40; y++)
            if (blocks[x][y][z] == Blocks.AIR && blocks[x][y+1][z] == Blocks.STONE && random.nextDouble() < 0.05) blocks[x][y][z] = Blocks.STONE;
    }

    private byte getBlockWorldAware(int lx, int ly, int lz) {
        if (ly < 0 || ly >= 128) return Blocks.AIR;
        if (lx >= 0 && lx < 16 && lz >= 0 && lz < 16) return blocks[lx][ly][lz];
        return world.getBlock((chunkX * 16) + lx, ly, (chunkZ * 16) + lz);
    }

    private boolean isTransparent(int x, int y, int z, byte sourceBlockType) {
        byte b = getBlockWorldAware(x, y, z);
        if (b == Blocks.AIR || b == Blocks.FIRE) return true;
        if (sourceBlockType != Blocks.WATER && b == Blocks.WATER) return true;
        return false;
    }

    public void buildMesh() {
        synchronized (meshLock) {
            vertexCount = 0;

            for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 128; y++) {
                for (int z = 0; z < 16; z++) {
                    byte type = blocks[x][y][z];
                    if (type == Blocks.AIR) continue;

                    float r = 1f, g = 1f, b = 1f, a = 1f;
                    if (type == Blocks.GRASS) { r = 0.3f; g = 0.7f; b = 0.2f; }
                    else if (type == Blocks.DIRT) { r = 0.4f; g = 0.25f; b = 0.1f; }
                    else if (type == Blocks.STONE) { r = 0.4f; g = 0.4f; b = 0.4f; }
                    else if (type == Blocks.WOOD) { r = 0.4f; g = 0.25f; b = 0.1f; }
                    else if (type == Blocks.LEAVES) { r = 0.1f; g = 0.5f; b = 0.1f; }
                    else if (type == Blocks.TNT) { r = 0.9f; g = 0.2f; b = 0.2f; }
                    else if (type == Blocks.WATER) { r = 0.2f; g = 0.4f; b = 0.9f; a = 0.7f; }
                    else if (type == Blocks.BEDROCK) { r = 0.1f; g = 0.1f; b = 0.1f; } // Bedrock (very dark grey)

                    if (type == Blocks.GRASS && y < 127 && blocks[x][y+1][z] != Blocks.AIR && blocks[x][y+1][z] != Blocks.WATER) {
                        r = 0.4f; g = 0.25f; b = 0.1f;
                    }

                    if (type == Blocks.FIRE) { addFireCross(sharedVData, sharedCData, x, y, z, 0.9f, 0.5f, 0.1f); continue; }

                    if (isTransparent(x, y+1, z, type)) addFace(sharedVData, sharedCData, x, y, z, 0, r, g, b, a);
                    if (isTransparent(x, y-1, z, type)) addFace(sharedVData, sharedCData, x, y, z, 1, r*0.5f, g*0.5f, b*0.5f, a);
                    if (isTransparent(x-1, y, z, type)) addFace(sharedVData, sharedCData, x, y, z, 2, r*0.8f, g*0.8f, b*0.8f, a);
                    if (isTransparent(x+1, y, z, type)) addFace(sharedVData, sharedCData, x, y, z, 3, r*0.8f, g*0.8f, b*0.8f, a);
                    if (isTransparent(x, y, z-1, type)) addFace(sharedVData, sharedCData, x, y, z, 4, r*0.9f, g*0.9f, b*0.9f, a);
                    if (isTransparent(x, y, z+1, type)) addFace(sharedVData, sharedCData, x, y, z, 5, r*0.9f, g*0.9f, b*0.9f, a);

                    if (type == Blocks.TNT) {
                        if (isTransparent(x, y, z-1, type)) drawLetterT(sharedVData, sharedCData, x, y, z, 4);
                        if (isTransparent(x, y, z+1, type)) drawLetterT(sharedVData, sharedCData, x, y, z, 5);
                        if (isTransparent(x-1, y, z, type)) drawLetterT(sharedVData, sharedCData, x, y, z, 2);
                        if (isTransparent(x+1, y, z, type)) drawLetterT(sharedVData, sharedCData, x, y, z, 3);
                    }
                }
            }
        }

            if (vertexBuffer == null || vertexCount > bufferCapacity) {
                bufferCapacity = vertexCount + 1000;
                vertexBuffer = ByteBuffer.allocateDirect(bufferCapacity * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
                colorBuffer = ByteBuffer.allocateDirect(bufferCapacity * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            }

            synchronized(this) {
                vertexBuffer.clear();
                vertexBuffer.put(sharedVData, 0, vertexCount * 3).position(0);

                colorBuffer.clear();
                colorBuffer.put(sharedCData, 0, vertexCount * 4).position(0);

            }
        }
    }

    private void addFace(float[] v, float[] c, float x, float y, float z, int s, float r, float g, float b, float a) {
        if(vertexCount >= (v.length/3) - 6) return;
        int vc = vertexCount * 3;
        switch(s) {
            case 0:
                v[vc]=x; v[vc+1]=y+1; v[vc+2]=z;
                v[vc+3]=x; v[vc+4]=y+1; v[vc+5]=z+1;
                v[vc+6]=x+1; v[vc+7]=y+1; v[vc+8]=z+1;
                v[vc+9]=x; v[vc+10]=y+1; v[vc+11]=z;
                v[vc+12]=x+1; v[vc+13]=y+1; v[vc+14]=z+1;
                v[vc+15]=x+1; v[vc+16]=y+1; v[vc+17]=z;
                break;
            case 1:
                v[vc]=x; v[vc+1]=y; v[vc+2]=z+1;
                v[vc+3]=x; v[vc+4]=y; v[vc+5]=z;
                v[vc+6]=x+1; v[vc+7]=y; v[vc+8]=z;
                v[vc+9]=x; v[vc+10]=y; v[vc+11]=z+1;
                v[vc+12]=x+1; v[vc+13]=y; v[vc+14]=z;
                v[vc+15]=x+1; v[vc+16]=y; v[vc+17]=z+1;
                break;
            case 2:
                v[vc]=x; v[vc+1]=y+1; v[vc+2]=z;
                v[vc+3]=x; v[vc+4]=y; v[vc+5]=z;
                v[vc+6]=x; v[vc+7]=y; v[vc+8]=z+1;
                v[vc+9]=x; v[vc+10]=y+1; v[vc+11]=z;
                v[vc+12]=x; v[vc+13]=y; v[vc+14]=z+1;
                v[vc+15]=x; v[vc+16]=y+1; v[vc+17]=z+1;
                break;
            case 3:
                v[vc]=x+1; v[vc+1]=y+1; v[vc+2]=z+1;
                v[vc+3]=x+1; v[vc+4]=y; v[vc+5]=z+1;
                v[vc+6]=x+1; v[vc+7]=y; v[vc+8]=z;
                v[vc+9]=x+1; v[vc+10]=y+1; v[vc+11]=z+1;
                v[vc+12]=x+1; v[vc+13]=y; v[vc+14]=z;
                v[vc+15]=x+1; v[vc+16]=y+1; v[vc+17]=z;
                break;
            case 4:
                v[vc]=x+1; v[vc+1]=y+1; v[vc+2]=z;
                v[vc+3]=x+1; v[vc+4]=y; v[vc+5]=z;
                v[vc+6]=x; v[vc+7]=y; v[vc+8]=z;
                v[vc+9]=x+1; v[vc+10]=y+1; v[vc+11]=z;
                v[vc+12]=x; v[vc+13]=y; v[vc+14]=z;
                v[vc+15]=x; v[vc+16]=y+1; v[vc+17]=z;
                break;
            case 5:
                v[vc]=x; v[vc+1]=y+1; v[vc+2]=z+1;
                v[vc+3]=x; v[vc+4]=y; v[vc+5]=z+1;
                v[vc+6]=x+1; v[vc+7]=y; v[vc+8]=z+1;
                v[vc+9]=x; v[vc+10]=y+1; v[vc+11]=z+1;
                v[vc+12]=x+1; v[vc+13]=y; v[vc+14]=z+1;
                v[vc+15]=x+1; v[vc+16]=y+1; v[vc+17]=z+1;
                break;
        }
        int cc = vertexCount * 4;
        c[cc] = r; c[cc + 1] = g; c[cc + 2] = b; c[cc + 3] = a;
        c[cc + 4] = r; c[cc + 5] = g; c[cc + 6] = b; c[cc + 7] = a;
        c[cc + 8] = r; c[cc + 9] = g; c[cc + 10] = b; c[cc + 11] = a;
        c[cc + 12] = r; c[cc + 13] = g; c[cc + 14] = b; c[cc + 15] = a;
        c[cc + 16] = r; c[cc + 17] = g; c[cc + 18] = b; c[cc + 19] = a;
        c[cc + 20] = r; c[cc + 21] = g; c[cc + 22] = b; c[cc + 23] = a;
        vertexCount += 6;
    }

    private void addFireCross(float[] v, float[] c, float x, float y, float z, float r, float g, float b) {
        // ps[0] = x,y,z,1f  | ps[1] = x+1,y,z+1,1f | ps[2] = x+1,y+1,z+1,0f | ps[3] = x,y+1,z,0f
        addQuad(v, c, x,y,z,1f, x+1,y,z+1,1f, x+1,y+1,z+1,0f, x,y+1,z,0f, r, g, b);
        addQuad(v, c, x+1,y,z+1,1f, x,y,z,1f, x,y+1,z,0f, x+1,y+1,z+1,0f, r, g, b);
        // ps[4] = x+1,y,z,1f | ps[5] = x,y,z+1,1f | ps[6] = x,y+1,z+1,0f | ps[7] = x+1,y+1,z,0f
        addQuad(v, c, x+1,y,z,1f, x,y,z+1,1f, x,y+1,z+1,0f, x+1,y+1,z,0f, r, g, b);
        addQuad(v, c, x,y,z+1,1f, x+1,y,z,1f, x+1,y+1,z,0f, x,y+1,z+1,0f, r, g, b);
    }

    private void addQuad(float[] v, float[] c, float x1, float y1, float z1, float a1,
                         float x2, float y2, float z2, float a2,
                         float x3, float y3, float z3, float a3,
                         float x4, float y4, float z4, float a4, float r, float g, float b) {
        if(vertexCount >= (v.length/3) - 6) return;
        int vc = vertexCount * 3;
        v[vc]=x1; v[vc+1]=y1; v[vc+2]=z1;
        v[vc+3]=x2; v[vc+4]=y2; v[vc+5]=z2;
        v[vc+6]=x3; v[vc+7]=y3; v[vc+8]=z3;
        v[vc+9]=x1; v[vc+10]=y1; v[vc+11]=z1;
        v[vc+12]=x3; v[vc+13]=y3; v[vc+14]=z3;
        v[vc+15]=x4; v[vc+16]=y4; v[vc+17]=z4;

        int cc = vertexCount * 4;
        c[cc]=r; c[cc+1]=g; c[cc+2]=b; c[cc+3]=a1;
        c[cc+4]=r; c[cc+5]=g; c[cc+6]=b; c[cc+7]=a2;
        c[cc+8]=r; c[cc+9]=g; c[cc+10]=b; c[cc+11]=a3;
        c[cc+12]=r; c[cc+13]=g; c[cc+14]=b; c[cc+15]=a1;
        c[cc+16]=r; c[cc+17]=g; c[cc+18]=b; c[cc+19]=a3;
        c[cc+20]=r; c[cc+21]=g; c[cc+22]=b; c[cc+23]=a4;
        vertexCount += 6;
    }

    // Memory-friendly T draw using simple local assignment instead of new float[]
    private void drawLetterT(float[] v, float[] c, float x, float y, float z, int s) {
        if(vertexCount >= (v.length/3) - 12) return;
        float e = 0.01f;
        int vc = vertexCount * 3;
        if (s == 4) {
            v[vc]=x+0.8f; v[vc+1]=y+0.8f; v[vc+2]=z-e; v[vc+3]=x+0.8f; v[vc+4]=y+0.65f; v[vc+5]=z-e; v[vc+6]=x+0.2f; v[vc+7]=y+0.65f; v[vc+8]=z-e; v[vc+9]=x+0.8f; v[vc+10]=y+0.8f; v[vc+11]=z-e; v[vc+12]=x+0.2f; v[vc+13]=y+0.65f; v[vc+14]=z-e; v[vc+15]=x+0.2f; v[vc+16]=y+0.8f; v[vc+17]=z-e;
            int cc = vertexCount * 4;
            c[cc] = 0f; c[cc + 1] = 0f; c[cc + 2] = 0f; c[cc + 3] = 1.0f;
            c[cc + 4] = 0f; c[cc + 5] = 0f; c[cc + 6] = 0f; c[cc + 7] = 1.0f;
            c[cc + 8] = 0f; c[cc + 9] = 0f; c[cc + 10] = 0f; c[cc + 11] = 1.0f;
            c[cc + 12] = 0f; c[cc + 13] = 0f; c[cc + 14] = 0f; c[cc + 15] = 1.0f;
            c[cc + 16] = 0f; c[cc + 17] = 0f; c[cc + 18] = 0f; c[cc + 19] = 1.0f;
            c[cc + 20] = 0f; c[cc + 21] = 0f; c[cc + 22] = 0f; c[cc + 23] = 1.0f;
            vertexCount += 6; vc = vertexCount * 3;
            v[vc]=x+0.6f; v[vc+1]=y+0.65f; v[vc+2]=z-e; v[vc+3]=x+0.6f; v[vc+4]=y+0.2f; v[vc+5]=z-e; v[vc+6]=x+0.4f; v[vc+7]=y+0.2f; v[vc+8]=z-e; v[vc+9]=x+0.6f; v[vc+10]=y+0.65f; v[vc+11]=z-e; v[vc+12]=x+0.4f; v[vc+13]=y+0.2f; v[vc+14]=z-e; v[vc+15]=x+0.4f; v[vc+16]=y+0.65f; v[vc+17]=z-e;
            cc = vertexCount * 4;
            c[cc] = 0f; c[cc + 1] = 0f; c[cc + 2] = 0f; c[cc + 3] = 1.0f;
            c[cc + 4] = 0f; c[cc + 5] = 0f; c[cc + 6] = 0f; c[cc + 7] = 1.0f;
            c[cc + 8] = 0f; c[cc + 9] = 0f; c[cc + 10] = 0f; c[cc + 11] = 1.0f;
            c[cc + 12] = 0f; c[cc + 13] = 0f; c[cc + 14] = 0f; c[cc + 15] = 1.0f;
            c[cc + 16] = 0f; c[cc + 17] = 0f; c[cc + 18] = 0f; c[cc + 19] = 1.0f;
            c[cc + 20] = 0f; c[cc + 21] = 0f; c[cc + 22] = 0f; c[cc + 23] = 1.0f;
            vertexCount += 6;
        } else if (s == 5) {
            v[vc]=x+0.2f; v[vc+1]=y+0.8f; v[vc+2]=z+1+e; v[vc+3]=x+0.2f; v[vc+4]=y+0.65f; v[vc+5]=z+1+e; v[vc+6]=x+0.8f; v[vc+7]=y+0.65f; v[vc+8]=z+1+e; v[vc+9]=x+0.2f; v[vc+10]=y+0.8f; v[vc+11]=z+1+e; v[vc+12]=x+0.8f; v[vc+13]=y+0.65f; v[vc+14]=z+1+e; v[vc+15]=x+0.8f; v[vc+16]=y+0.8f; v[vc+17]=z+1+e;
            int cc = vertexCount * 4;
            c[cc] = 0f; c[cc + 1] = 0f; c[cc + 2] = 0f; c[cc + 3] = 1.0f;
            c[cc + 4] = 0f; c[cc + 5] = 0f; c[cc + 6] = 0f; c[cc + 7] = 1.0f;
            c[cc + 8] = 0f; c[cc + 9] = 0f; c[cc + 10] = 0f; c[cc + 11] = 1.0f;
            c[cc + 12] = 0f; c[cc + 13] = 0f; c[cc + 14] = 0f; c[cc + 15] = 1.0f;
            c[cc + 16] = 0f; c[cc + 17] = 0f; c[cc + 18] = 0f; c[cc + 19] = 1.0f;
            c[cc + 20] = 0f; c[cc + 21] = 0f; c[cc + 22] = 0f; c[cc + 23] = 1.0f;
            vertexCount += 6; vc = vertexCount * 3;
            v[vc]=x+0.4f; v[vc+1]=y+0.65f; v[vc+2]=z+1+e; v[vc+3]=x+0.4f; v[vc+4]=y+0.2f; v[vc+5]=z+1+e; v[vc+6]=x+0.6f; v[vc+7]=y+0.2f; v[vc+8]=z+1+e; v[vc+9]=x+0.4f; v[vc+10]=y+0.65f; v[vc+11]=z+1+e; v[vc+12]=x+0.6f; v[vc+13]=y+0.2f; v[vc+14]=z+1+e; v[vc+15]=x+0.6f; v[vc+16]=y+0.65f; v[vc+17]=z+1+e;
            cc = vertexCount * 4;
            c[cc] = 0f; c[cc + 1] = 0f; c[cc + 2] = 0f; c[cc + 3] = 1.0f;
            c[cc + 4] = 0f; c[cc + 5] = 0f; c[cc + 6] = 0f; c[cc + 7] = 1.0f;
            c[cc + 8] = 0f; c[cc + 9] = 0f; c[cc + 10] = 0f; c[cc + 11] = 1.0f;
            c[cc + 12] = 0f; c[cc + 13] = 0f; c[cc + 14] = 0f; c[cc + 15] = 1.0f;
            c[cc + 16] = 0f; c[cc + 17] = 0f; c[cc + 18] = 0f; c[cc + 19] = 1.0f;
            c[cc + 20] = 0f; c[cc + 21] = 0f; c[cc + 22] = 0f; c[cc + 23] = 1.0f;
            vertexCount += 6;
        } else if (s == 2) {
            v[vc]=x-e; v[vc+1]=y+0.8f; v[vc+2]=z+0.2f; v[vc+3]=x-e; v[vc+4]=y+0.65f; v[vc+5]=z+0.2f; v[vc+6]=x-e; v[vc+7]=y+0.65f; v[vc+8]=z+0.8f; v[vc+9]=x-e; v[vc+10]=y+0.8f; v[vc+11]=z+0.2f; v[vc+12]=x-e; v[vc+13]=y+0.65f; v[vc+14]=z+0.8f; v[vc+15]=x-e; v[vc+16]=y+0.8f; v[vc+17]=z+0.8f;
            int cc = vertexCount * 4;
            c[cc] = 0f; c[cc + 1] = 0f; c[cc + 2] = 0f; c[cc + 3] = 1.0f;
            c[cc + 4] = 0f; c[cc + 5] = 0f; c[cc + 6] = 0f; c[cc + 7] = 1.0f;
            c[cc + 8] = 0f; c[cc + 9] = 0f; c[cc + 10] = 0f; c[cc + 11] = 1.0f;
            c[cc + 12] = 0f; c[cc + 13] = 0f; c[cc + 14] = 0f; c[cc + 15] = 1.0f;
            c[cc + 16] = 0f; c[cc + 17] = 0f; c[cc + 18] = 0f; c[cc + 19] = 1.0f;
            c[cc + 20] = 0f; c[cc + 21] = 0f; c[cc + 22] = 0f; c[cc + 23] = 1.0f;
            vertexCount += 6; vc = vertexCount * 3;
            v[vc]=x-e; v[vc+1]=y+0.65f; v[vc+2]=z+0.4f; v[vc+3]=x-e; v[vc+4]=y+0.2f; v[vc+5]=z+0.4f; v[vc+6]=x-e; v[vc+7]=y+0.2f; v[vc+8]=z+0.6f; v[vc+9]=x-e; v[vc+10]=y+0.65f; v[vc+11]=z+0.4f; v[vc+12]=x-e; v[vc+13]=y+0.2f; v[vc+14]=z+0.6f; v[vc+15]=x-e; v[vc+16]=y+0.65f; v[vc+17]=z+0.6f;
            cc = vertexCount * 4;
            c[cc] = 0f; c[cc + 1] = 0f; c[cc + 2] = 0f; c[cc + 3] = 1.0f;
            c[cc + 4] = 0f; c[cc + 5] = 0f; c[cc + 6] = 0f; c[cc + 7] = 1.0f;
            c[cc + 8] = 0f; c[cc + 9] = 0f; c[cc + 10] = 0f; c[cc + 11] = 1.0f;
            c[cc + 12] = 0f; c[cc + 13] = 0f; c[cc + 14] = 0f; c[cc + 15] = 1.0f;
            c[cc + 16] = 0f; c[cc + 17] = 0f; c[cc + 18] = 0f; c[cc + 19] = 1.0f;
            c[cc + 20] = 0f; c[cc + 21] = 0f; c[cc + 22] = 0f; c[cc + 23] = 1.0f;
            vertexCount += 6;
        } else if (s == 3) {
            v[vc]=x+1+e; v[vc+1]=y+0.8f; v[vc+2]=z+0.8f; v[vc+3]=x+1+e; v[vc+4]=y+0.65f; v[vc+5]=z+0.8f; v[vc+6]=x+1+e; v[vc+7]=y+0.65f; v[vc+8]=z+0.2f; v[vc+9]=x+1+e; v[vc+10]=y+0.8f; v[vc+11]=z+0.8f; v[vc+12]=x+1+e; v[vc+13]=y+0.65f; v[vc+14]=z+0.2f; v[vc+15]=x+1+e; v[vc+16]=y+0.8f; v[vc+17]=z+0.2f;
            int cc = vertexCount * 4;
            c[cc] = 0f; c[cc + 1] = 0f; c[cc + 2] = 0f; c[cc + 3] = 1.0f;
            c[cc + 4] = 0f; c[cc + 5] = 0f; c[cc + 6] = 0f; c[cc + 7] = 1.0f;
            c[cc + 8] = 0f; c[cc + 9] = 0f; c[cc + 10] = 0f; c[cc + 11] = 1.0f;
            c[cc + 12] = 0f; c[cc + 13] = 0f; c[cc + 14] = 0f; c[cc + 15] = 1.0f;
            c[cc + 16] = 0f; c[cc + 17] = 0f; c[cc + 18] = 0f; c[cc + 19] = 1.0f;
            c[cc + 20] = 0f; c[cc + 21] = 0f; c[cc + 22] = 0f; c[cc + 23] = 1.0f;
            vertexCount += 6; vc = vertexCount * 3;
            v[vc]=x+1+e; v[vc+1]=y+0.65f; v[vc+2]=z+0.6f; v[vc+3]=x+1+e; v[vc+4]=y+0.2f; v[vc+5]=z+0.6f; v[vc+6]=x+1+e; v[vc+7]=y+0.2f; v[vc+8]=z+0.4f; v[vc+9]=x+1+e; v[vc+10]=y+0.65f; v[vc+11]=z+0.6f; v[vc+12]=x+1+e; v[vc+13]=y+0.2f; v[vc+14]=z+0.4f; v[vc+15]=x+1+e; v[vc+16]=y+0.65f; v[vc+17]=z+0.4f;
            cc = vertexCount * 4;
            c[cc] = 0f; c[cc + 1] = 0f; c[cc + 2] = 0f; c[cc + 3] = 1.0f;
            c[cc + 4] = 0f; c[cc + 5] = 0f; c[cc + 6] = 0f; c[cc + 7] = 1.0f;
            c[cc + 8] = 0f; c[cc + 9] = 0f; c[cc + 10] = 0f; c[cc + 11] = 1.0f;
            c[cc + 12] = 0f; c[cc + 13] = 0f; c[cc + 14] = 0f; c[cc + 15] = 1.0f;
            c[cc + 16] = 0f; c[cc + 17] = 0f; c[cc + 18] = 0f; c[cc + 19] = 1.0f;
            c[cc + 20] = 0f; c[cc + 21] = 0f; c[cc + 22] = 0f; c[cc + 23] = 1.0f;
            vertexCount += 6;
        }
    }
}
