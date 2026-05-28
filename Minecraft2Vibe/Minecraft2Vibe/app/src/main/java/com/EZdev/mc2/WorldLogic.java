package com.EZdev.mc2;

import android.opengl.GLES20;
import android.opengl.Matrix;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorldLogic {
    public static final int CHUNK_SIZE = 16;
    public Map<String, Chunk> chunks = new ConcurrentHashMap<>();
    public ArrayList<Entity> entities = new ArrayList<>();
    public ArrayList<Gameplay.ItemEntity> droppedItems = new ArrayList<>();
    public SaveManager saveManager;
    public MultiplayerManager multiplayerManager;
    public long worldSeed = 1337L;
    public int renderDistance = 2;
    public boolean fogEnabled = true;
    private float[] modelMatrix = new float[16];
    private float[] finalMVP = new float[16];

    public WorldLogic() {
        Noise.init(worldSeed);
        TextRenderer.init();
    }

    public void updateChunks(float px, float pz) {
        int cx = (int) Math.floor(px / CHUNK_SIZE);
        int cz = (int) Math.floor(pz / CHUNK_SIZE);
        for (int x = -renderDistance; x <= renderDistance; x++) {
            for (int z = -renderDistance; z <= renderDistance; z++) {
                String key = (cx + x) + "," + (cz + z);
                if (!chunks.containsKey(key)) {
                    chunks.put(key, new Chunk(this, cx + x, cz + z));
                }
            }
        }
    }

    public byte getBlock(int x, int y, int z) {
        if (y < 0 || y >= 128) return Blocks.AIR;
        Chunk c = chunks.get(((int) Math.floor((float) x / 16)) + "," + ((int) Math.floor((float) z / 16)));
        return (c != null) ? c.blocks[x & 15][y][z & 15] : Blocks.AIR;
    }

    public void setBlock(int x, int y, int z, byte block) {
        setBlock(x, y, z, block, true);
    }

    public synchronized void setBlock(int x, int y, int z, byte block, boolean sync) {
        if (y < 0 || y >= 128) return;
        if (y == 0 && block == Blocks.AIR) return;
        if (getBlock(x, y, z) == Blocks.BEDROCK && block == Blocks.AIR) return;
        Chunk c = chunks.get(((int) Math.floor((float) x / 16)) + "," + ((int) Math.floor((float) z / 16)));
        if (c != null) {
            c.blocks[x & 15][y][z & 15] = block;
            c.buildMesh();

            if (block == Blocks.FIRE && gameplayRef != null) {
                boolean exists = false;
                for (int i = 0; i < gameplayRef.activeFires.size(); i++) {
                    Gameplay.ActiveFire f = gameplayRef.activeFires.get(i);
                    if (f.x == x && f.y == y && f.z == z) { exists = true; break; }
                }
                if (!exists) {
                    gameplayRef.activeFires.add(gameplayRef.obtainFire(x, y, z));
                }
            }

            if (sync && multiplayerManager != null) {
                multiplayerManager.sendBlockChange(x, y, z, block);
            }
        }
    }

    public void explode(float x, float y, float z, float radius) {
        if (gameplayRef != null) {
            gameplayRef.addExplosionParticles(x, y, z);
            gameplayRef.shakeIntensity = 0.5f;
        }
        int r = (int) Math.ceil(radius);
        float rSq = radius * radius;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    float d2 = (float) dx * dx + (float) dy * dy + (float) dz * dz;
                    if (d2 <= rSq) {
                        int bx = (int) Math.floor(x + dx);
                        int by = (int) Math.floor(y + dy);
                        int bz = (int) Math.floor(z + dz);
                        if (by >= 1 && by < 127) {
                            byte b = getBlock(bx, by, bz);
                            if (b == Blocks.TNT) {
                                setBlock(bx, by, bz, Blocks.AIR);
                                if (gameplayRef != null) {
                                    Gameplay.ActiveTNT tnt = gameplayRef.obtainTNT(bx + 0.5f, by, bz + 0.5f);
                                    gameplayRef.tickingTNTs.add(tnt);
                                }
                            } else if (b != Blocks.BEDROCK && b != Blocks.AIR) {
                                setBlock(bx, by, bz, Blocks.AIR);
                                if (gameplayRef != null) gameplayRef.addBlockParticles(bx, by, bz, b);
                            }
                        }
                    }
                }
            }
        }
    }

    public void checkIgnition(int x, int y, int z, Gameplay g) {
        int[][] neighbors = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        for (int[] n : neighbors) {
            int nx = x + n[0], ny = y + n[1], nz = z + n[2];
            if (getBlock(nx, ny, nz) == Blocks.TNT) {
                setBlock(nx, ny, nz, Blocks.AIR);
                g.tickingTNTs.add(g.obtainTNT(nx + 0.5f, ny, nz + 0.5f));
            }
        }
    }

    private void checkIgnitionIfTntPlaced(int x, int y, int z, Gameplay g) {
        int[][] neighbors = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        for (int[] n : neighbors) {
            int nx = x + n[0], ny = y + n[1], nz = z + n[2];
            if (getBlock(nx, ny, nz) == Blocks.FIRE) {
                setBlock(x, y, z, Blocks.AIR);
                g.tickingTNTs.add(g.obtainTNT(x + 0.5f, y, z + 0.5f));
                return;
            }
        }
    }

    public void render(float[] vpMatrix, Gameplay gameplay) {
        GLES20.glEnableVertexAttribArray(Booster.posHandle);
        GLES20.glEnableVertexAttribArray(Booster.colorHandle);

        GLES20.glUniform1i(Booster.fogEnabledHandle, fogEnabled ? 1 : 0);
        GLES20.glUniform1f(Booster.fogEndHandle, renderDistance * 16.0f);
        GLES20.glUniform1i(Booster.isFlashingHandle, 0);

        for (Chunk c : chunks.values()) {
            if (c.vertexBuffer != null) {
                Matrix.setIdentityM(modelMatrix, 0);
                Matrix.translateM(modelMatrix, 0, c.chunkX * 16, 0, c.chunkZ * 16);
                Matrix.multiplyMM(finalMVP, 0, vpMatrix, 0, modelMatrix, 0);
                GLES20.glUniformMatrix4fv(Booster.mvpHandle, 1, false, finalMVP, 0);
                GLES20.glUniform1i(Booster.pTypeHandle, 0);
                GLES20.glVertexAttribPointer(Booster.posHandle, 3, GLES20.GL_FLOAT, false, 0, c.vertexBuffer);
                GLES20.glVertexAttribPointer(Booster.colorHandle, 4, GLES20.GL_FLOAT, false, 0, c.colorBuffer);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, c.vertexCount);
            }
        }

        if (Booster.tntVertexBuffer != null) {
            GLES20.glUniform1i(Booster.isFlashingHandle, 1);
            for (Gameplay.ActiveTNT tnt : gameplay.tickingTNTs) {
                Matrix.setIdentityM(modelMatrix, 0);
                Matrix.translateM(modelMatrix, 0, tnt.x, tnt.y, tnt.z);
                float scale = 1.0f + ((float) Math.sin(gameplay.gameTime * 15.0) * 0.05f);
                Matrix.scaleM(modelMatrix, 0, scale, scale, scale);
                Matrix.multiplyMM(finalMVP, 0, vpMatrix, 0, modelMatrix, 0);
                GLES20.glUniformMatrix4fv(Booster.mvpHandle, 1, false, finalMVP, 0);
                GLES20.glUniform1i(Booster.pTypeHandle, Blocks.TNT);
                GLES20.glVertexAttribPointer(Booster.posHandle, 3, GLES20.GL_FLOAT, false, 0, Booster.tntVertexBuffer);
                GLES20.glVertexAttribPointer(Booster.colorHandle, 4, GLES20.GL_FLOAT, false, 0, Booster.tntColorBuffer);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
            }
            GLES20.glUniform1i(Booster.isFlashingHandle, 0);
        }

        if (multiplayerManager != null) {
            for (MultiplayerManager.RemotePlayer rp : multiplayerManager.players.values()) {
                if (System.currentTimeMillis() - rp.lastUpdate < 5000) {
                    renderRemotePlayer(rp, vpMatrix);
                }
            }
        }
        GLES20.glDisableVertexAttribArray(Booster.posHandle);
        GLES20.glDisableVertexAttribArray(Booster.colorHandle);
    }

    private void renderRemotePlayer(MultiplayerManager.RemotePlayer rp, float[] vpMatrix) {
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, rp.x, rp.y + 0.9f, rp.z);
        Matrix.rotateM(modelMatrix, 0, -rp.yaw, 0, 1, 0);
        Matrix.scaleM(modelMatrix, 0, 0.6f, 1.2f, 0.3f);
        Matrix.multiplyMM(finalMVP, 0, vpMatrix, 0, modelMatrix, 0);
        GLES20.glUniformMatrix4fv(Booster.mvpHandle, 1, false, finalMVP, 0);
        GLES20.glUniform1i(Booster.pTypeHandle, Blocks.STONE);
        GLES20.glVertexAttribPointer(Booster.posHandle, 3, GLES20.GL_FLOAT, false, 0, Booster.tntVertexBuffer);
        GLES20.glVertexAttribPointer(Booster.colorHandle, 4, GLES20.GL_FLOAT, false, 0, Booster.tntColorBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, rp.x, rp.y + 1.8f, rp.z);
        Matrix.rotateM(modelMatrix, 0, -rp.yaw, 0, 1, 0);
        Matrix.rotateM(modelMatrix, 0, rp.pitch, 1, 0, 0);
        Matrix.scaleM(modelMatrix, 0, 0.4f, 0.4f, 0.4f);
        Matrix.multiplyMM(finalMVP, 0, vpMatrix, 0, modelMatrix, 0);
        GLES20.glUniformMatrix4fv(Booster.mvpHandle, 1, false, finalMVP, 0);
        GLES20.glUniform1i(Booster.pTypeHandle, Blocks.ENTITY_PIG);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);

        if (rp.nameTagTexture == -1) rp.nameTagTexture = TextRenderer.createTextTexture(rp.name);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, rp.x, rp.y + 2.2f, rp.z);
        Matrix.rotateM(modelMatrix, 0, -rp.yaw, 0, 1, 0);
        Matrix.scaleM(modelMatrix, 0, 1.5f, 0.5f, 1.0f);
        Matrix.multiplyMM(finalMVP, 0, vpMatrix, 0, modelMatrix, 0);
        GLES20.glUniformMatrix4fv(Booster.mvpHandle, 1, false, finalMVP, 0);
        GLES20.glUniform1i(Booster.useTexHandle, 1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, rp.nameTagTexture);
        GLES20.glEnableVertexAttribArray(Booster.texCoordHandle);
        GLES20.glVertexAttribPointer(Booster.texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, Booster.quadTexCoordBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        GLES20.glDisableVertexAttribArray(Booster.texCoordHandle);
        GLES20.glUniform1i(Booster.useTexHandle, 0);
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    public void interact(Gameplay g, boolean place, UIManager ui) {
        float eyeHeight = g.camY + g.playerHeight - 0.2f;
        float dirX = (float) (Math.cos(Math.toRadians(g.pitch)) * Math.sin(Math.toRadians(g.yaw)));
        float dirY = (float) Math.sin(Math.toRadians(g.pitch));
        float dirZ = (float) (-Math.cos(Math.toRadians(g.pitch)) * Math.cos(Math.toRadians(g.yaw)));
        int lastX = -1, lastY = -1, lastZ = -1;

        for (float dist = 0; dist < 6f; dist += 0.1f) {
            int bx = (int) Math.floor(g.camX + dirX * dist);
            int by = (int) Math.floor(eyeHeight + dirY * dist);
            int bz = (int) Math.floor(g.camZ + dirZ * dist);

            byte hitBlock = getBlock(bx, by, bz);
            if (hitBlock > 0 && hitBlock != Blocks.WATER && hitBlock != Blocks.FIRE) {
                if (place) {
                    if (g.activeBlock == Blocks.FIRE && hitBlock == Blocks.TNT) {
                        setBlock(bx, by, bz, Blocks.AIR);
                        g.tickingTNTs.add(g.obtainTNT(bx + 0.5f, by, bz + 0.5f));
                        return;
                    }
                    if (lastX != -1 && !isPlayerInside(g, lastX, lastY, lastZ)) {
                        setBlock(lastX, lastY, lastZ, g.activeBlock);
                        if (g.activeBlock == Blocks.FIRE) checkIgnition(lastX, lastY, lastZ, g);
                        if (g.activeBlock == Blocks.TNT) checkIgnitionIfTntPlaced(lastX, lastY, lastZ, g);
                        if (!g.isCreative && ui != null && ui.inventory[g.activeSlot] != 999) {
                            ui.inventory[g.activeSlot]--;
                            ui.updateHotbarUI();
                        }
                    }
                } else {
                    if (g.isCreative) {
                        g.addBlockParticles(bx, by, bz, hitBlock);
                        setBlock(bx, by, bz, Blocks.AIR);
                    } else {
                        g.isBreaking = true;
                        g.targetX = bx;
                        g.targetY = by;
                        g.targetZ = bz;
                    }
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

    public int[] raycastBlock(Gameplay g, int[] out) {
        float eye = g.camY + g.playerHeight - 0.2f;
        float dirX = (float) (Math.cos(Math.toRadians(g.pitch)) * Math.sin(Math.toRadians(g.yaw)));
        float dirY = (float) Math.sin(Math.toRadians(g.pitch));
        float dirZ = (float) (-Math.cos(Math.toRadians(g.pitch)) * Math.cos(Math.toRadians(g.yaw)));
        for (float d = 0; d < 6f; d += 0.1f) {
            int bx = (int) Math.floor(g.camX + dirX * d);
            int by = (int) Math.floor(eye + dirY * d);
            int bz = (int) Math.floor(g.camZ + dirZ * d);
            byte b = getBlock(bx, by, bz);
            if (b > 0 && b != Blocks.WATER && b != Blocks.FIRE) {
                if (out != null) {
                    out[0] = bx;
                    out[1] = by;
                    out[2] = bz;
                }
                return out;
            }
        }
        return null;
    }

    public int[] raycastBlock(Gameplay g) {
        return raycastBlock(g, new int[3]);
    }

    public void spawnItemEntity(float x, float y, float z, byte type) {
        droppedItems.add(new Gameplay().new ItemEntity(x, y, z, type));
    }

    public void spawnItemEntity(float x, float y, float z, byte type, int count) {
        Gameplay.ItemEntity item = new Gameplay().new ItemEntity(x, y, z, type);
        item.count = Math.max(0, count);
        droppedItems.add(item);
    }

    public Gameplay gameplayRef;

    public void setGameplay(Gameplay g) {
        this.gameplayRef = g;
    }
}
