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

    public class ActiveTNT {
        public float x, y, z;
        public float vx = 0f, vy = 0f, vz = 0f; // New Velocity
        public float timer = 3.0f;
        public ActiveTNT(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
    }

    public class ActiveFire {
        public int x, y, z;
        public float timer;
        public ActiveFire(int x, int y, int z) {
            this.x = x; this.y = y; this.z = z;
            this.timer = 1.0f + (float)Math.random() * 2.0f; // Fire lasts between 1 and 3 seconds
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
                tnt.y = (float) Math.floor(nextY) + 1.0f; // Snap to block surface
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

            fire.timer -= dt;
            if (fire.timer <= 0) {
                // Time to spread or destroy
                int[][] neighbors = {
                    {1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1},
                    {1,1,0}, {-1,-1,0}, {0,1,1}, {0,-1,-1}, {1,0,1}, {-1,0,-1} // And some diagonals
                };

                boolean spread = false;
                for(int[] n : neighbors) {
                    int nx = fire.x + n[0];
                    int ny = fire.y + n[1];
                    int nz = fire.z + n[2];

                    if (Math.random() < 0.3f && world.getBlock(nx, ny, nz) == 3) { // 3 is wood
                        // Turn wood to fire
                        world.setBlock(nx, ny, nz, (byte)6);
                        activeFires.add(new ActiveFire(nx, ny, nz));
                        spread = true;
                    }
                }

                // Destroy original wood block under/around it, or burn out
                if (Math.random() < 0.5f || spread) {
                    world.setBlock(fire.x, fire.y, fire.z, (byte)0); // Burn out
                } else {
                    fire.timer = 1.0f + (float)Math.random() * 2.0f; // Burn a bit longer
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
        float dirX = (float) Math.sin(Math.toRadians(yaw));
        float dirZ = (float) -Math.cos(Math.toRadians(yaw));
        float rightX = (float) Math.cos(Math.toRadians(yaw));
        float rightZ = (float) Math.sin(Math.toRadians(yaw));

        float moveX = (dirX * -joyMoveY + rightX * joyMoveX) * speed;
        float moveZ = (dirZ * -joyMoveY + rightZ * joyMoveX) * speed;

        if (!checkCollision(world, camX + moveX, camY, camZ)) camX += moveX;
        if (!checkCollision(world, camX, camY, camZ + moveZ)) camZ += moveZ;

        if (camY < -20f) { hasSpawned = false; camY = 100f; velocityY = 0; }
    }

    public void jump() { wantsToJump = true; }

    private boolean checkCollisionPoint(WorldLogic world, float x, float y, float z) {
        // Simple point collision for TNT
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
                    // Feuer (6) hat KEINE Kollision! Man kann durchlaufen!
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
