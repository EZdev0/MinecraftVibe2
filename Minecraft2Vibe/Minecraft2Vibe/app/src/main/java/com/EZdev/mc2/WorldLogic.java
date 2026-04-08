package com.EZdev.mc2;

import android.opengl.GLES20;
import android.opengl.Matrix;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Random;

public class WorldLogic {
    private final Random random = new Random();
    private HashMap<String, Chunk> chunks = new HashMap<>();
    private float[] finalMVP = new float[16];
    private float[] modelMatrix = new float[16];

    public int renderDistance = 2;
    public boolean fogEnabled = true;

    private Gameplay gameplayRef;
    public SaveManager saveManager;

    public ArrayList<Entity> entities = new ArrayList<>();

        public class ItemEntity {
        public float x, y, z;
        public byte type;
        public float life = 300.0f;
        public float hoverOffset = 0f;
        public float vy = 0f;
        public int count = 1;
        public float pickupDelay = 0.5f;

        public ItemEntity(float x, float y, float z, byte type) {
            this.x = x; this.y = y; this.z = z; this.type = type; this.count = 1;
        }
        public ItemEntity(float x, float y, float z, byte type, int count) {
            this.x = x; this.y = y; this.z = z; this.type = type; this.count = count;
        }
        public void update(float dt) {
            hoverOffset += dt;
            life -= dt;
            if(pickupDelay > 0) pickupDelay -= dt;
            vy -= 15f * dt;
            float nextY = y + vy * dt;
            if(getBlock((int)Math.floor(x), (int)Math.floor(nextY), (int)Math.floor(z)) == 0) {
                y = nextY;
            } else {
                vy = 0;
                y = (float)Math.floor(nextY) + 1.0f; // Rest on ground precisely
            }

    }
    }

    public ArrayList<ItemEntity> droppedItems = new ArrayList<>();

    public void spawnItemEntity(int x, int y, int z, byte type) {
        spawnItemEntity(x + 0.5f, y + 0.5f, z + 0.5f, type, 1);
    }
    public void spawnItemEntity(float x, float y, float z, byte type, int count) {
        droppedItems.add(new ItemEntity(x, y, z, type, count));
    }

    public void updateChunks(float playerX, float playerZ) {
        int playerChunkX = (int) Math.floor(playerX / 16.0);
        int playerChunkZ = (int) Math.floor(playerZ / 16.0);

        for (int x = -renderDistance; x <= renderDistance; x++) {
            for (int z = -renderDistance; z <= renderDistance; z++) {
                if (x*x + z*z > renderDistance*renderDistance) continue;

                String key = (playerChunkX + x) + "," + (playerChunkZ + z);
                if (!chunks.containsKey(key)) {
                    Chunk newChunk = new Chunk(this, playerChunkX + x, playerChunkZ + z);
                    chunks.put(key, newChunk);
                    updateNeighbors(playerChunkX + x, playerChunkZ + z);

                    if(random.nextFloat() < 0.2f) {
                        float ey = 127;
                        while(ey > 0 && getBlock((playerChunkX+x)*16 + 8, (int)ey, (playerChunkZ+z)*16 + 8) == 0) ey--;
                        if(ey > 50) entities.add(new Entity((playerChunkX+x)*16 + 8, ey+1.1f, (playerChunkZ+z)*16 + 8));
                    }
                    return; // Only generate ONE chunk per frame to avoid severe lag spikes!
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
        // Feature 9: Bedrock cannot be modified
        if (getBlock(x, y, z) == 9 && type == 0) return;

        int cx = (int) Math.floor(x / 16.0);
        int cz = (int) Math.floor(z / 16.0);
        String key = cx + "," + cz;
        Chunk c = chunks.get(key);
        if (c != null) {
            int lx = x - (cx * 16);
            int lz = z - (cz * 16);
            c.blocks[lx][y][lz] = type;
            c.buildMesh();

            if (saveManager != null) saveManager.saveChunk(c);

            if (lx == 0) updateNeighbors(cx, cz);
            if (lx == 15) updateNeighbors(cx, cz);
            if (lz == 0) updateNeighbors(cx, cz);
            if (lz == 15) updateNeighbors(cx, cz);
        }
    }

    public void checkIgnition(int x, int y, int z, Gameplay g) {
        int[][] neighbors = {{1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}};
        for(int[] n : neighbors) {
            int nx = x + n[0];
            int ny = y + n[1];
            int nz = z + n[2];
            if(getBlock(nx, ny, nz) == 5) {
                setBlock(nx, ny, nz, (byte)0);
                Gameplay.ActiveTNT newTNT = g.new ActiveTNT(nx + 0.5f, ny, nz + 0.5f);
                g.tickingTNTs.add(newTNT);
            }
        }
    }

    public void explode(float ex, float ey, float ez, float radius) {
        int minX = (int) Math.floor(ex - radius); int maxX = (int) Math.ceil(ex + radius);
        int minY = (int) Math.floor(ey - radius); int maxY = (int) Math.ceil(ey + radius);
        int minZ = (int) Math.floor(ez - radius); int maxZ = (int) Math.ceil(ez + radius);
        HashSet<Chunk> chunksToUpdate = new HashSet<>();

        if (gameplayRef != null) {
            float distToPlayer = (float)Math.sqrt((gameplayRef.camX-ex)*(gameplayRef.camX-ex) + (gameplayRef.camY-ey)*(gameplayRef.camY-ey) + (gameplayRef.camZ-ez)*(gameplayRef.camZ-ez));
            if(distToPlayer < 25f) {
                gameplayRef.shakeIntensity = (25f - distToPlayer) / 25f;
                gameplayRef.shakeTimer = 0.6f;
            }

            gameplayRef.addExplosionParticles(ex, ey, ez);

            float r2 = radius * 2.0f;
            float r2sq = r2 * r2;

            for (Gameplay.ActiveTNT tnt : gameplayRef.tickingTNTs) {
                float dx = tnt.x - ex;
                float dy = tnt.y - ey;
                float dz = tnt.z - ez;
                float distSq = dx*dx + dy*dy + dz*dz;
                if (distSq > 0 && distSq <= r2sq) {
                    float dist = (float) Math.sqrt(distSq);
                    float force = (r2 - dist) / r2;
                    tnt.vx += (dx / dist) * force * 15.0f;
                    tnt.vy += (dy / dist) * force * 15.0f + force * 5.0f;
                    tnt.vz += (dz / dist) * force * 15.0f;
                }
            }

            for (Entity e : entities) {
                float dx = e.x - ex;
                float dy = e.y - ey;
                float dz = e.z - ez;
                float distSq = dx*dx + dy*dy + dz*dz;
                if (distSq > 0 && distSq <= r2sq) {
                    float dist = (float) Math.sqrt(distSq);
                    float force = (r2 - dist) / r2;
                    e.targetX = e.x + (dx / dist) * force * 20.0f;
                    e.targetZ = e.z + (dz / dist) * force * 20.0f;
                    e.vy += force * 15.0f;
                }
            }
        }

        float radiusSq = radius * radius;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    float dx = x + 0.5f - ex;
                    float dy = y + 0.5f - ey;
                    float dz = z + 0.5f - ez;
                    float distSq = dx*dx + dy*dy + dz*dz;
                    if (distSq <= radiusSq && y >= 1 && y < 128) {
                        byte block = getBlock(x, y, z);
                        if (block == 9) continue; // Bedrock immune

                        if (block == 5 && gameplayRef != null) {
                            setBlock(x, y, z, (byte)0);
                            Gameplay.ActiveTNT newTNT = gameplayRef.new ActiveTNT(x + 0.5f, y + 0.5f, z + 0.5f);
                            float dist = (float) Math.sqrt(distSq);
                            float force = (radius - dist) / radius;
                            newTNT.vx = (dx / dist) * force * 15.0f;
                            newTNT.vy = (dy / dist) * force * 15.0f + 5.0f;
                            newTNT.vz = (dz / dist) * force * 15.0f;
                            newTNT.timer = 0.5f + random.nextFloat() * 1.5f;
                            gameplayRef.tickingTNTs.add(newTNT);
                        } else if (block != 0 && block != 7) {
                            if (gameplayRef != null && random.nextFloat() < 0.2f) gameplayRef.addBlockParticles(x, y, z, block);

                            // Explosion drops some blocks in survival
                            if (gameplayRef != null && !gameplayRef.isCreative && random.nextFloat() < 0.3f) {
                                spawnItemEntity(x, y, z, block);
                            }

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
        for (Chunk c : chunksToUpdate) {
            c.buildMesh();
            if(saveManager != null) saveManager.saveChunk(c);
        }
    }

    public void render(float[] vpMatrix, Gameplay gameplay) {

        if (gameplay == null || vpMatrix == null) return;
        if (gameplay.tickingTNTs == null) gameplay.tickingTNTs = new ArrayList<>();
        if (gameplay.fireParticles == null) gameplay.fireParticles = new ArrayList<>();
        if (gameplay.blockParticles == null) gameplay.blockParticles = new ArrayList<>();
        this.gameplayRef = gameplay;

        float dt = 0.016f;

        float dayCycle = (float)(Math.sin(gameplay.gameTime * 0.05f) * 0.5f + 0.5f);
        GLES20.glClearColor(0.1f + dayCycle*0.4f, 0.2f + dayCycle*0.6f, 0.4f + dayCycle*0.6f, 1.0f);

        GLES20.glUseProgram(Booster.shaderProgram);
        GLES20.glEnableVertexAttribArray(Booster.posHandle);
        GLES20.glEnableVertexAttribArray(Booster.colorHandle);

        GLES20.glUniform1i(Booster.fogEnabledHandle, fogEnabled ? 1 : 0);
        GLES20.glUniform1f(Booster.fogEndHandle, renderDistance * 16.0f);
        GLES20.glUniform1i(Booster.isFlashingHandle, 0);
        GLES20.glUniform1i(Booster.pTypeHandle, 0);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        float camChunkX = gameplay.camX / 16.0f;
        float camChunkZ = gameplay.camZ / 16.0f;
        float renderRadiusSq = (renderDistance + 1) * (renderDistance + 1);

        for (Map.Entry<String, Chunk> entry : chunks.entrySet()) {
            Chunk c = entry.getValue();
            if (c.vertexBuffer == null || c.vertexCount == 0) continue;

            float dx = c.chunkX - camChunkX;
            float dz = c.chunkZ - camChunkZ;
            if (dx*dx + dz*dz > renderRadiusSq) continue;

            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, c.chunkX * 16, 0, c.chunkZ * 16);
            Matrix.multiplyMM(finalMVP, 0, vpMatrix, 0, modelMatrix, 0);
            GLES20.glUniformMatrix4fv(Booster.mvpHandle, 1, false, finalMVP, 0);
            GLES20.glUniform1i(Booster.pTypeHandle, 0);
            c.vertexBuffer.position(0);
            GLES20.glVertexAttribPointer(Booster.posHandle, 3, GLES20.GL_FLOAT, false, 0, c.vertexBuffer);
            c.colorBuffer.position(0);
            GLES20.glVertexAttribPointer(Booster.colorHandle, 4, GLES20.GL_FLOAT, false, 0, c.colorBuffer);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, c.vertexCount);
        }

        GLES20.glDisable(GLES20.GL_BLEND);

        if (Booster.tntVertexBuffer != null) {
            GLES20.glUniform1i(Booster.pTypeHandle, 100);
            for(int i=0; i<entities.size(); i++) {
                Entity e = entities.get(i);
                if (e == null) { int lastIdx = entities.size() - 1; if (i < lastIdx) entities.set(i, entities.get(lastIdx)); entities.remove(lastIdx); continue; }
                Matrix.setIdentityM(modelMatrix, 0);
                Matrix.translateM(modelMatrix, 0, e.x - 0.4f, e.y, e.z - 0.4f);
                Matrix.scaleM(modelMatrix, 0, 0.8f, 0.8f, 0.8f);
                Matrix.multiplyMM(finalMVP, 0, vpMatrix, 0, modelMatrix, 0);
                GLES20.glUniformMatrix4fv(Booster.mvpHandle, 1, false, finalMVP, 0);
                GLES20.glVertexAttribPointer(Booster.posHandle, 3, GLES20.GL_FLOAT, false, 0, Booster.tntVertexBuffer);
                GLES20.glVertexAttribPointer(Booster.colorHandle, 4, GLES20.GL_FLOAT, false, 0, Booster.tntColorBuffer);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
            }

            for(int i=droppedItems.size()-1; i>=0; i--) {
                ItemEntity item = droppedItems.get(i);
                if (item == null) { int lastIdx = droppedItems.size() - 1; if (i < lastIdx) droppedItems.set(i, droppedItems.get(lastIdx)); droppedItems.remove(lastIdx); continue; }
                item.update(dt);

                if (item.life <= 0) {
                    int lastIdx = droppedItems.size() - 1; if (i < lastIdx) droppedItems.set(i, droppedItems.get(lastIdx)); droppedItems.remove(lastIdx);
                    continue;
                }


                // Item Stacking (O(n) per item, but small lists and max 64)
                for(int j=0; j<droppedItems.size(); j++) {
                    if (i != j) {
                        ItemEntity other = droppedItems.get(j);
                        if(other != null && other.type == item.type && item.count < 64 && other.count < 64) {
                            float dxi = item.x - other.x; float dyi = item.y - other.y; float dzi = item.z - other.z;
                            if(dxi*dxi + dyi*dyi + dzi*dzi < 0.25f) {
                                int space = 64 - item.count; int transfer = Math.min(space, other.count);
                                item.count += transfer; other.count -= transfer;
                                if(other.count <= 0) other.life = 0;
                            }
                        }
                    }
                }




                float playerCenterY = gameplay.camY - (gameplay.playerHeight / 2.0f);
                float dx = item.x - gameplay.camX;
                float dy = item.y - playerCenterY;
                float dz = item.z - gameplay.camZ;
                float distanceSq = dx*dx + dy*dy + dz*dz;

                if (item.pickupDelay <= 0 && !gameplay.isCreative) {
                    if (distanceSq < 6.0f) { // ~2.5 blocks magnet
                        float dist = (float)Math.sqrt(distanceSq);

                        // Disable gravity while magnetized
                        item.vy = 0;

                        // Fly towards player
                        item.x -= (dx / dist) * 10.0f * dt;
                        item.y -= (dy / dist) * 10.0f * dt;
                        item.z -= (dz / dist) * 10.0f * dt;

                        // True Hitbox AABB check for pickup
                        float pMinX = gameplay.camX - (gameplay.playerWidth / 2f);
                        float pMaxX = gameplay.camX + (gameplay.playerWidth / 2f);
                        float pMinY = gameplay.camY;
                        float pMaxY = gameplay.camY + gameplay.playerHeight + 0.2f; // Little tolerance above head
                        float pMinZ = gameplay.camZ - (gameplay.playerWidth / 2f);
                        float pMaxZ = gameplay.camZ + (gameplay.playerWidth / 2f);

                        // Item size ~0.3
                        float iMinX = item.x - 0.15f; float iMaxX = item.x + 0.15f;
                        float iMinY = item.y - 0.15f; float iMaxY = item.y + 0.15f;
                        float iMinZ = item.z - 0.15f; float iMaxZ = item.z + 0.15f;

                        boolean intersect = (pMinX <= iMaxX && pMaxX >= iMinX) &&
                                            (pMinY <= iMaxY && pMaxY >= iMinY) &&
                                            (pMinZ <= iMaxZ && pMaxZ >= iMinZ);

                        if(intersect) {
                            if(gameplay.activity != null && gameplay.activity.uiManager != null) {
                                int overflow = gameplay.activity.uiManager.addToInventory(item.type, item.count);
                                if (overflow > 0) {
                                    item.count = overflow;
                                } else {
                                    item.life = 0;
                                    int lastIdx = droppedItems.size() - 1;
                                    if (i < lastIdx) droppedItems.set(i, droppedItems.get(lastIdx));
                                    droppedItems.remove(lastIdx);
                                    continue;
                                }
                            }
                        }
                    }
                }






                Matrix.setIdentityM(modelMatrix, 0);
                Matrix.translateM(modelMatrix, 0, item.x - 0.15f, item.y + (float)Math.sin(item.hoverOffset*3f)*0.1f, item.z - 0.15f);
                Matrix.rotateM(modelMatrix, 0, item.hoverOffset * 50f, 0, 1, 0);
                Matrix.scaleM(modelMatrix, 0, 0.3f, 0.3f, 0.3f);
                Matrix.multiplyMM(finalMVP, 0, vpMatrix, 0, modelMatrix, 0);
                GLES20.glUniformMatrix4fv(Booster.mvpHandle, 1, false, finalMVP, 0);
                GLES20.glUniform1i(Booster.pTypeHandle, item.type);
                GLES20.glVertexAttribPointer(Booster.posHandle, 3, GLES20.GL_FLOAT, false, 0, Booster.tntVertexBuffer);
                GLES20.glVertexAttribPointer(Booster.colorHandle, 4, GLES20.GL_FLOAT, false, 0, Booster.tntColorBuffer);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
            }

            GLES20.glUniform1i(Booster.isFlashingHandle, 1);
            GLES20.glUniform1i(Booster.pTypeHandle, 5);
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
            GLES20.glUniform1i(Booster.isFlashingHandle, 0);

            for(int i=0; i<gameplay.fireParticles.size(); i++) {
                Gameplay.ActiveFireParticle p = gameplay.fireParticles.get(i);
                if (p != null) {
                    float size = p.type == 99 ? 0.3f : 0.15f;
                    renderParticle(p, vpMatrix, size, p.type);
                }
            }
            for(int i=0; i<gameplay.blockParticles.size(); i++) {
                Gameplay.ActiveFireParticle bp = gameplay.blockParticles.get(i);
                if(bp != null) {
                    renderParticle(bp, vpMatrix, 0.1f, bp.type);
                }
            }
        }
        GLES20.glDisableVertexAttribArray(Booster.posHandle);
        GLES20.glDisableVertexAttribArray(Booster.colorHandle);
    }

    private void renderParticle(Gameplay.ActiveFireParticle p, float[] vpMatrix, float size, byte type) {
        if(p == null) return;
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, p.x, p.y, p.z);
        Matrix.scaleM(modelMatrix, 0, size, size, size);
        Matrix.multiplyMM(finalMVP, 0, vpMatrix, 0, modelMatrix, 0);
        GLES20.glUniformMatrix4fv(Booster.mvpHandle, 1, false, finalMVP, 0);
        GLES20.glVertexAttribPointer(Booster.posHandle, 3, GLES20.GL_FLOAT, false, 0, Booster.tntVertexBuffer);

        GLES20.glUniform1i(Booster.pTypeHandle, type);
        if (type == 99) GLES20.glUniform1i(Booster.isFlashingHandle, 2);

        GLES20.glVertexAttribPointer(Booster.colorHandle, 4, GLES20.GL_FLOAT, false, 0, Booster.tntColorBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
        GLES20.glUniform1i(Booster.isFlashingHandle, 0);
    }

    public int[] raycastBlock(Gameplay g) {
        float eyeHeight = g.camY + g.playerHeight - 0.2f;
        float dirX = (float) (Math.cos(Math.toRadians(g.pitch)) * Math.sin(Math.toRadians(g.yaw)));
        float dirY = (float) Math.sin(Math.toRadians(g.pitch));
        float dirZ = (float) (-Math.cos(Math.toRadians(g.pitch)) * Math.cos(Math.toRadians(g.yaw)));

        for(float dist = 0; dist < 6f; dist += 0.1f) {
            int bx = (int) Math.floor(g.camX + dirX * dist);
            int by = (int) Math.floor(eyeHeight + dirY * dist);
            int bz = (int) Math.floor(g.camZ + dirZ * dist);
            byte hitBlock = getBlock(bx, by, bz);
            if (hitBlock > 0 && hitBlock != 6 && hitBlock != 7) {
                return new int[]{bx, by, bz};
            }
        }
        return null;
    }

    public void interact(Gameplay g, boolean place, UIManager ui) {
        if (g == null) return;
        if (entities == null) entities = new ArrayList<>();
        if (g == null) return;
        float eyeHeight = g.camY + g.playerHeight - 0.2f;
        float dirX = (float) (Math.cos(Math.toRadians(g.pitch)) * Math.sin(Math.toRadians(g.yaw)));
        float dirY = (float) Math.sin(Math.toRadians(g.pitch));
        float dirZ = (float) (-Math.cos(Math.toRadians(g.pitch)) * Math.cos(Math.toRadians(g.yaw)));
        int lastX = -1, lastY = -1, lastZ = -1;

        for(float dist = 0; dist < 6f; dist += 0.1f) {
            int bx = (int) Math.floor(g.camX + dirX * dist);
            int by = (int) Math.floor(eyeHeight + dirY * dist);
            int bz = (int) Math.floor(g.camZ + dirZ * dist);

            for(int i=entities.size()-1; i>=0; i--) {
                Entity e = entities.get(i);
                if (e == null) { int lastIdx = entities.size() - 1; if (i < lastIdx) entities.set(i, entities.get(lastIdx)); entities.remove(lastIdx); continue; }
                if(bx == (int)Math.floor(e.x) && by == (int)Math.floor(e.y) && bz == (int)Math.floor(e.z)) {
                    if(!place) {
                        int lastIdx = entities.size() - 1; if (i < lastIdx) entities.set(i, entities.get(lastIdx)); entities.remove(lastIdx);
                        return;
                    }
                }
            }

            byte hitBlock = getBlock(bx, by, bz);
            if (hitBlock > 0 && hitBlock != 6 && hitBlock != 7) {
                if (hitBlock == 9) return; // Cannot interact with Bedrock

                if (!place && hitBlock == 5 && g.activeBlock == 6) {
                    setBlock(bx, by, bz, (byte)0);
                    g.tickingTNTs.add(g.new ActiveTNT(bx + 0.5f, by, bz + 0.5f));
                    return;
                }
                if (place && lastX != -1 && !isPlayerInside(g, lastX, lastY, lastZ)) {
                    if (!g.isCreative && ui != null) {
                        int slot = -1;
                        for(int s=0; s<ui.blockIds.length; s++) if(ui.blockIds[s] == g.activeBlock) slot = s;
                        if(slot != -1 && ui.inventory[slot] <= 0) return;
                        if(slot != -1 && ui.inventory[slot] != 999) {
                            ui.inventory[slot]--;
                            ui.updateHotbarUI();
                        }
                    }

                    setBlock(lastX, lastY, lastZ, g.activeBlock);
                    if(g.activeBlock == 6) {
                        checkIgnition(lastX, lastY, lastZ, g);
                        g.activeFires.add(g.new ActiveFire(lastX, lastY, lastZ));
                    }
                    if (g.activeBlock == 5) {
                        checkIgnitionIfTntPlaced(lastX, lastY, lastZ, g);
                    }
                } else if (!place) {
                    if (g.isCreative) {
                        g.addBlockParticles(bx, by, bz, hitBlock);
                        setBlock(bx, by, bz, (byte)0);
                    } else {
                        // Mark as target
                        g.isBreaking = true;
                        g.targetX = bx; g.targetY = by; g.targetZ = bz;
                    }
                }
                return;
            }
            lastX = bx; lastY = by; lastZ = bz;
        }
        if(!place) {
            g.isBreaking = false;
        }
    }

    private void checkIgnitionIfTntPlaced(int x, int y, int z, Gameplay g) {
        int[][] neighbors = {{1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}};
        for(int[] n : neighbors) {
            int nx = x + n[0];
            int ny = y + n[1];
            int nz = z + n[2];
            if(getBlock(nx, ny, nz) == 6) {
                setBlock(x, y, z, (byte)0);
                Gameplay.ActiveTNT newTNT = g.new ActiveTNT(x + 0.5f, y, z + 0.5f);
                g.tickingTNTs.add(newTNT);
                return;
            }
        }
    }

    private boolean isPlayerInside(Gameplay g, int bx, int by, int bz) {
        if (g == null) return false;
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
