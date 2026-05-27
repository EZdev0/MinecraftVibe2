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

    public WorldLogic() { Noise.init(worldSeed); TextRenderer.init(); }

    public void updateChunks(float px, float pz) {
        int cx = (int) Math.floor(px / CHUNK_SIZE), cz = (int) Math.floor(pz / CHUNK_SIZE);
        for (int x = -renderDistance; x <= renderDistance; x++)
            for (int z = -renderDistance; z <= renderDistance; z++) {
                String key = (cx + x) + "," + (cz + z);
                if (!chunks.containsKey(key)) chunks.put(key, new Chunk(this, cx + x, cz + z));
            }
    }

    public byte getBlock(int x, int y, int z) {
        if (y < 0 || y >= 128) return Blocks.AIR;
        Chunk c = chunks.get(((int)Math.floor((float)x/16)) + "," + ((int)Math.floor((float)z/16)));
        return (c != null) ? c.blocks[x&15][y][z&15] : Blocks.AIR;
    }

    public void setBlock(int x, int y, int z, byte block) { setBlock(x, y, z, block, true); }
    public synchronized void setBlock(int x, int y, int z, byte block, boolean sync) {
        if (y < 0 || y >= 128 || y == 0 || getBlock(x, y, z) == Blocks.BEDROCK) return;
        Chunk c = chunks.get(((int)Math.floor((float)x/16)) + "," + ((int)Math.floor((float)z/16)));
        if (c != null) {
            c.blocks[x&15][y][z&15] = block; c.buildMesh();
            if (sync && multiplayerManager != null) multiplayerManager.sendBlockChange(x, y, z, block);
        }
    }

    public void explode(float x, float y, float z, float radius) {
        int r = (int) radius;
        for(int dx=-r; dx<=r; dx++) for(int dy=-r; dy<=r; dy++) for(int dz=-r; dz<=r; dz++)
            if (dx*dx+dy*dy+dz*dz < radius*radius) {
                int bx=(int)(x+dx), by=(int)(y+dy), bz=(int)(z+dz);
                if (by >= 1 && by < 127) setBlock(bx, by, bz, Blocks.AIR);
            }
    }

    public void checkIgnition(int x, int y, int z, Gameplay g) {
        int[][] neighbors = {{1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1}};
        for(int[] n : neighbors) {
            int nx=x+n[0], ny=y+n[1], nz=z+n[2];
            if(getBlock(nx, ny, nz) == Blocks.TNT) { setBlock(nx, ny, nz, Blocks.AIR); g.tickingTNTs.add(g.obtainTNT(nx+0.5f, ny, nz+0.5f)); }
        }
    }

    public void render(float[] vpMatrix, Gameplay gameplay) {
        GLES20.glEnableVertexAttribArray(Booster.posHandle); GLES20.glEnableVertexAttribArray(Booster.colorHandle);
        for (Chunk c : chunks.values()) if (c.vertexBuffer != null) {
            Matrix.setIdentityM(modelMatrix, 0); Matrix.multiplyMM(finalMVP, 0, vpMatrix, 0, modelMatrix, 0);
            GLES20.glUniformMatrix4fv(Booster.mvpHandle, 1, false, finalMVP, 0);
            GLES20.glVertexAttribPointer(Booster.posHandle, 3, GLES20.GL_FLOAT, false, 0, c.vertexBuffer);
            GLES20.glVertexAttribPointer(Booster.colorHandle, 4, GLES20.GL_FLOAT, false, 0, c.colorBuffer);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, c.vertexCount);
        }
        if (multiplayerManager != null) for (MultiplayerManager.RemotePlayer rp : multiplayerManager.players.values())
            if (System.currentTimeMillis() - rp.lastUpdate < 5000) renderRemotePlayer(rp, vpMatrix);
        GLES20.glDisableVertexAttribArray(Booster.posHandle); GLES20.glDisableVertexAttribArray(Booster.colorHandle);
    }

    private void renderRemotePlayer(MultiplayerManager.RemotePlayer rp, float[] vpMatrix) {
        // Body
        Matrix.setIdentityM(modelMatrix, 0); Matrix.translateM(modelMatrix, 0, rp.x, rp.y+0.9f, rp.z);
        Matrix.rotateM(modelMatrix, 0, -rp.yaw, 0, 1, 0); Matrix.scaleM(modelMatrix, 0, 0.6f, 1.2f, 0.3f);
        Matrix.multiplyMM(finalMVP, 0, vpMatrix, 0, modelMatrix, 0);
        GLES20.glUniformMatrix4fv(Booster.mvpHandle, 1, false, finalMVP, 0); GLES20.glUniform1i(Booster.pTypeHandle, Blocks.STONE);
        GLES20.glVertexAttribPointer(Booster.posHandle, 3, GLES20.GL_FLOAT, false, 0, Booster.tntVertexBuffer);
        GLES20.glVertexAttribPointer(Booster.colorHandle, 4, GLES20.GL_FLOAT, false, 0, Booster.tntColorBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
        // Head
        Matrix.setIdentityM(modelMatrix, 0); Matrix.translateM(modelMatrix, 0, rp.x, rp.y+1.8f, rp.z);
        Matrix.rotateM(modelMatrix, 0, -rp.yaw, 0, 1, 0); Matrix.rotateM(modelMatrix, 0, rp.pitch, 1, 0, 0);
        Matrix.scaleM(modelMatrix, 0, 0.4f, 0.4f, 0.4f); Matrix.multiplyMM(finalMVP, 0, vpMatrix, 0, modelMatrix, 0);
        GLES20.glUniformMatrix4fv(Booster.mvpHandle, 1, false, finalMVP, 0); GLES20.glUniform1i(Booster.pTypeHandle, Blocks.ENTITY_PIG);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
        // Name Tag (Billboard)
        if (rp.nameTagTexture == -1) rp.nameTagTexture = TextRenderer.createTextTexture(rp.name);
        GLES20.glEnable(GLES20.GL_BLEND); GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        Matrix.setIdentityM(modelMatrix, 0); Matrix.translateM(modelMatrix, 0, rp.x, rp.y+2.2f, rp.z);
        Matrix.rotateM(modelMatrix, 0, -rp.yaw, 0, 1, 0); Matrix.scaleM(modelMatrix, 0, 1.5f, 0.5f, 1.0f);
        Matrix.multiplyMM(finalMVP, 0, vpMatrix, 0, modelMatrix, 0);
        GLES20.glUniformMatrix4fv(Booster.mvpHandle, 1, false, finalMVP, 0);
        GLES20.glUniform1i(Booster.useTexHandle, 1); GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, rp.nameTagTexture);
        GLES20.glEnableVertexAttribArray(Booster.texCoordHandle);
        GLES20.glVertexAttribPointer(Booster.texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, Booster.quadTexCoordBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        GLES20.glDisableVertexAttribArray(Booster.texCoordHandle); GLES20.glUniform1i(Booster.useTexHandle, 0);
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    public void interact(Gameplay g, boolean place, UIManager ui) {
        int[] hit = raycastBlock(g, new int[3]);
        if (hit != null) { if (place) setBlock(hit[0], hit[1]+1, hit[2], g.activeBlock); else setBlock(hit[0], hit[1], hit[2], Blocks.AIR); }
    }

    public int[] raycastBlock(Gameplay g, int[] out) {
        float dirX=(float)(Math.cos(Math.toRadians(g.pitch))*Math.sin(Math.toRadians(g.yaw))), dirY=(float)Math.sin(Math.toRadians(g.pitch)), dirZ=(float)(-Math.cos(Math.toRadians(g.pitch))*Math.cos(Math.toRadians(g.yaw)));
        for(float d=0; d<6f; d+=0.1f) {
            int bx=(int)Math.floor(g.camX+dirX*d), by=(int)Math.floor(g.camY+1.6f+dirY*d), bz=(int)Math.floor(g.camZ+dirZ*d);
            if(getBlock(bx,by,bz) > 0) { if(out != null) { out[0]=bx; out[1]=by; out[2]=bz; } return out; }
        }
        return null;
    }
    public void spawnItemEntity(float x, float y, float z, byte type) { droppedItems.add(new Gameplay().new ItemEntity(x, y, z, type)); }
    public void spawnItemEntity(float x, float y, float z, byte type, int count) { Gameplay.ItemEntity item = new Gameplay().new ItemEntity(x, y, z, type); item.count = Math.max(0, count); droppedItems.add(item); }
    private Gameplay gameplayRef; private void setGameplay(Gameplay g) { this.gameplayRef = g; }
}
