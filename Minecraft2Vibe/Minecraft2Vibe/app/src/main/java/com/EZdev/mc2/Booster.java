package com.EZdev.mc2;

import android.opengl.GLES20;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Booster {
    public static int shaderProgram, posHandle, colorHandle, mvpHandle;
    public static int fogEnabledHandle, fogEndHandle, timeHandle, isFlashingHandle, pTypeHandle;
    public static int texHandle, texCoordHandle, useTexHandle;
    public static FloatBuffer tntVertexBuffer, tntColorBuffer, quadTexCoordBuffer;
    public static int tntTexId = -1;


    public static void createTNTTexture() {
        if (tntTexId != -1) return;
        Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.rgb(200, 50, 50)); // Red base
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(24);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        canvas.drawText("TNT", 32, 40, paint);

        int[] textures = new int[1];
        android.opengl.GLES20.glGenTextures(1, textures, 0);
        tntTexId = textures[0];
        android.opengl.GLES20.glBindTexture(android.opengl.GLES20.GL_TEXTURE_2D, tntTexId);
        android.opengl.GLES20.glTexParameteri(android.opengl.GLES20.GL_TEXTURE_2D, android.opengl.GLES20.GL_TEXTURE_MIN_FILTER, android.opengl.GLES20.GL_NEAREST);
        android.opengl.GLES20.glTexParameteri(android.opengl.GLES20.GL_TEXTURE_2D, android.opengl.GLES20.GL_TEXTURE_MAG_FILTER, android.opengl.GLES20.GL_NEAREST);
        GLUtils.texImage2D(android.opengl.GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();
    }

    public static void initGeometry() {
        String v = "precision mediump float; precision mediump int; uniform mat4 uMVPMatrix; uniform float uTime; uniform int pType; attribute vec4 vPosition; attribute vec4 vColor; attribute vec2 aTexCoord; varying vec4 fColor; varying float vDist; varying vec2 vTexCoord; " +
                   "void main() { vec4 pos = vPosition; vTexCoord = aTexCoord; if (vColor.r > 0.8 && vColor.g > 0.4 && vColor.b < 0.2 && pType != " + Blocks.SMOKE + " && pType != " + Blocks.ENTITY_PIG + ") { float w = 1.0 - vColor.a; " +
                   "pos.x += sin(uTime * 15.0 + pos.y * 10.0) * 0.15 * w; pos.z += cos(uTime * 15.0 + pos.x * 10.0) * 0.15 * w; } " +
                   "gl_Position = uMVPMatrix * pos; fColor = vec4(vColor.rgb, 1.0); vDist = gl_Position.w; }";

        String f = "precision mediump float; precision mediump int; varying vec4 fColor; varying float vDist; varying vec2 vTexCoord; uniform int uFogEnabled; uniform float uFogEnd; uniform float uTime; uniform int uIsFlashing; uniform int pType; uniform sampler2D uTexture; uniform int uUseTex; " +
                   "void main() { vec4 fc = fColor; if (uUseTex == 1) { fc = texture2D(uTexture, vTexCoord); if (fc.a < 0.1) discard; } " +
                   "if (uIsFlashing == 1) { float p = (sin(uTime * 15.0) + 1.0) * 0.5; fc = mix(fc, vec4(1.0), p); } " +
                   "else if (uIsFlashing == 2 && pType == " + Blocks.SMOKE + ") { fc = vec4(0.8, 0.8, 0.8, 0.7); } " +
                   "else if (pType == " + Blocks.ENTITY_PIG + ") { fc = vec4(0.9, 0.6, 0.7, 1.0); } " +
                   "else if (pType == " + Blocks.GRASS + ") { fc = vec4(0.3, 0.7, 0.2, 1.0); } " +
                   "else if (pType == " + Blocks.DIRT + ") { fc = vec4(0.4, 0.25, 0.1, 1.0); } " +
                   "else if (pType == " + Blocks.STONE + ") { fc = vec4(0.4, 0.4, 0.4, 1.0); } " +
                   "else if (pType == " + Blocks.WOOD + ") { fc = vec4(0.4, 0.25, 0.1, 1.0); } " +
                   "else if (pType == " + Blocks.LEAVES + ") { fc = vec4(0.1, 0.5, 0.1, 1.0); } " +
                   "else if (pType == " + Blocks.FIRE + ") { fc = vec4(0.9, 0.5, 0.1, 1.0); } " +
                   "if (uFogEnabled == 1) { float ff = clamp((vDist - (uFogEnd * 0.4)) / (uFogEnd * 0.6), 0.0, 1.0); gl_FragColor = mix(fc, vec4(0.5, 0.8, 1.0, 1.0), ff); } " +
                   "else { gl_FragColor = fc; } }";

        int vs = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER); GLES20.glShaderSource(vs, v); GLES20.glCompileShader(vs);
        int fs = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER); GLES20.glShaderSource(fs, f); GLES20.glCompileShader(fs);
        shaderProgram = GLES20.glCreateProgram(); GLES20.glAttachShader(shaderProgram, vs); GLES20.glAttachShader(shaderProgram, fs); GLES20.glLinkProgram(shaderProgram);

        posHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        colorHandle = GLES20.glGetAttribLocation(shaderProgram, "vColor");
        texCoordHandle = GLES20.glGetAttribLocation(shaderProgram, "aTexCoord");
        mvpHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");
        fogEnabledHandle = GLES20.glGetUniformLocation(shaderProgram, "uFogEnabled");
        fogEndHandle = GLES20.glGetUniformLocation(shaderProgram, "uFogEnd");
        timeHandle = GLES20.glGetUniformLocation(shaderProgram, "uTime");
        isFlashingHandle = GLES20.glGetUniformLocation(shaderProgram, "uIsFlashing");
        pTypeHandle = GLES20.glGetUniformLocation(shaderProgram, "pType");
        texHandle = GLES20.glGetUniformLocation(shaderProgram, "uTexture");
        useTexHandle = GLES20.glGetUniformLocation(shaderProgram, "uUseTex");
        buildTnt();
    }

    private static void buildTnt() {
        float[] v = {-0.5f,0.5f,-0.5f, -0.5f,-0.5f,-0.5f, 0.5f,-0.5f,-0.5f, 0.5f,-0.5f,-0.5f, 0.5f,0.5f,-0.5f, -0.5f,0.5f,-0.5f, -0.5f,-0.5f,0.5f, -0.5f,-0.5f,-0.5f, -0.5f,0.5f,-0.5f, -0.5f,0.5f,-0.5f, -0.5f,0.5f,0.5f, -0.5f,-0.5f,0.5f, 0.5f,-0.5f,-0.5f, 0.5f,-0.5f,0.5f, 0.5f,0.5f,0.5f, 0.5f,0.5f,0.5f, 0.5f,0.5f,-0.5f, 0.5f,-0.5f,-0.5f, -0.5f,-0.5f,0.5f, 0.5f,-0.5f,0.5f, 0.5f,0.5f,0.5f, 0.5f,0.5f,0.5f, -0.5f,0.5f,0.5f, -0.5f,-0.5f,0.5f, -0.5f,0.5f,-0.5f, 0.5f,0.5f,-0.5f, 0.5f,0.5f,0.5f, 0.5f,0.5f,0.5f, -0.5f,0.5f,0.5f, -0.5f,0.5f,-0.5f, -0.5f,-0.5f,0.5f, -0.5f,-0.5f,-0.5f, 0.5f,-0.5f,-0.5f, 0.5f,-0.5f,-0.5f, 0.5f,-0.5f,0.5f, -0.5f,-0.5f,0.5f};
        float[] c = new float[v.length / 3 * 4]; for(int i=0; i<c.length; i+=4) { c[i]=0.9f; c[i+1]=0.2f; c[i+2]=0.2f; c[i+3]=1.0f; }
        tntVertexBuffer = ByteBuffer.allocateDirect(v.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(v); tntVertexBuffer.position(0);
        tntColorBuffer = ByteBuffer.allocateDirect(c.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(c); tntColorBuffer.position(0);

        float[] tc = {0,0, 0,1, 1,1, 1,1, 1,0, 0,0, 0,1, 0,0, 1,0, 1,0, 1,1, 0,1, 0,1, 1,1, 1,0, 1,0, 0,0, 0,1, 0,1, 1,1, 1,0, 1,0, 0,0, 0,1, 0,0, 1,0, 1,1, 1,1, 0,1, 0,0, 0,1, 0,0, 1,0, 1,0, 1,1, 0,1};
        quadTexCoordBuffer = ByteBuffer.allocateDirect(tc.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(tc); quadTexCoordBuffer.position(0);
        createTNTTexture();
    }
}
