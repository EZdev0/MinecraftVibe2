package com.EZdev.mc2;

import java.util.Random;

public class Entity {
    private final Random random = new Random();
    public float x, y, z;
    public float vx = 0, vy = 0, vz = 0;
    public float targetX, targetZ;
    public float changeDirTimer = 0f;
    public boolean onGround = false;

    public Entity(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
        this.targetX = x; this.targetZ = z;
    }

    public void update(float dt, WorldLogic world) {
        // Simple random wandering pathfinding
        changeDirTimer -= dt;
        if (changeDirTimer <= 0) {
            targetX = x + (random.nextFloat() * 10f - 5f);
            targetZ = z + (random.nextFloat() * 10f - 5f);
            changeDirTimer = 3.0f + random.nextFloat() * 3.0f;
        }

        // Move towards target
        float dx = targetX - x;
        float dz = targetZ - z;
        float dist = (float)Math.sqrt(dx*dx + dz*dz);
        if (dist > 0.5f) {
            vx = (dx / dist) * 1.5f; // Walk speed
            vz = (dz / dist) * 1.5f;
        } else {
            vx = 0; vz = 0;
        }

        // Gravity
        vy -= 20.0f * dt;

        float nextY = y + vy * dt;
        if (!checkCollisionPoint(world, x, nextY, z)) {
            y = nextY;
            onGround = false;
        } else {
            vy = 0;
            onGround = true;
        }

        // Auto-Jump over blocks
        float nextX = x + vx * dt;
        float nextZ = z + vz * dt;
        if (onGround && (checkCollisionPoint(world, nextX, y, z) || checkCollisionPoint(world, x, y, nextZ))) {
            if (!checkCollisionPoint(world, nextX, y + 1.1f, z) && !checkCollisionPoint(world, x, y + 1.1f, nextZ)) {
                y += 1.1f; // Jump up
            } else {
                // Change direction if completely blocked
                changeDirTimer = 0;
                vx = 0; vz = 0;
            }
        }

        if (!checkCollisionPoint(world, x + vx * dt, y, z)) x += vx * dt;
        if (!checkCollisionPoint(world, x, y, z + vz * dt)) z += vz * dt;
    }

    private boolean checkCollisionPoint(WorldLogic world, float cx, float cy, float cz) {
        if (world == null) return false;
        byte block = world.getBlock((int)Math.floor(cx), (int)Math.floor(cy), (int)Math.floor(cz));
        return (block > 0 && block != 6 && block != 7);
    }
}
