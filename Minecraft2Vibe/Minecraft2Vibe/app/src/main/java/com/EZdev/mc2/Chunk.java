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
                float noiseVal = Noise.simplex2(gx * 0.015f, gz * 0.015f);
                int height = 65 + (int)(noiseVal * 20f); // Higher terrain to make room for deeper caves
                for (int y = 0; y <= height; y++) {
                    if (y == 0) { blocks[x][y][z] = 9; continue; } // Bedrock (ID 9) at y=0
                    if (y == 1 && Math.random() < 0.5) { blocks[x][y][z] = 9; continue; } // Bedrock layer 1 noise

                    // Feature 4: Minecraft-like 3D Cave Generation (Simplex Noise)
                    // Base terrain filling
                    if (y == height) blocks[x][y][z] = 1; // Grass
                    else if (y > height - 4) blocks[x][y][z] = 1; // Dirt (should technically be dirt, but we use 1 for both)
                    else blocks[x][y][z] = 2; // Stone

                    // Only dig caves if we are safely below the surface (e.g. 5 blocks deep)
                    if (y < height - 5 && y > 1) {
                        float caveNoise1 = Noise.simplex3(gx * 0.03f, y * 0.03f, gz * 0.03f);
                        float caveNoise2 = Noise.simplex3(gx * 0.03f + 1000f, y * 0.03f + 1000f, gz * 0.03f + 1000f);

                        // "Spaghetti" Tunnels (worms) - intersections of two noises around 0
                        boolean isWorm = Math.abs(caveNoise1) < 0.06f && Math.abs(caveNoise2) < 0.06f;

                        // "Cheese" Caves - large hollow areas based on a density threshold
                        float cheeseNoise = Noise.simplex3(gx * 0.015f, y * 0.02f, gz * 0.015f);
                        boolean isCheese = cheeseNoise > 0.5f;

                        if (isWorm || isCheese) {
                            blocks[x][y][z] = 0; // Dig out the cave (Air)

                            // If digging near the bottom, fill with water (like underground lakes)
                            if (y < 12) blocks[x][y][z] = 7;
                        }
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
                    else if (type == 9) { r = 0.1f; g = 0.1f; b = 0.1f; } // Bedrock (very dark grey)

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

        synchronized(this) {
            vertexBuffer.clear();
            vertexBuffer.put(vData, 0, vertexCount * 3).position(0);

            colorBuffer.clear();
            colorBuffer.put(cData, 0, vertexCount * 4).position(0);

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
        for(int i=0; i<6; i++) { c[cc + i*4] = r; c[cc + i*4+1] = g; c[cc + i*4+2] = b; c[cc + i*4+3] = a; }
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

    private void addDirect(float[] v, float[] c, float[] values) {
        if(vertexCount >= (v.length/3) - 6) return;
        int vc = vertexCount * 3;
        for(int i=0; i<18; i++) v[vc + i] = values[i];
        int cc = vertexCount * 4;
        for(int i=0; i<6; i++) { c[cc + i*4] = 0f; c[cc + i*4+1] = 0f; c[cc + i*4+2] = 0f; c[cc + i*4+3] = 1.0f; }
        vertexCount += 6;
    }

    // Memory-friendly T draw using simple local assignment instead of new float[]
    private void drawLetterT(float[] v, float[] c, float x, float y, float z, int s) {
        if(vertexCount >= (v.length/3) - 12) return;
        float e = 0.01f;
        int vc = vertexCount * 3;
        if (s == 4) {
            v[vc]=x+0.8f; v[vc+1]=y+0.8f; v[vc+2]=z-e; v[vc+3]=x+0.8f; v[vc+4]=y+0.65f; v[vc+5]=z-e; v[vc+6]=x+0.2f; v[vc+7]=y+0.65f; v[vc+8]=z-e; v[vc+9]=x+0.8f; v[vc+10]=y+0.8f; v[vc+11]=z-e; v[vc+12]=x+0.2f; v[vc+13]=y+0.65f; v[vc+14]=z-e; v[vc+15]=x+0.2f; v[vc+16]=y+0.8f; v[vc+17]=z-e;
            int cc = vertexCount * 4; for(int i=0; i<6; i++) { c[cc + i*4] = 0f; c[cc + i*4+1] = 0f; c[cc + i*4+2] = 0f; c[cc + i*4+3] = 1.0f; } vertexCount += 6; vc = vertexCount * 3;
            v[vc]=x+0.6f; v[vc+1]=y+0.65f; v[vc+2]=z-e; v[vc+3]=x+0.6f; v[vc+4]=y+0.2f; v[vc+5]=z-e; v[vc+6]=x+0.4f; v[vc+7]=y+0.2f; v[vc+8]=z-e; v[vc+9]=x+0.6f; v[vc+10]=y+0.65f; v[vc+11]=z-e; v[vc+12]=x+0.4f; v[vc+13]=y+0.2f; v[vc+14]=z-e; v[vc+15]=x+0.4f; v[vc+16]=y+0.65f; v[vc+17]=z-e;
            cc = vertexCount * 4; for(int i=0; i<6; i++) { c[cc + i*4] = 0f; c[cc + i*4+1] = 0f; c[cc + i*4+2] = 0f; c[cc + i*4+3] = 1.0f; } vertexCount += 6;
        } else if (s == 5) {
            v[vc]=x+0.2f; v[vc+1]=y+0.8f; v[vc+2]=z+1+e; v[vc+3]=x+0.2f; v[vc+4]=y+0.65f; v[vc+5]=z+1+e; v[vc+6]=x+0.8f; v[vc+7]=y+0.65f; v[vc+8]=z+1+e; v[vc+9]=x+0.2f; v[vc+10]=y+0.8f; v[vc+11]=z+1+e; v[vc+12]=x+0.8f; v[vc+13]=y+0.65f; v[vc+14]=z+1+e; v[vc+15]=x+0.8f; v[vc+16]=y+0.8f; v[vc+17]=z+1+e;
            int cc = vertexCount * 4; for(int i=0; i<6; i++) { c[cc + i*4] = 0f; c[cc + i*4+1] = 0f; c[cc + i*4+2] = 0f; c[cc + i*4+3] = 1.0f; } vertexCount += 6; vc = vertexCount * 3;
            v[vc]=x+0.4f; v[vc+1]=y+0.65f; v[vc+2]=z+1+e; v[vc+3]=x+0.4f; v[vc+4]=y+0.2f; v[vc+5]=z+1+e; v[vc+6]=x+0.6f; v[vc+7]=y+0.2f; v[vc+8]=z+1+e; v[vc+9]=x+0.4f; v[vc+10]=y+0.65f; v[vc+11]=z+1+e; v[vc+12]=x+0.6f; v[vc+13]=y+0.2f; v[vc+14]=z+1+e; v[vc+15]=x+0.6f; v[vc+16]=y+0.65f; v[vc+17]=z+1+e;
            cc = vertexCount * 4; for(int i=0; i<6; i++) { c[cc + i*4] = 0f; c[cc + i*4+1] = 0f; c[cc + i*4+2] = 0f; c[cc + i*4+3] = 1.0f; } vertexCount += 6;
        } else if (s == 2) {
            v[vc]=x-e; v[vc+1]=y+0.8f; v[vc+2]=z+0.2f; v[vc+3]=x-e; v[vc+4]=y+0.65f; v[vc+5]=z+0.2f; v[vc+6]=x-e; v[vc+7]=y+0.65f; v[vc+8]=z+0.8f; v[vc+9]=x-e; v[vc+10]=y+0.8f; v[vc+11]=z+0.2f; v[vc+12]=x-e; v[vc+13]=y+0.65f; v[vc+14]=z+0.8f; v[vc+15]=x-e; v[vc+16]=y+0.8f; v[vc+17]=z+0.8f;
            int cc = vertexCount * 4; for(int i=0; i<6; i++) { c[cc + i*4] = 0f; c[cc + i*4+1] = 0f; c[cc + i*4+2] = 0f; c[cc + i*4+3] = 1.0f; } vertexCount += 6; vc = vertexCount * 3;
            v[vc]=x-e; v[vc+1]=y+0.65f; v[vc+2]=z+0.4f; v[vc+3]=x-e; v[vc+4]=y+0.2f; v[vc+5]=z+0.4f; v[vc+6]=x-e; v[vc+7]=y+0.2f; v[vc+8]=z+0.6f; v[vc+9]=x-e; v[vc+10]=y+0.65f; v[vc+11]=z+0.4f; v[vc+12]=x-e; v[vc+13]=y+0.2f; v[vc+14]=z+0.6f; v[vc+15]=x-e; v[vc+16]=y+0.65f; v[vc+17]=z+0.6f;
            cc = vertexCount * 4; for(int i=0; i<6; i++) { c[cc + i*4] = 0f; c[cc + i*4+1] = 0f; c[cc + i*4+2] = 0f; c[cc + i*4+3] = 1.0f; } vertexCount += 6;
        } else if (s == 3) {
            v[vc]=x+1+e; v[vc+1]=y+0.8f; v[vc+2]=z+0.8f; v[vc+3]=x+1+e; v[vc+4]=y+0.65f; v[vc+5]=z+0.8f; v[vc+6]=x+1+e; v[vc+7]=y+0.65f; v[vc+8]=z+0.2f; v[vc+9]=x+1+e; v[vc+10]=y+0.8f; v[vc+11]=z+0.8f; v[vc+12]=x+1+e; v[vc+13]=y+0.65f; v[vc+14]=z+0.2f; v[vc+15]=x+1+e; v[vc+16]=y+0.8f; v[vc+17]=z+0.2f;
            int cc = vertexCount * 4; for(int i=0; i<6; i++) { c[cc + i*4] = 0f; c[cc + i*4+1] = 0f; c[cc + i*4+2] = 0f; c[cc + i*4+3] = 1.0f; } vertexCount += 6; vc = vertexCount * 3;
            v[vc]=x+1+e; v[vc+1]=y+0.65f; v[vc+2]=z+0.6f; v[vc+3]=x+1+e; v[vc+4]=y+0.2f; v[vc+5]=z+0.6f; v[vc+6]=x+1+e; v[vc+7]=y+0.2f; v[vc+8]=z+0.4f; v[vc+9]=x+1+e; v[vc+10]=y+0.65f; v[vc+11]=z+0.6f; v[vc+12]=x+1+e; v[vc+13]=y+0.2f; v[vc+14]=z+0.4f; v[vc+15]=x+1+e; v[vc+16]=y+0.65f; v[vc+17]=z+0.4f;
            cc = vertexCount * 4; for(int i=0; i<6; i++) { c[cc + i*4] = 0f; c[cc + i*4+1] = 0f; c[cc + i*4+2] = 0f; c[cc + i*4+3] = 1.0f; } vertexCount += 6;
        }
    }
}
