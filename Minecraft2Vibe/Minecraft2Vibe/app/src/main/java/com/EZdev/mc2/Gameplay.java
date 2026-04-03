package com.EZdev.mc2;

import java.util.ArrayList;

public class Gameplay {
    public float camX = 8f, camY = 100f, camZ = 8f;
    public float yaw = 0f, pitch = 0f;
    public float joyMoveX = 0f, joyMoveY = 0f;

    public float velocityY = 0f;
    public boolean onGround = false;
    private boolean hasSpawned = false;
    private boolean wantsToJump = false;

    public float playerWidth = 0.6f;
    public float playerHeight = 1.8f;

    public byte activeBlock = 1;
    public float gameTime = 0f;

    public ArrayList<ActiveTNT> tickingTNTs = new ArrayList<>();
    public ArrayList<ActiveFire> activeFires = new ArrayList<>();

    // Static arrays to avoid allocation in game loop
    private static final int[][] FIRE_NEIGHBORS = {
        {1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1},
        {1,1,0}, {-1,-1,0}, {0,1,1}, {0,-1,-1}, {1,0,1}, {-1,0,-1}
    };

    public class ActiveTNT {
        public float x, y, z;
        public float vx = 0f, vy = 0f, vz = 0f;
        public float timer = 3.0f;
        public ActiveTNT(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
    }

    public class ActiveFire {
        public int x, y, z;
        public float life; // Total time this fire block exists
        public float spreadTimer; // Time until next spread attempt
        public ActiveFire(int x, int y, int z) {
            this.x = x; this.y = y; this.z = z;
            this.life = 4.0f + (float)Math.random() * 4.0f; // Burns for 4 to 8 seconds
            this.spreadTimer = 1.0f + (float)Math.random() * 1.5f; // Attempts to spread every 1-2.5 seconds
        }
    }

    public void update(float dt, WorldLogic world) {
        if (!hasSpawned) { spawnOnHighestBlock(world); return; }
        if (dt > 0.05f) dt = 0.05f;

        gameTime += dt;

        // TNT Countdown and Movement
        for (int i = tickingTNTs.size() - 1; i >= 0; i--) {
            ActiveTNT tnt = tickingTNTs.get(i);
            tnt.timer -= dt;

            // TNT Gravity and Velocity
            tnt.vy -= 10.0f * dt;

            float nextY = tnt.y + tnt.vy * dt;
            if(!checkCollisionPoint(world, tnt.x, nextY, tnt.z)) {
                tnt.y = nextY;
            } else {
                tnt.vy = 0f;
                tnt.vx *= 0.5f;
                tnt.vz *= 0.5f;
            }

            float nextX = tnt.x + tnt.vx * dt;
            if(!checkCollisionPoint(world, nextX, tnt.y, tnt.z)) {
                tnt.x = nextX;
            } else {
                tnt.vx = 0f;
            }

            float nextZ = tnt.z + tnt.vz * dt;
            if(!checkCollisionPoint(world, tnt.x, tnt.y, nextZ)) {
                tnt.z = nextZ;
            } else {
                tnt.vz = 0f;
            }

            if (tnt.timer <= 0) {
                world.explode(tnt.x, tnt.y, tnt.z, 4.0f);
                tickingTNTs.remove(i);
            }
        }

        // Fire Logic Update
        for (int i = activeFires.size() - 1; i >= 0; i--) {
            ActiveFire fire = activeFires.get(i);
            // Check if it's still a fire block
            if(world.getBlock(fire.x, fire.y, fire.z) != 6) {
                activeFires.remove(i);
                continue;
            }

            fire.life -= dt;
            fire.spreadTimer -= dt;

            if (fire.life <= 0) {
                // Fire burns out, destroying the block (or just removing itself if the block was already air/fire)
                world.setBlock(fire.x, fire.y, fire.z, (byte)0);
                activeFires.remove(i);
                continue;
            }

            if (fire.spreadTimer <= 0) {
                // Reset spread timer for next attempt
                fire.spreadTimer = 1.0f + (float)Math.random() * 1.5f;

                // Attempt to spread to nearby wood or leaves
                for(int[] n : FIRE_NEIGHBORS) {
                    int nx = fire.x + n[0];
                    int ny = fire.y + n[1];
                    int nz = fire.z + n[2];

                    byte blockType = world.getBlock(nx, ny, nz);
                    // 3 is wood, 4 is leaves
                    if ((blockType == 3 || blockType == 4)) {
                        // Leaves catch fire easily (50%), wood is harder (20%)
                        float spreadChance = (blockType == 4) ? 0.5f : 0.2f;

                        if (Math.random() < spreadChance) {
                            // Ignite the block. The original fire STAYS, allowing the fire to multiply!
                            world.setBlock(nx, ny, nz, (byte)6);
                            activeFires.add(new ActiveFire(nx, ny, nz));
                        }
                    }
                }
            }
        }

        if (checkCollision(world, camX, camY, camZ)) { camY += 2.5f * dt; }

        if (wantsToJump && onGround) { velocityY = 8.5f; onGround = false; }
        wantsToJump = false;

        velocityY -= 25f * dt;
        if (velocityY < -20f) velocityY = -20f;

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

        // Cache trigonometric functions
        float yawRad = (float)Math.toRadians(yaw);
        float sinYaw = (float) Math.sin(yawRad);
        float cosYaw = (float) Math.cos(yawRad);

        float dirX = sinYaw;
        float dirZ = -cosYaw;
        float rightX = cosYaw;
        float rightZ = sinYaw;

        float moveX = (dirX * -joyMoveY + rightX * joyMoveX) * speed;
        float moveZ = (dirZ * -joyMoveY + rightZ * joyMoveX) * speed;

        if (!checkCollision(world, camX + moveX, camY, camZ)) camX += moveX;
        if (!checkCollision(world, camX, camY, camZ + moveZ)) camZ += moveZ;

        if (camY < -20f) { hasSpawned = false; camY = 100f; velocityY = 0; }
    }

    public void jump() { wantsToJump = true; }

    private boolean checkCollisionPoint(WorldLogic world, float x, float y, float z) {
        byte block = world.getBlock((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
        return (block > 0 && block != 6);
    }

    private boolean checkCollision(WorldLogic world, float x, float y, float z) {
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
                    if (block > 0 && block != 6) return true;
                }
            }
        }
        return false;
    }

    private void spawnOnHighestBlock(WorldLogic world) {
        int cx = (int) Math.floor(camX);
        int cz = (int) Math.floor(camZ);
        for (int y = 127; y > 0; y--) {
            if (world.getBlock(cx, y, cz) > 0) {
                camY = y + 1.001f;
                hasSpawned = true;
                return;
            }
        }
    }
}
