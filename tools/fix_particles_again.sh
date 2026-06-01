#!/bin/bash
cat << 'INNER_EOF' > Minecraft2Vibe/Minecraft2Vibe/app/src/main/java/com/EZdev/mc2/Gameplay.java
package com.EZdev.mc2;

import java.util.ArrayList;

public class Gameplay {
    public float camX = 8f, camY = 100f, camZ = 8f;
    public float yaw = 0f, pitch = 0f;
    public float joyMoveX = 0f, joyMoveY = 0f;

    public float velocityY = 0f;
    public boolean onGround = false;
    private boolean hasSpawned = false;

    public boolean wantsToJump = false;

    public boolean isSneaking = false;
    public boolean isFlying = false;
    public boolean isSprinting = false;

    public float shakeIntensity = 0f;
    public float shakeTimer = 0f;

    public float playerHeight = 1.8f;
    public float playerWidth = 0.6f;

    public byte activeBlock = 1;
    public float gameTime = 0f;

    public ArrayList<ActiveTNT> tickingTNTs = new ArrayList<>();
    public ArrayList<ActiveFire> activeFires = new ArrayList<>();
    public ArrayList<ActiveFireParticle> fireParticles = new ArrayList<>();
    public ArrayList<ActiveFireParticle> blockParticles = new ArrayList<>();

    public class ActiveTNT {
        public float x, y, z;
        public float vx = 0f, vy = 0f, vz = 0f;
        public float timer = 3.0f;
        public ActiveTNT(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
    }

    public class ActiveFire {
        public int x, y, z;
        public float life;
        public float spreadTimer;
        public ActiveFire(int x, int y, int z) {
            this.x = x; this.y = y; this.z = z;
            this.life = 5.0f + (float)Math.random() * 5.0f;
            this.spreadTimer = 0.5f + (float)Math.random() * 1.5f;
        }
    }

    public class ActiveFireParticle {
        public float x, y, z;
        public float vx, vy, vz;
        public float life;
        public byte type;
        public ActiveFireParticle(float x, float y, float z, byte type) {
            this.x = x; this.y = y; this.z = z;
            this.vx = ((float)Math.random() - 0.5f) * 6f;
            this.vy = ((float)Math.random()) * 6f + 3f;
            this.vz = ((float)Math.random() - 0.5f) * 6f;
            this.life = 0.8f + (float)Math.random() * 2.0f;
            this.type = type;
        }
        public ActiveFireParticle(float x, float y, float z, float intensity) {
            this.x = x; this.y = y; this.z = z;
            this.vx = ((float)Math.random() - 0.5f) * intensity;
            this.vy = ((float)Math.random() - 0.5f) * intensity;
            this.vz = ((float)Math.random() - 0.5f) * intensity;
            this.life = 1.0f + (float)Math.random() * 3.0f;
            this.type = 99; // 99 means Smoke (White/Grey)
        }
    }

    public void addBlockParticles(float x, float y, float z, byte blockType) {
        for(int i=0; i<15; i++) {
            blockParticles.add(new ActiveFireParticle(x+0.5f, y+0.5f, z+0.5f, blockType));
        }
    }

    public void addExplosionParticles(float x, float y, float z) {
        for(int i=0; i<25; i++) { // Reduced count a bit, but now it's smoke
            fireParticles.add(new ActiveFireParticle(x, y, z, 20f));
        }
    }

    public void update(float dt, WorldLogic world) {
        if (!hasSpawned) { spawnOnHighestBlock(world); return; }
        if (dt > 0.05f) dt = 0.05f;

        gameTime += dt;

        if (shakeTimer > 0) {
            shakeTimer -= dt;
            if (shakeTimer <= 0) shakeIntensity = 0;
        } else {
            shakeIntensity = 0;
        }

        // --- TNT UPDATE ---
        for (int i = tickingTNTs.size() - 1; i >= 0; i--) {
            ActiveTNT tnt = tickingTNTs.get(i);
            tnt.timer -= dt;
            tnt.vy -= 10.0f * dt;

            float nextY = tnt.y + tnt.vy * dt;
            if(!checkCollisionPoint(world, tnt.x, nextY, tnt.z)) { tnt.y = nextY; }
            else { tnt.vy = 0f; tnt.vx *= 0.5f; tnt.vz *= 0.5f; }

            float nextX = tnt.x + tnt.vx * dt;
            if(!checkCollisionPoint(world, nextX, tnt.y, tnt.z)) { tnt.x = nextX; } else { tnt.vx = 0f; }

            float nextZ = tnt.z + tnt.vz * dt;
            if(!checkCollisionPoint(world, tnt.x, tnt.y, nextZ)) { tnt.z = nextZ; } else { tnt.vz = 0f; }

            if (tnt.timer <= 0) {
                world.explode(tnt.x, tnt.y, tnt.z, 4.0f);
                tickingTNTs.remove(i);
            }
        }

        // --- PARTICLES UPDATE ---
        updateParticles(fireParticles, dt, world, true);
        updateParticles(blockParticles, dt, world, false);

        // --- FIRE LOGIC UPDATE ---
        for (int i = activeFires.size() - 1; i >= 0; i--) {
            ActiveFire fire = activeFires.get(i);

            if(world.getBlock(fire.x, fire.y, fire.z) != 6) {
                activeFires.remove(i);
                continue;
            }

            boolean hasSupport = false;
            int[][] neighbors = {{1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}};
            for(int[] n : neighbors) {
                if (world.getBlock(fire.x+n[0], fire.y+n[1], fire.z+n[2]) > 0) hasSupport = true;
            }
            if(!hasSupport) {
                world.setBlock(fire.x, fire.y, fire.z, (byte)0);
                activeFires.remove(i);
                continue;
            }

            fire.life -= dt;
            fire.spreadTimer -= dt;

            // Significantly reduced fire particle spawn rate (0.01 instead of 0.1) so it doesn't "snow"
            if (Math.random() < 0.01f) {
                fireParticles.add(new ActiveFireParticle(fire.x + 0.5f, fire.y + 0.5f, fire.z + 0.5f, (byte)6));
            }

            if (fire.life <= 0) {
                world.setBlock(fire.x, fire.y, fire.z, (byte)0);
                activeFires.remove(i);
                continue;
            }

            if (fire.spreadTimer <= 0) {
                fire.spreadTimer = 0.5f + (float)Math.random() * 1.0f;
                for (int attempts = 0; attempts < 3; attempts++) {
                    int dx = (int)(Math.random() * 3) - 1;
                    int dy = (int)(Math.random() * 6) - 1;
                    int dz = (int)(Math.random() * 3) - 1;
                    int nx = fire.x + dx; int ny = fire.y + dy; int nz = fire.z + dz;
                    byte blockType = world.getBlock(nx, ny, nz);
                    if ((blockType == 3 || blockType == 4)) {
                        float distancePenalty = (Math.abs(dx) + Math.abs(dy) + Math.abs(dz)) * 0.15f;
                        float baseChance = (blockType == 4) ? 0.9f : 0.6f;
                        if (Math.random() < baseChance - distancePenalty) {
                            world.setBlock(nx, ny, nz, (byte)6);
                            activeFires.add(new ActiveFire(nx, ny, nz));
                        }
                    }
                }
            }
        }

        if (Math.random() < 0.1f) {
            int px = (int)camX; int py = (int)camY-2; int pz = (int)camZ;
            byte b = world.getBlock(px, py, pz);
            if (b == 8 && world.getBlock(px, py-1, pz) == 0) {
                world.setBlock(px, py, pz, (byte)0);
                world.setBlock(px, py-1, pz, (byte)8);
            }
        }

        // --- PLAYER MOVEMENT ---
        boolean inWater = false;
        byte blockAtFeet = world.getBlock((int)Math.floor(camX), (int)Math.floor(camY), (int)Math.floor(camZ));
        byte blockAtHead = world.getBlock((int)Math.floor(camX), (int)Math.floor(camY + 1.5f), (int)Math.floor(camZ));
        if (blockAtFeet == 7 || blockAtHead == 7) inWater = true;

        playerHeight = isSneaking ? 1.5f : 1.8f;

        if (checkCollision(world, camX, camY, camZ)) { camY += 2.5f * dt; }

        if (wantsToJump) {
            if (isFlying) {
                velocityY = 10.0f;
            } else if (inWater) {
                velocityY = 4.0f;
            } else if (onGround) {
                velocityY = 8.5f;
                onGround = false;
            }
        } else if (isFlying) {
            if(isSneaking) velocityY = -10.0f;
            else velocityY = 0f;
        }

        if (!isFlying) {
            if (inWater) {
                if(!wantsToJump) velocityY -= 5f * dt;
                if (velocityY < -3f) velocityY = -3f;
            } else {
                velocityY -= 25f * dt;
                if (velocityY < -20f) velocityY = -20f;
            }
        }

        float nextY = camY + velocityY * dt;
        if (!checkCollision(world, camX, nextY, camZ)) {
            camY = nextY;
            onGround = false;
        } else {
            if (velocityY < 0) {
                onGround = true;
                camY = (float) Math.floor(nextY) + 1.001f;
            } else {
                camY = (float) Math.floor(nextY + playerHeight) - playerHeight - 0.001f;
            }
            velocityY = 0;
        }

        float speed = 5.0f * dt;
        if (inWater) speed = 2.5f * dt;
        else if (isSprinting) speed = 8.0f * dt;
        else if (isSneaking) speed = 2.0f * dt;
        if (isFlying) speed = 10.0f * dt;

        float yawRad = (float)Math.toRadians(yaw);
        float sinYaw = (float) Math.sin(yawRad);
        float cosYaw = (float) Math.cos(yawRad);

        float dirX = sinYaw;
        float dirZ = -cosYaw;
        float rightX = cosYaw;
        float rightZ = sinYaw;

        float moveX = (dirX * -joyMoveY + rightX * joyMoveX) * speed;
        float moveZ = (dirZ * -joyMoveY + rightZ * joyMoveX) * speed;

        if (onGround && (joyMoveX != 0 || joyMoveY != 0)) {
            if (checkCollision(world, camX + moveX, camY, camZ) || checkCollision(world, camX, camY, camZ + moveZ)) {
                if (!checkCollision(world, camX + moveX, camY + 1.1f, camZ) && !checkCollision(world, camX, camY + 1.1f, camZ + moveZ)) {
                    camY += 1.1f;
                }
            }
        }

        if (onGround && isSneaking && !inWater) {
            if (!checkCollision(world, camX + moveX, camY - 0.5f, camZ)) moveX = 0;
            if (!checkCollision(world, camX, camY - 0.5f, camZ + moveZ)) moveZ = 0;
        }

        if (!checkCollision(world, camX + moveX, camY, camZ)) camX += moveX;
        if (!checkCollision(world, camX, camY, camZ + moveZ)) camZ += moveZ;

        if (camY < -20f) { hasSpawned = false; camY = 100f; velocityY = 0; }
    }

    private void updateParticles(ArrayList<ActiveFireParticle> list, float dt, WorldLogic world, boolean isFire) {
        for(int i = list.size() - 1; i >= 0; i--) {
            ActiveFireParticle p = list.get(i);
            p.life -= dt;
            p.vy -= 15.0f * dt;
            p.x += p.vx * dt; p.y += p.vy * dt; p.z += p.vz * dt;

            if (p.life <= 0) { list.remove(i); continue; }

            if(checkCollisionPoint(world, p.x, p.y, p.z)) {
                if (isFire && p.type == 6) { // Only actual fire particles ignite, not smoke (99)
                    byte block = world.getBlock((int)Math.floor(p.x), (int)Math.floor(p.y), (int)Math.floor(p.z));
                    if (block == 3 || block == 4) {
                        if (Math.random() < 0.3f) {
                            world.setBlock((int)Math.floor(p.x), (int)Math.floor(p.y), (int)Math.floor(p.z), (byte)6);
                            activeFires.add(new ActiveFire((int)Math.floor(p.x), (int)Math.floor(p.y), (int)Math.floor(p.z)));
                        }
                    }
                } else if (!isFire) {
                    p.vy *= -0.3f;
                    p.vx *= 0.5f; p.vz *= 0.5f;
                    p.y += 0.1f;
                    if(Math.abs(p.vy) < 0.1f) { p.vx = 0; p.vz = 0; }
                }

                // Smoke (99) also gets removed when it hits ground, but it bounces a bit first if we want.
                // Let's just remove it for simplicity.
                if(isFire) list.remove(i);
            }
        }
    }

    public void jump() { wantsToJump = true; }

    private boolean checkCollisionPoint(WorldLogic world, float x, float y, float z) {
        if (world == null) return false;
        byte block = world.getBlock((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
        return (block > 0 && block != 6 && block != 7);
    }

    private boolean checkCollision(WorldLogic world, float x, float y, float z) {
        if (world == null) return false;
        float shrink = 0.01f;
        int minX = (int) Math.floor(x - playerWidth / 2f + shrink);
        int maxX = (int) Math.floor(x + playerWidth / 2f - shrink);
        int minY = (int) Math.floor(y);
        int maxY = (int) Math.floor(y + playerHeight - shrink);
        int minZ = (int) Math.floor(z - playerWidth / 2f + shrink);
        int maxZ = (int) Math.floor(z + playerWidth / 2f - shrink);

        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    byte block = world.getBlock(bx, by, bz);
                    if (block > 0 && block != 6 && block != 7) return true;
                }
            }
        }
        return false;
    }

    private void spawnOnHighestBlock(WorldLogic world) {
        if (world == null) return;
        int cx = (int) Math.floor(camX);
        int cz = (int) Math.floor(camZ);
        for (int y = 127; y > 0; y--) {
            byte b = world.getBlock(cx, y, cz);
            if (b > 0 && b != 7) {
                camY = y + 1.001f;
                hasSpawned = true;
                return;
            }
        }
    }
}
INNER_EOF
