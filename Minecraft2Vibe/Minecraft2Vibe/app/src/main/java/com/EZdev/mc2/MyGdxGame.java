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
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        Matrix.perspectiveM(projectionMatrix, 0, 70f, (float)width/height, 0.1f, 300f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        long now = System.nanoTime();
        float dt = (now - lastTime) / 1000000000.0f;
        lastTime = now;

        gameplay.update(dt, world);
        world.updateChunks(gameplay.camX, gameplay.camZ);

        if (activity != null && activity.uiManager != null && activity.uiManager.touchOverlay != null) {
            activity.uiManager.touchOverlay.postInvalidate();
        }

        float eyeHeight = gameplay.camY + gameplay.playerHeight - 0.2f;
        float dirX = (float) (Math.cos(Math.toRadians(gameplay.pitch)) * Math.sin(Math.toRadians(gameplay.yaw)));
        float dirY = (float) Math.sin(Math.toRadians(gameplay.pitch));
        float dirZ = (float) (-Math.cos(Math.toRadians(gameplay.pitch)) * Math.cos(Math.toRadians(gameplay.yaw)));

        Matrix.setLookAtM(viewMatrix, 0,
            gameplay.camX, eyeHeight, gameplay.camZ,
            gameplay.camX + dirX, eyeHeight + dirY, gameplay.camZ + dirZ,
            0f, 1f, 0f);
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Übergibt die Spielzeit an die Grafikkarte (für Feuer und TNT-Blinken)
        GLES20.glUseProgram(Booster.shaderProgram);
        GLES20.glUniform1f(Booster.timeHandle, gameplay.gameTime);

        world.render(vpMatrix, gameplay);
    }
}