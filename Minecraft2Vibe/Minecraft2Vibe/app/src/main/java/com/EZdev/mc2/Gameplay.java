package com.EZdev.mc2;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Random;

public class Gameplay {
    private final Random random = new Random();
    public float camX = 8f, camY = 100f, camZ = 8f;
    public float yaw = 0f, pitch = 0f;
    public float joyMoveX = 0f, joyMoveY = 0f;

    public float velocityY = 0f;
    public boolean onGround = false;
    private boolean hasSpawned = false;

    public boolean wantsToJump = false;
    public MainActivity activity;
    public boolean isSneaking = false;
    public boolean isSprinting = false;

    public float shakeIntensity = 0f;
    public float shakeTimer = 0f;

    public float playerHeight = 1.8f;
    public float playerWidth = 0.6f;

    public byte activeBlock = Blocks.GRASS;
    public int activeSlot = 0;
    public float gameTime = 0f;

    public ArrayList<ActiveTNT> tickingTNTs = new ArrayList<>();
    public ArrayList<ActiveFire> activeFires = new ArrayList<>();
    public ArrayList<ActiveFireParticle> fireParticles = new ArrayList<>();
    public ArrayList<ActiveFireParticle> blockParticles = new ArrayList<>();

    private final Deque<ActiveTNT> tntPool = new ArrayDeque<>();
    private final Deque<ActiveFire> firePool = new ArrayDeque<>();
    private final Deque<ActiveFireParticle> particlePool = new ArrayDeque<>();

    public boolean isCreative = false;
    public boolean isFlying = false;
    public float health = 20.0f;
    public float fireDamageTimer = 0f;
    public boolean isRaining = false;
    public boolean isThundering = false;
    public float weatherTimer = 300f;


    private static final int[][] FIRE_NEIGHBORS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    // Breaking logic
    public boolean isBreaking = false;
    public float breakTimer = 0f;
    public int targetX = -1, targetY = -1, targetZ = -1;

    public class ActiveTNT {
        public float x, y, z;
        public float vx = 0f, vy = 0f, vz = 0f;
        public float timer = 3.0f;
        public ActiveTNT(float x, float y, float z) { init(x, y, z); }
        public void init(float x, float y, float z) {
            this.x = x; this.y = y; this.z = z;
            this.vx = 0f; this.vy = 0f; this.vz = 0f;
            this.timer = 3.0f;
        }
    }

    public class ActiveFire {
        public int x, y, z;
        public float life;
        public float spreadTimer;
        public ActiveFire(int x, int y, int z) { init(x, y, z); }
        public void init(int x, int y, int z) {
            this.x = x; this.y = y; this.z = z;
            this.life = 5.0f + random.nextFloat() * 5.0f;
            this.spreadTimer = 0.5f + random.nextFloat() * 1.5f;
        }
    }

    public class ActiveFireParticle {
        public float x, y, z;
        public float vx, vy, vz;
        public float life;
        public byte type;
        public ActiveFireParticle(float x, float y, float z, byte type) { init(x, y, z, type); }
        public void init(float x, float y, float z, byte type) {
            this.x = x; this.y = y; this.z = z;
            this.vx = (random.nextFloat() - 0.5f) * 6f;
            this.vy = (random.nextFloat()) * 6f + 3f;
            this.vz = (random.nextFloat() - 0.5f) * 6f;
            this.life = 0.8f + random.nextFloat() * 2.0f;
            this.type = type;
        }
        public ActiveFireParticle(float x, float y, float z, float intensity) { init(x, y, z, intensity); }
        public void init(float x, float y, float z, float intensity) {
            this.x = x; this.y = y; this.z = z;
            this.vx = (random.nextFloat() - 0.5f) * intensity;
            this.vy = (random.nextFloat() - 0.5f) * intensity;
            this.vz = (random.nextFloat() - 0.5f) * intensity;
            this.life = 1.0f + random.nextFloat() * 3.0f;
            this.type = Blocks.SMOKE;
        }
    }

    public ActiveTNT obtainTNT(float x, float y, float z) {
        if (!tntPool.isEmpty()) {
            ActiveTNT tnt = tntPool.pop();
            tnt.init(x, y, z);
            return tnt;
        }
        return new ActiveTNT(x, y, z);
    }

    public void releaseTNT(ActiveTNT tnt) {
        if (tnt != null) tntPool.push(tnt);
    }

    public ActiveFire obtainFire(int x, int y, int z) {
        if (!firePool.isEmpty()) {
            ActiveFire fire = firePool.pop();
            fire.init(x, y, z);
            return fire;
        }
        return new ActiveFire(x, y, z);
    }

    public void releaseFire(ActiveFire fire) {
        if (fire != null) firePool.push(fire);
    }

    public ActiveFireParticle obtainParticle(float x, float y, float z, byte type) {
        if (!particlePool.isEmpty()) {
            ActiveFireParticle p = particlePool.pop();
            p.init(x, y, z, type);
            return p;
        }
        return new ActiveFireParticle(x, y, z, type);
    }

    public ActiveFireParticle obtainParticle(float x, float y, float z, float intensity) {
        if (!particlePool.isEmpty()) {
            ActiveFireParticle p = particlePool.pop();
            p.init(x, y, z, intensity);
            return p;
        }
        return new ActiveFireParticle(x, y, z, intensity);
    }

    public void releaseParticle(ActiveFireParticle p) {
        if (p != null) particlePool.push(p);
    }

    public void addBlockParticles(float x, float y, float z, byte blockType) {
        for(int i=0; i<8; i++) { // Reduced count!
            blockParticles.add(obtainParticle(x+0.5f, y+0.5f, z+0.5f, blockType));
        }
    }

    public void addExplosionParticles(float x, float y, float z) {
        for(int i=0; i<25; i++) {
            fireParticles.add(obtainParticle(x, y, z, 20f));
        }
    }

        public void update(float dt, WorldLogic world) {
        // Update Reverb in Caves
        if (activity != null && activity.soundManager != null) {
            boolean inCave = camY < 40;
            activity.soundManager.updateReverb(inCave);
        }
        if (world == null) return;
        if (!hasSpawned) { spawnOnHighestBlock(world); return; }
        if (dt > 0.05f) dt = 0.05f;

        // Anti-Stuck Mechanism
        if (!isFlying && (world.getBlock((int)Math.floor(camX), (int)Math.floor(camY), (int)Math.floor(camZ)) != Blocks.AIR ||
            world.getBlock((int)Math.floor(camX), (int)Math.floor(camY + playerHeight - 0.5f), (int)Math.floor(camZ)) != Blocks.AIR)) {
            camY += 5.0f * dt; // Push up smoothly
        }

        gameTime += dt;
        weatherTimer -= dt;
        if (weatherTimer <= 0) {
            weatherTimer = 60f + random.nextFloat() * 200f;
            if (!isRaining) {
                isRaining = true;
                isThundering = random.nextFloat() < 0.3f;
            } else {
                isRaining = false;
                isThundering = false;
            }
        }
        if (isRaining) {
            if (random.nextFloat() < 0.5f) {
                float rx = camX + (random.nextFloat() * 40f - 20f);
                float rz = camZ + (random.nextFloat() * 40f - 20f);
                float ry = camY + 20f + random.nextFloat() * 10f;
                ActiveFireParticle p = obtainParticle(rx, ry, rz, Blocks.WATER);
                p.vy = -10f - random.nextFloat() * 5f;
                blockParticles.add(p);
            }
        }
        if (isThundering && random.nextFloat() < 0.01f) {
            float tx = camX + (random.nextFloat() * 50f - 25f);
            float tz = camZ + (random.nextFloat() * 50f - 25f);
            for (int y = 127; y > 0; y--) {
                if (world != null && world.getBlock((int)tx, y, (int)tz) > Blocks.AIR) {
                    world.setBlock((int)tx, y + 1, (int)tz, Blocks.FIRE);
                    activeFires.add(obtainFire((int)tx, y + 1, (int)tz));
                    if (activity != null) activity.runOnUiThread(() -> {
                        activity.getWindow().getDecorView().setBackgroundColor(android.graphics.Color.WHITE);
                        new android.os.Handler().postDelayed(() -> activity.getWindow().getDecorView().setBackgroundColor(android.graphics.Color.BLACK), 50);
                    });
                    break;
                }
            }
        }



        if (shakeTimer > 0) {
            shakeTimer -= dt;
            if (shakeTimer <= 0) shakeIntensity = 0;
        } else {
            shakeIntensity = 0;
        }

        // Entity Update
        if(world.entities != null) {
            for(Entity e : world.entities) {
                e.update(dt, world);
            }
        }

        // Health Regeneration in Survival
        if (!isCreative && health > 0 && health < 20.0f) {
            health += 0.5f * dt; // Regenerate slowly
            if(health > 20.0f) health = 20.0f;
        }

        if (!isCreative) {
            isFlying = false;
            int px = (int)Math.floor(camX);
            int py = (int)Math.floor(camY);
            int pz = (int)Math.floor(camZ);
            byte headBlock = (world != null) ? world.getBlock(px, (int)Math.floor(camY+1f), pz) : Blocks.AIR;
            if ((world != null && world.getBlock(px, py, pz) == Blocks.FIRE) || headBlock == Blocks.FIRE) {
                fireDamageTimer -= dt;
                if (fireDamageTimer <= 0) {
                    health -= 2.0f;
                    fireDamageTimer = 1.0f;
                    if (health <= 0) {
                        if(activity != null && !activity.getSharedPreferences("McPrefs", android.content.Context.MODE_PRIVATE).getBoolean("FIRE_UNLOCKED", false)) {
                            activity.getSharedPreferences("McPrefs", android.content.Context.MODE_PRIVATE).edit().putBoolean("FIRE_UNLOCKED", true).apply();
                            activity.runOnUiThread(() -> android.widget.Toast.makeText(activity, "FEUERZEUG FREIGESCHALTET!", android.widget.Toast.LENGTH_LONG).show());
                            if(activity.uiManager != null) {
                                activity.uiManager.inventory[5] = 999;
                                activity.uiManager.updateHotbarUI();
                            }
                        }
                        hasSpawned = false;
                        health = 20.0f;
                    }
                }
            } else {
                fireDamageTimer = 0f;
            }
        }

        // --- TNT UPDATE ---
        for (int i = tickingTNTs.size() - 1; i >= 0; i--) {
            ActiveTNT tnt = tickingTNTs.get(i);
            if (tnt == null) { int lastIdx = tickingTNTs.size() - 1; if (i < lastIdx) tickingTNTs.set(i, tickingTNTs.get(lastIdx)); tickingTNTs.remove(lastIdx); continue; }
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
                if (world != null) world.explode(tnt.x, tnt.y, tnt.z, 4.0f);
                if (!isCreative) {
                    float dist = (float)Math.sqrt((camX-tnt.x)*(camX-tnt.x) + (camY-tnt.y)*(camY-tnt.y) + (camZ-tnt.z)*(camZ-tnt.z));
                    if (dist < 5.0f) {
                        health -= (5.0f - dist) * 4.0f;
                        if (health <= 0) { hasSpawned = false; health = 20.0f; }
                        if(activity != null && !activity.getSharedPreferences("McPrefs", android.content.Context.MODE_PRIVATE).getBoolean("FIRE_UNLOCKED", false)) {
                            activity.getSharedPreferences("McPrefs", android.content.Context.MODE_PRIVATE).edit().putBoolean("FIRE_UNLOCKED", true).apply();
                            activity.runOnUiThread(() -> android.widget.Toast.makeText(activity, "FEUERZEUG FREIGESCHALTET!", android.widget.Toast.LENGTH_LONG).show());
                            if(activity.uiManager != null) {
                                activity.uiManager.inventory[5] = 999;
                                activity.uiManager.updateHotbarUI();
                            }
                        }
                    }
                }
                int lastIdx = tickingTNTs.size() - 1;
                if (i < lastIdx) tickingTNTs.set(i, tickingTNTs.get(lastIdx));
                tickingTNTs.remove(lastIdx);
                releaseTNT(tnt);
            }
        }

        // --- PARTICLES UPDATE ---
        updateParticles(fireParticles, dt, world, true);
        updateParticles(blockParticles, dt, world, false);

        // --- FIRE LOGIC UPDATE ---
        for (int i = activeFires.size() - 1; i >= 0; i--) {
            ActiveFire fire = activeFires.get(i);
            if (fire == null) { int lastIdx = activeFires.size() - 1; if (i < lastIdx) activeFires.set(i, activeFires.get(lastIdx)); activeFires.remove(lastIdx); continue; }

            if (world != null && world.getBlock(fire.x, fire.y, fire.z) != Blocks.FIRE) {
                int lastIdx = activeFires.size() - 1;
                if (i < lastIdx) {
                    activeFires.set(i, activeFires.get(lastIdx));
                }
                activeFires.remove(lastIdx);
                releaseFire(fire);
                continue;
            }

            boolean hasSupport = false;
            for (int[] n : FIRE_NEIGHBORS) {
                if (world != null && world.getBlock(fire.x + n[0], fire.y + n[1], fire.z + n[2]) > Blocks.AIR)
                    hasSupport = true;
            }
            if (!hasSupport) {
                if (world != null) world.setBlock(fire.x, fire.y, fire.z, Blocks.AIR);
                int lastIdx = activeFires.size() - 1;
                if (i < lastIdx) {
                    activeFires.set(i, activeFires.get(lastIdx));
                }
                activeFires.remove(lastIdx);
                releaseFire(fire);
                continue;
            }

            fire.life -= dt;
            fire.spreadTimer -= dt;

            if (random.nextFloat() < 0.01f) {
                fireParticles.add(obtainParticle(fire.x + 0.5f, fire.y + 0.5f, fire.z + 0.5f, Blocks.FIRE));
            }

            if (fire.life <= 0) {
                if (world != null) world.setBlock(fire.x, fire.y, fire.z, Blocks.AIR);
                int lastIdx = activeFires.size() - 1;
                if (i < lastIdx) {
                    activeFires.set(i, activeFires.get(lastIdx));
                }
                activeFires.remove(lastIdx);
                releaseFire(fire);
                continue;
            }

            if (fire.spreadTimer <= 0) {
                fire.spreadTimer = 0.5f + random.nextFloat() * 1.0f;
                for (int attempts = 0; attempts < 3; attempts++) {
                    int dx = random.nextInt(3) - 1;
                    int dy = random.nextInt(6) - 1;
                    int dz = random.nextInt(3) - 1;
                    int nx = fire.x + dx; int ny = fire.y + dy; int nz = fire.z + dz;
                    byte blockType = world.getBlock(nx, ny, nz);
                    if ((blockType == Blocks.WOOD || blockType == Blocks.LEAVES)) {
                        float distancePenalty = (Math.abs(dx) + Math.abs(dy) + Math.abs(dz)) * 0.15f;
                        float baseChance = (blockType == Blocks.LEAVES) ? 0.9f : 0.6f;
                        if (random.nextFloat() < baseChance - distancePenalty) {
                            if(world != null) world.setBlock(nx, ny, nz, Blocks.FIRE);
                            activeFires.add(obtainFire(nx, ny, nz));
                        }
                    }
                }
            }
        }

        if (random.nextFloat() < 0.1f) {
            int px = (int)camX; int py = (int)camY-2; int pz = (int)camZ;
            byte b = (world != null) ? world.getBlock(px, py, pz) : Blocks.AIR;
            if (world != null && b == Blocks.SAND && world.getBlock(px, py-1, pz) == Blocks.AIR) {
                if(world != null) world.setBlock(px, py, pz, Blocks.AIR);
                if(world != null) world.setBlock(px, py-1, pz, Blocks.SAND);
            }
        }

        // --- PLAYER MOVEMENT ---
        boolean inWater = false;
        byte blockAtFeet = (world != null) ? world.getBlock((int)Math.floor(camX), (int)Math.floor(camY), (int)Math.floor(camZ)) : Blocks.AIR;
        byte blockAtHead = (world != null) ? world.getBlock((int)Math.floor(camX), (int)Math.floor(camY + 1.5f), (int)Math.floor(camZ)) : Blocks.AIR;
        if (blockAtFeet == Blocks.WATER || blockAtHead == Blocks.WATER) inWater = true;

        playerHeight = isSneaking ? 1.5f : 1.8f;

        if (checkCollision(world, camX, camY, camZ)) { camY += 2.5f * dt; }

        if (wantsToJump) {
            if (isFlying && isCreative) {
                velocityY = 10.0f;
            } else if (inWater) {
                velocityY = 4.0f;
            } else if (onGround) {
                velocityY = 8.5f;
                onGround = false;
            }
        } else if (isFlying && isCreative) {
            if(isSneaking) velocityY = -10.0f;
            else velocityY = 0f;
        }

        if (!isFlying || !isCreative) {
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
        if (isFlying && isCreative) speed = 10.0f * dt;

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

        // --- BREAKING CONTINUOUS UPDATE ---
        if (isBreaking && !isCreative) {
            float eyeHeight = camY + playerHeight - 0.2f;
            int bx = (int) Math.floor(camX + dirX * 3.0f); // Simple forward trace
            float dirY = (float) Math.sin(Math.toRadians(pitch));
            int by = (int) Math.floor(eyeHeight + dirY * 3.0f);
            int bz = (int) Math.floor(camZ + dirZ * 3.0f);

            // To make it simple: we use world.interact for raycast to find target block
            int[] hit = (world != null) ? world.raycastBlock(this) : null;
            if(hit != null) {
                if(hit[0] == targetX && hit[1] == targetY && hit[2] == targetZ) {
                    breakTimer += dt;
                    byte hitType = (world != null) ? world.getBlock(targetX, targetY, targetZ) : Blocks.AIR;
                    float requiredTime = 1.0f; // Default 1 sec
                    if(hitType == Blocks.GRASS || hitType == Blocks.DIRT || hitType == Blocks.LEAVES) requiredTime = 0.3f; // Dirt/Leaves fast
                    else if(hitType == Blocks.STONE) requiredTime = 2.0f; // Stone slow
                    else if(hitType == Blocks.WOOD) requiredTime = 1.5f; // Wood medium

                    // Add some breaking particles while hitting
                    if(random.nextFloat() < 0.1f) addBlockParticles(targetX, targetY, targetZ, hitType);

                    if (breakTimer >= requiredTime) {
                        if(world != null) world.setBlock(targetX, targetY, targetZ, Blocks.AIR);
                        addBlockParticles(targetX, targetY, targetZ, hitType); // Final burst
                        if(world != null) world.spawnItemEntity(targetX, targetY, targetZ, hitType); // Drop item!
                        if(activity != null && activity.soundManager != null) activity.soundManager.playSoundForBlock(hitType);
                        isBreaking = false;
                        targetX = -1;
                    }
                } else {
                    targetX = hit[0]; targetY = hit[1]; targetZ = hit[2];
                    breakTimer = 0f;
                }
            } else {
                isBreaking = false;
                targetX = -1;
            }
        }
    }

    private void updateParticles(ArrayList<ActiveFireParticle> list, float dt, WorldLogic world, boolean isFire) {
        for(int i = list.size() - 1; i >= 0; i--) {
            ActiveFireParticle p = list.get(i);
            if (p == null) { int lastIdx = list.size() - 1; if (i < lastIdx) list.set(i, list.get(lastIdx)); list.remove(lastIdx); continue; }
            p.life -= dt;
            p.vy -= 15.0f * dt;
            p.x += p.vx * dt; p.y += p.vy * dt; p.z += p.vz * dt;

            if (p.life <= 0) {
                int lastIdx = list.size() - 1;
                if (i < lastIdx) list.set(i, list.get(lastIdx));
                list.remove(lastIdx);
                releaseParticle(p);
                continue;
            }

            if(checkCollisionPoint(world, p.x, p.y, p.z)) {
                if (isFire && p.type == Blocks.FIRE) {
                    byte block = world.getBlock((int)Math.floor(p.x), (int)Math.floor(p.y), (int)Math.floor(p.z));
                    if (block == Blocks.WOOD || block == Blocks.LEAVES) {
                        if (random.nextFloat() < 0.3f) {
                            if(world != null) world.setBlock((int)Math.floor(p.x), (int)Math.floor(p.y), (int)Math.floor(p.z), Blocks.FIRE);
                            activeFires.add(obtainFire((int)Math.floor(p.x), (int)Math.floor(p.y), (int)Math.floor(p.z)));
                        }
                    }
                } else if (!isFire) {
                    p.vy *= -0.3f;
                    p.vx *= 0.5f; p.vz *= 0.5f;
                    p.y += 0.1f;
                    if(Math.abs(p.vy) < 0.1f) { p.vx = 0; p.vz = 0; }
                }

                if (isFire) {
                    int lastIdx = list.size() - 1;
                    if (i < lastIdx) list.set(i, list.get(lastIdx));
                    list.remove(lastIdx);
                    releaseParticle(p);
                }
            }
        }
    }

    public void jump() { wantsToJump = true; }

    private boolean checkCollisionPoint(WorldLogic world, float x, float y, float z) {
        if (world == null) return false;
        byte block = world.getBlock((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
        return (block > Blocks.AIR && block != Blocks.FIRE && block != Blocks.WATER);
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
                    if (block > Blocks.AIR && block != Blocks.FIRE && block != Blocks.WATER) return true;
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
            if (b > Blocks.AIR && b != Blocks.WATER) {
                camY = y + 1.001f;
                hasSpawned = true;
                return;
            }
        }
    }
}
