package com.EZdev.mc2;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGdxGame implements GLSurfaceView.Renderer {

    public Gameplay gameplay = new Gameplay();
    public WorldLogic world = new WorldLogic();
    public MainActivity activity;

    private float[] projectionMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] vpMatrix = new float[16];

    private long lastTime;

    private int frames = 0;
    private long lastFPSUpdate = 0;
    public int currentFPS = 0;

    private float screenRatio = 1.0f;

    public MyGdxGame(MainActivity act) {
        this.activity = act;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.8f, 1.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        Booster.initGeometry();
        lastTime = System.nanoTime();
        lastFPSUpdate = System.currentTimeMillis();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        screenRatio = (float)width/height;
        Matrix.perspectiveM(projectionMatrix, 0, 70f, screenRatio, 0.1f, 300f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        long now = System.nanoTime();
        float dt = (now - lastTime) / 1000000000.0f;
        lastTime = now;

        frames++;
        if (System.currentTimeMillis() - lastFPSUpdate >= 1000) {
            currentFPS = frames;
            frames = 0;
            lastFPSUpdate = System.currentTimeMillis();
        }

        gameplay.update(dt, world);
        world.updateChunks(gameplay.camX, gameplay.camZ);

        if (activity != null && activity.uiManager != null && activity.uiManager.touchOverlay != null) {
            activity.uiManager.touchOverlay.postInvalidate();
        }

        // Apply Dynamic FOV
        float fov = 70f;
        if(gameplay.isSprinting || gameplay.isFlying) fov = 90f;
        Matrix.perspectiveM(projectionMatrix, 0, fov, screenRatio, 0.1f, 300f);

        float eyeHeight = gameplay.camY + gameplay.playerHeight - 0.2f;

        // Feature 7: Apply Screen Shake
        float shakeX = 0, shakeY = 0, shakeZ = 0;
        if (gameplay.shakeIntensity > 0) {
            shakeX = ((float)Math.random() - 0.5f) * gameplay.shakeIntensity;
            shakeY = ((float)Math.random() - 0.5f) * gameplay.shakeIntensity;
            shakeZ = ((float)Math.random() - 0.5f) * gameplay.shakeIntensity;
        }

        float dirX = (float) (Math.cos(Math.toRadians(gameplay.pitch)) * Math.sin(Math.toRadians(gameplay.yaw)));
        float dirY = (float) Math.sin(Math.toRadians(gameplay.pitch));
        float dirZ = (float) (-Math.cos(Math.toRadians(gameplay.pitch)) * Math.cos(Math.toRadians(gameplay.yaw)));

        Matrix.setLookAtM(viewMatrix, 0,
            gameplay.camX + shakeX, eyeHeight + shakeY, gameplay.camZ + shakeZ,
            gameplay.camX + dirX + shakeX, eyeHeight + dirY + shakeY, gameplay.camZ + dirZ + shakeZ,
            0f, 1f, 0f);
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(Booster.shaderProgram);
        GLES20.glUniform1f(Booster.timeHandle, gameplay.gameTime);

        world.render(vpMatrix, gameplay);
    }
}
