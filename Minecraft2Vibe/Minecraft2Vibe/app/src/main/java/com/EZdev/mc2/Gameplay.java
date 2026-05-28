package com.EZdev.mc2;

import android.util.Log;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.ArrayDeque;

public class Gameplay {
    private static final String TAG = "Gameplay";
    private final SecureRandom random = new SecureRandom();

    public float camX = 8, camY = 100, camZ = 8;
    public float yaw = 0, pitch = 0;
    public float playerWidth = 0.6f, playerHeight = 1.8f;
    public float velocityY = 0;
    public boolean onGround = false;
    public boolean hasSpawned = false;

    public float joyMoveX = 0, joyMoveY = 0;
    public boolean wantsToJump = false;
    public boolean isSprinting = false;
    public boolean isSneaking = false;
    public boolean isFlying = false;
    public boolean isCreative = false;

    public float gameTime = 0;
    public MainActivity activity;

    public byte activeBlock = Blocks.GRASS;
    public int activeSlot = 0;

    public boolean isBreaking = false;
    public int targetX, targetY, targetZ;
    public float breakTimer = 0f;
    private final int[] raycastResult = new int[3];

    public float shakeIntensity = 0f;
    public float health = 20.0f;
    public float fireDamageTimer = 0f;

    // Entities & Particles
    public ArrayList<ActiveTNT> tickingTNTs = new ArrayList<>();
    public ArrayList<ActiveFire> activeFires = new ArrayList<>();
    public ArrayList<ActiveFireParticle> fireParticles = new ArrayList<>();
    public ArrayList<ActiveFireParticle> blockParticles = new ArrayList<>();
    public ArrayList<ItemEntity> itemEntities = new ArrayList<>();

    private final ArrayDeque<ActiveTNT> tntPool = new ArrayDeque<>(10);
    private final ArrayDeque<ActiveFire> firePool = new ArrayDeque<>(10);
    private final ArrayDeque<ActiveFireParticle> particlePool = new ArrayDeque<>(50);

    private long lastMultiplayerUpdate = 0;

    public class ActiveTNT {
        public float x, y, z;
        public float timer = 3.0f;
        public ActiveTNT() {}
        public ActiveTNT(float x, float y, float z) { init(x, y, z); }
        public void init(float x, float y, float z) { this.x = x; this.y = y; this.z = z; this.timer = 3.0f; }
    }

    public class ActiveFire {
        public int x, y, z;
        public float life = 10.0f;
        public float spreadTimer = 1.0f;
        public void init(int x, int y, int z) {
            this.x = x; this.y = y; this.z = z;
            this.life = 10.0f + random.nextFloat() * 10f;
            this.spreadTimer = 0.5f + random.nextFloat() * 1.0f;
        }
    }

    public class ActiveFireParticle {
        public float x, y, z, vx, vy, vz, life;
        public byte type;
        public void init(float x, float y, float z, float vx, float vy, float vz, float life, byte type) {
            this.x = x; this.y = y; this.z = z; this.vx = vx; this.vy = vy; this.vz = vz; this.life = life; this.type = type;
        }
    }

    public class ItemEntity {
        public float x, y, z, hoverOffset;
        public byte type;
        public int count = 1;
        public ItemEntity(float x, float y, float z, byte type) { this.x = x; this.y = y; this.z = z; this.type = type; this.hoverOffset = random.nextFloat() * 10f; }
    }

    public ActiveTNT obtainTNT(float x, float y, float z) {
        ActiveTNT tnt = tntPool.poll();
        if (tnt == null) tnt = new ActiveTNT();
        tnt.init(x, y, z);
        return tnt;
    }

    public ActiveFire obtainFire(int x, int y, int z) {
        ActiveFire f = firePool.poll();
        if (f == null) f = new ActiveFire();
        f.init(x, y, z);
        return f;
    }

    public ActiveFireParticle obtainParticle(float x, float y, float z, float vx, float vy, float vz, float life, byte type) {
        ActiveFireParticle p = particlePool.poll();
        if (p == null) p = new ActiveFireParticle();
        p.init(x, y, z, vx, vy, vz, life, type);
        return p;
    }

    public void releaseTNT(ActiveTNT tnt) { tntPool.offer(tnt); }
    public void releaseFire(ActiveFire f) { firePool.offer(f); }
    public void releaseParticle(ActiveFireParticle p) { particlePool.offer(p); }

    public void addBlockParticles(int bx, int by, int bz, byte type) {
        for(int i=0; i<8; i++) {
            blockParticles.add(obtainParticle(bx+0.5f, by+0.5f, bz+0.5f,
                (random.nextFloat()-0.5f)*3f, random.nextFloat()*5f, (random.nextFloat()-0.5f)*3f,
                0.5f + random.nextFloat()*0.5f, type));
        }
    }

    public void addExplosionParticles(float x, float y, float z) {
        for(int i=0; i<25; i++) {
            fireParticles.add(obtainParticle(x, y, z,
                (random.nextFloat()-0.5f)*10f, (random.nextFloat()-0.5f)*10f, (random.nextFloat()-0.5f)*10f,
                1.0f, Blocks.FIRE));
        }
    }

    public void update(float dt, WorldLogic world) {
        gameTime += dt;
        if (!hasSpawned) spawnOnHighestBlock(world);

        if (activity != null && activity.multiplayerManager != null) {
            activity.multiplayerManager.updateInterpolation(dt);
            if (System.currentTimeMillis() - lastMultiplayerUpdate > 50) {
                activity.multiplayerManager.sendPositionUpdate();
                lastMultiplayerUpdate = System.currentTimeMillis();
            }
        }

        // --- TNT ---
        for (int i = tickingTNTs.size() - 1; i >= 0; i--) {
            ActiveTNT tnt = tickingTNTs.get(i);
            tnt.timer -= dt;
            if (tnt.timer <= 0) {
                if (world != null) world.explode(tnt.x, tnt.y, tnt.z, 4.0f);
                tickingTNTs.remove(i);
                releaseTNT(tnt);
            }
        }

        // --- FIRE ---
        for (int i = activeFires.size() - 1; i >= 0; i--) {
            ActiveFire f = activeFires.get(i);
            f.life -= dt;
            f.spreadTimer -= dt;

            if (f.life <= 0 || (world != null && world.getBlock(f.x, f.y, f.z) != Blocks.FIRE)) {
                if (world != null && world.getBlock(f.x, f.y, f.z) == Blocks.FIRE) world.setBlock(f.x, f.y, f.z, Blocks.AIR);
                activeFires.remove(i);
                releaseFire(f);
                continue;
            }

            if (random.nextFloat() < 0.01f) {
                fireParticles.add(obtainParticle(f.x + 0.5f, f.y + 0.5f, f.z + 0.5f, 0, 1.0f + random.nextFloat(), 0, 0.5f + random.nextFloat(), Blocks.FIRE));
            }

            // Spreading logic from original
            if (f.spreadTimer <= 0 && world != null) {
                f.spreadTimer = 0.5f + random.nextFloat() * 1.0f;
                for (int attempts = 0; attempts < 3; attempts++) {
                    int dx = random.nextInt(3) - 1;
                    int dy = random.nextInt(3) - 1;
                    int dz = random.nextInt(3) - 1;
                    int nx = f.x + dx, ny = f.y + dy, nz = f.z + dz;
                    byte block = world.getBlock(nx, ny, nz);
                    if (block == Blocks.WOOD || block == Blocks.LEAVES) {
                        float distPenalty = (Math.abs(dx) + Math.abs(dy) + Math.abs(dz)) * 0.15f;
                        float baseChance = (block == Blocks.LEAVES) ? 0.9f : 0.6f;
                        if (random.nextFloat() < (baseChance - distPenalty)) {
                            world.setBlock(nx, ny, nz, Blocks.FIRE);
                            activeFires.add(obtainFire(nx, ny, nz));
                        }
                    }
                }
            }
        }

        updateParticles(fireParticles, dt, world, true);
        updateParticles(blockParticles, dt, world, false);

        // --- PLAYER MOVEMENT ---
        boolean inWater = false;
        byte bf = (world != null) ? world.getBlock((int)Math.floor(camX), (int)Math.floor(camY), (int)Math.floor(camZ)) : 0;
        if (bf == Blocks.WATER) inWater = true;

        playerHeight = isSneaking ? 1.5f : 1.8f;
        if (!isFlying || !isCreative) {
            velocityY -= (inWater ? 5f : 25f) * dt;
            if (velocityY < -20f) velocityY = -20f;
        } else if (isFlying) {
            velocityY = wantsToJump ? 10f : (isSneaking ? -10f : 0f);
        }

        if (wantsToJump && onGround && !isFlying) {
            velocityY = 8.5f;
            onGround = false;
        }

        float nextY = camY + velocityY * dt;
        if (!checkCollision(world, camX, nextY, camZ)) {
            camY = nextY;
            onGround = false;
        } else {
            if (velocityY < 0) onGround = true;
            velocityY = 0;
        }

        float speed = (isSprinting ? 8f : (isSneaking ? 2f : 5f)) * dt;
        float yawRad = (float)Math.toRadians(yaw);
        float sinY = (float)Math.sin(yawRad), cosY = (float)Math.cos(yawRad);
        float moveX = (sinY * -joyMoveY + cosY * joyMoveX) * speed;
        float moveZ = (-cosY * -joyMoveY + sinY * joyMoveX) * speed;

        if (!checkCollision(world, camX + moveX, camY, camZ)) camX += moveX;
        if (!checkCollision(world, camX, camY, camZ + moveZ)) camZ += moveZ;

        if (camY < -20f) { hasSpawned = false; camY = 100f; velocityY = 0; }

        // --- BREAKING ---
        if (isBreaking && !isCreative) {
            int[] hit = (world != null) ? world.raycastBlock(this, raycastResult) : null;
            if (hit != null) {
                if (hit[0] == targetX && hit[1] == targetY && hit[2] == targetZ) {
                    breakTimer += dt;
                    byte hitType = world.getBlock(targetX, targetY, targetZ);
                    float req = 1.0f;
                    if(hitType == Blocks.GRASS || hitType == Blocks.DIRT || hitType == Blocks.LEAVES) req = 0.3f;
                    else if(hitType == Blocks.STONE) req = 2.0f;
                    else if(hitType == Blocks.WOOD) req = 1.5f;

                    if (random.nextFloat() < 0.1f) addBlockParticles(targetX, targetY, targetZ, hitType);

                    if (breakTimer >= req) {
                        world.setBlock(targetX, targetY, targetZ, Blocks.AIR);
                        addBlockParticles(targetX, targetY, targetZ, hitType);
                        world.spawnItemEntity(targetX, targetY, targetZ, hitType);
                        if(activity != null && activity.soundManager != null) activity.soundManager.playSoundForBlock(hitType);
                        isBreaking = false;
                        targetX = -1;
                    }
                } else {
                    targetX = hit[0]; targetY = hit[1]; targetZ = hit[2];
                    breakTimer = 0f;
                }
            } else { isBreaking = false; targetX = -1; }
        }
    }

    private void updateParticles(ArrayList<ActiveFireParticle> list, float dt, WorldLogic world, boolean isFire) {
        for(int i = list.size() - 1; i >= 0; i--) {
            ActiveFireParticle p = list.get(i);
            p.life -= dt; p.y += p.vy * dt;
            if (p.life <= 0) { list.remove(i); releaseParticle(p); }
        }
    }

    private boolean checkCollision(WorldLogic world, float x, float y, float z) {
        if (world == null) return false;
        float shrink = 0.05f;
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
                    if (block > Blocks.AIR && block != Blocks.FIRE && block != Blocks.WATER) return true;
                }
            }
        }
        return false;
    }

    private void spawnOnHighestBlock(WorldLogic world) {
        if (world == null) return;
        int cx = (int) Math.floor(camX), cz = (int) Math.floor(camZ);
        for (int y = 127; y > 0; y--) {
            if (world.getBlock(cx, y, cz) > Blocks.AIR) {
                camY = y + 1.1f; hasSpawned = true; return;
            }
        }
        camY = 100;
    }

    public void jump() { wantsToJump = true; }
}
