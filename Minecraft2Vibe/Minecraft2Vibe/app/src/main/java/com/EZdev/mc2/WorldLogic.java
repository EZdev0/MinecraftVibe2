package com.EZdev.mc2;

import android.opengl.GLES20;
import android.opengl.Matrix;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;

public class WorldLogic {
    private HashMap<String, Chunk> chunks = new HashMap<>();
    private float[] finalMVP = new float[16];
    private float[] modelMatrix = new float[16];

    public int renderDistance = 2;
    public boolean fogEnabled = true;

    private Gameplay gameplayRef; // Reference to gameplay for active fires

    public void updateChunks(float playerX, float playerZ) {
        int playerChunkX = (int) Math.floor(playerX / 16.0);
        int playerChunkZ = (int) Math.floor(playerZ / 16.0);

        for (int x = -renderDistance; x <= renderDistance; x++) {
            for (int z = -renderDistance; z <= renderDistance; z++) {
                String key = (playerChunkX + x) + "," + (playerChunkZ + z);
                if (!chunks.containsKey(key)) {
                    Chunk newChunk = new Chunk(this, playerChunkX + x, playerChunkZ + z);
                    chunks.put(key, newChunk);
                    updateNeighbors(playerChunkX + x, playerChunkZ + z);
                }
            }
        }
    }

    private void updateNeighbors(int cx, int cz) {
        String[] ns = {(cx+1)+","+cz, (cx-1)+","+cz, cx+","+(cz+1), cx+","+(cz-1)};
        for(String nKey : ns) {
            Chunk n = chunks.get(nKey);
            if(n != null) n.buildMesh();
        }
    }

    public byte getBlock(int x, int y, int z) {
        if (y < 0 || y >= 128) return 0;
        int cx = (int) Math.floor(x / 16.0);
        int cz = (int) Math.floor(z / 16.0);
        String key = cx + "," + cz;
        Chunk c = chunks.get(key);
        if (c != null) return c.blocks[x - (cx * 16)][y][z - (cz * 16)];
        return 0;
    }

    public void setBlock(int x, int y, int z, byte type) {
        if (y < 0 || y >= 128) return;
        int cx = (int) Math.floor(x / 16.0);
        int cz = (int) Math.floor(z / 16.0);
        String key = cx + "," + cz;
        Chunk c = chunks.get(key);
        if (c != null) {
            int lx = x - (cx * 16);
            int lz = z - (cz * 16);
            c.blocks[lx][y][lz] = type;
            c.buildMesh();

            // Nachbar-Mesh Update für Culling
            if (lx == 0) updateNeighbors(cx, cz);
            if (lx == 15) updateNeighbors(cx, cz);
            if (lz == 0) updateNeighbors(cx, cz);
            if (lz == 15) updateNeighbors(cx, cz);
        }
    }

    // --- ZÜND-CHECK (Feuer sucht TNT) ---
    public void checkIgnition(int x, int y, int z, Gameplay g) {
        // Prüfe alle 6 Nachbarn um die Position (x,y,z)
        int[][] neighbors = {{1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}};
        for(int[] n : neighbors) {
            int nx = x + n[0];
            int ny = y + n[1];
            int nz = z + n[2];
            if(getBlock(nx, ny, nz) == 5) { // Wenn Nachbar TNT ist
                setBlock(nx, ny, nz, (byte)0); // Entferne TNT Block
                Gameplay.ActiveTNT newTNT = g.new ActiveTNT(nx + 0.5f, ny, nz + 0.5f);
                g.tickingTNTs.add(newTNT); // Starte Animation
            }
        }
    }

    public void explode(float ex, float ey, float ez, float radius) {
        int minX = (int) Math.floor(ex - radius); int maxX = (int) Math.ceil(ex + radius);
        int minY = (int) Math.floor(ey - radius); int maxY = (int) Math.ceil(ey + radius);
        int minZ = (int) Math.floor(ez - radius); int maxZ = (int) Math.ceil(ez + radius);
        HashSet<Chunk> chunksToUpdate = new HashSet<>();

        if (gameplayRef != null) {
            // Apply velocity to existing active TNTs
            for (Gameplay.ActiveTNT tnt : gameplayRef.tickingTNTs) {
                float dx = tnt.x - ex;
                float dy = tnt.y - ey;
                float dz = tnt.z - ez;
                float dist = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
                if (dist > 0 && dist <= radius * 2.0f) {
                    float force = (radius * 2.0f - dist) / (radius * 2.0f);
                    tnt.vx += (dx / dist) * force * 15.0f;
                    tnt.vy += (dy / dist) * force * 15.0f + force * 5.0f; // Extra upward boost
                    tnt.vz += (dz / dist) * force * 15.0f;
                }
            }
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    float dx = x + 0.5f - ex;
                    float dy = y + 0.5f - ey;
                    float dz = z + 0.5f - ez;
                    float dist = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
                    if (dist <= radius && y >= 1 && y < 128) {
                        byte block = getBlock(x, y, z);
                        if (block == 5 && gameplayRef != null) { // Ignite other TNTs in radius
                            setBlock(x, y, z, (byte)0);
                            Gameplay.ActiveTNT newTNT = gameplayRef.new ActiveTNT(x + 0.5f, y + 0.5f, z + 0.5f);
                            // Give them velocity
                            float force = (radius - dist) / radius;
                            newTNT.vx = (dx / dist) * force * 15.0f;
                            newTNT.vy = (dy / dist) * force * 15.0f + 5.0f;
                            newTNT.vz = (dz / dist) * force * 15.0f;
                            newTNT.timer = 0.5f + (float)Math.random() * 1.5f; // Randomize fuse for chain reaction
                            gameplayRef.tickingTNTs.add(newTNT);
                        } else if (block != 0) {
                            int cx = (int) Math.floor(x / 16.0);
                            int cz = (int) Math.floor(z / 16.0);
                            Chunk c = chunks.get(cx + "," + cz);
                            if (c != null) {
                                c.blocks[x - (cx * 16)][y][z - (cz * 16)] = 0;
                                chunksToUpdate.add(c);
                            }
                        }
                    }
                }
            }
        }
        for (Chunk c : chunksToUpdate) c.buildMesh();
    }

    public void render(float[] vpMatrix, Gameplay gameplay) {
        this.gameplayRef = gameplay;
        GLES20.glUseProgram(Booster.shaderProgram);
        GLES20.glEnableVertexAttribArray(Booster.posHandle);
        GLES20.glEnableVertexAttribArray(Booster.colorHandle);

        GLES20.glUniform1i(Booster.fogEnabledHandle, fogEnabled ? 1 : 0);
        GLES20.glUniform1f(Booster.fogEndHandle, renderDistance * 16.0f);
        GLES20.glUniform1i(Booster.isFlashingHandle, 0);

        for (Map.Entry<String, Chunk> entry : chunks.entrySet()) {
            Chunk c = entry.getValue();
            if (c.vertexBuffer == null || c.vertexCount == 0) continue;
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, c.chunkX * 16, 0, c.chunkZ * 16);
            Matrix.multiplyMM(finalMVP, 0, vpMatrix, 0, modelMatrix, 0);
            GLES20.glUniformMatrix4fv(Booster.mvpHandle, 1, false, finalMVP, 0);
            GLES20.glVertexAttribPointer(Booster.posHandle, 3, GLES20.GL_FLOAT, false, 0, c.vertexBuffer);
            GLES20.glVertexAttribPointer(Booster.colorHandle, 4, GLES20.GL_FLOAT, false, 0, c.colorBuffer);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, c.vertexCount);
        }

        if (Booster.tntVertexBuffer != null) {
            GLES20.glUniform1i(Booster.isFlashingHandle, 1);
            for (Gameplay.ActiveTNT tnt : gameplay.tickingTNTs) {
                Matrix.setIdentityM(modelMatrix, 0);
                Matrix.translateM(modelMatrix, 0, tnt.x, tnt.y, tnt.z);
                float scale = 1.0f + ((float)Math.sin(gameplay.gameTime * 15.0) * 0.05f);
                Matrix.scaleM(modelMatrix, 0, scale, scale, scale);
                Matrix.multiplyMM(finalMVP, 0, vpMatrix, 0, modelMatrix, 0);
                GLES20.glUniformMatrix4fv(Booster.mvpHandle, 1, false, finalMVP, 0);
                GLES20.glVertexAttribPointer(Booster.posHandle, 3, GLES20.GL_FLOAT, false, 0, Booster.tntVertexBuffer);
                GLES20.glVertexAttribPointer(Booster.colorHandle, 4, GLES20.GL_FLOAT, false, 0, Booster.tntColorBuffer);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
            }
        }
        GLES20.glDisableVertexAttribArray(Booster.posHandle);
        GLES20.glDisableVertexAttribArray(Booster.colorHandle);
    }

    public void interact(Gameplay g, boolean place, UIManager ui) {
        float eyeHeight = g.camY + g.playerHeight - 0.2f;
        float dirX = (float) (Math.cos(Math.toRadians(g.pitch)) * Math.sin(Math.toRadians(g.yaw)));
        float dirY = (float) Math.sin(Math.toRadians(g.pitch));
        float dirZ = (float) (-Math.cos(Math.toRadians(g.pitch)) * Math.cos(Math.toRadians(g.yaw)));
        int lastX = -1, lastY = -1, lastZ = -1;

        for(float dist = 0; dist < 6f; dist += 0.1f) {
            int bx = (int) Math.floor(g.camX + dirX * dist);
            int by = (int) Math.floor(eyeHeight + dirY * dist);
            int bz = (int) Math.floor(g.camZ + dirZ * dist);
            byte hitBlock = getBlock(bx, by, bz);
            if (hitBlock > 0 && hitBlock != 6) {
                if (!place && hitBlock == 5 && g.activeBlock == 6) {
                    setBlock(bx, by, bz, (byte)0);
                    g.tickingTNTs.add(g.new ActiveTNT(bx + 0.5f, by, bz + 0.5f));
                    return;
                }
                if (place && lastX != -1 && !isPlayerInside(g, lastX, lastY, lastZ)) {
                    setBlock(lastX, lastY, lastZ, g.activeBlock);
                    // Wenn wir Feuer (6) platzieren, checke Nachbarn auf TNT
                    if(g.activeBlock == 6) {
                        checkIgnition(lastX, lastY, lastZ, g);
                        g.activeFires.add(g.new ActiveFire(lastX, lastY, lastZ)); // Start spreading
                    }
                    if (g.activeBlock == 5) {
                        g.activeBlock = 6;
                        if (ui != null) ui.updateHotbarUI();
                        // Wenn wir TNT (5) platzieren, checke ob schon Feuer daneben ist
                        checkIgnition(lastX, lastY, lastZ, g);
                    }
                } else if (!place) {
                    setBlock(bx, by, bz, (byte)0);
                }
                return;
            }
            lastX = bx; lastY = by; lastZ = bz;
        }
    }

    private boolean isPlayerInside(Gameplay g, int bx, int by, int bz) {
        float shrink = 0.05f;
        float pMinX = g.camX - g.playerWidth / 2f + shrink;
        float pMaxX = g.camX + g.playerWidth / 2f - shrink;
        float pMinY = g.camY + 0.1f;
        float pMaxY = g.camY + g.playerHeight - shrink;
        float pMinZ = g.camZ - g.playerWidth / 2f + shrink;
        float pMaxZ = g.camZ + g.playerWidth / 2f - shrink;
        return (pMinX < bx + 1 && pMaxX > bx && pMinY < by + 1 && pMaxY > by && pMinZ < bz + 1 && pMaxZ > bz);
    }
}
