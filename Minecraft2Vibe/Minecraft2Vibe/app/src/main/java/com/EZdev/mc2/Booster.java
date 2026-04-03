package com.EZdev.mc2;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Booster {
    public static int shaderProgram, posHandle, colorHandle, mvpHandle;
    public static int fogEnabledHandle, fogEndHandle, timeHandle, isFlashingHandle;
    public static FloatBuffer tntVertexBuffer, tntColorBuffer;

    public static void initGeometry() {
        String v = "uniform mat4 uMVPMatrix; uniform float uTime; attribute vec4 vPosition; attribute vec4 vColor; varying vec4 fColor; varying float vDist; " +
                   "void main() { vec4 pos = vPosition; if (vColor.r > 0.8 && vColor.g > 0.4 && vColor.b < 0.2) { float w = 1.0 - vColor.a; " +
                   "pos.x += sin(uTime * 15.0 + pos.y * 10.0) * 0.15 * w; pos.z += cos(uTime * 15.0 + pos.x * 10.0) * 0.15 * w; } " +
                   "gl_Position = uMVPMatrix * pos; fColor = vec4(vColor.rgb, 1.0); vDist = gl_Position.w; }";
        String f = "precision mediump float; varying vec4 fColor; varying float vDist; uniform int uFogEnabled; uniform float uFogEnd; uniform float uTime; uniform int uIsFlashing; " +
                   "void main() { vec4 fc = fColor; if (uIsFlashing == 1) { float p = (sin(uTime * 15.0) + 1.0) * 0.5; fc = mix(fColor, vec4(1.0), p); } " +
                   "if (uFogEnabled == 1) { float ff = clamp((vDist - (uFogEnd * 0.4)) / (uFogEnd * 0.6), 0.0, 1.0); gl_FragColor = mix(fc, vec4(0.5, 0.8, 1.0, 1.0), ff); } " +
                   "else { gl_FragColor = fc; } }";
        int vs = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER); GLES20.glShaderSource(vs, v); GLES20.glCompileShader(vs);
        int fs = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER); GLES20.glShaderSource(fs, f); GLES20.glCompileShader(fs);
        shaderProgram = GLES20.glCreateProgram(); GLES20.glAttachShader(shaderProgram, vs); GLES20.glAttachShader(shaderProgram, fs); GLES20.glLinkProgram(shaderProgram);
        posHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition"); colorHandle = GLES20.glGetAttribLocation(shaderProgram, "vColor");
        mvpHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix"); fogEnabledHandle = GLES20.glGetUniformLocation(shaderProgram, "uFogEnabled");
        fogEndHandle = GLES20.glGetUniformLocation(shaderProgram, "uFogEnd"); timeHandle = GLES20.glGetUniformLocation(shaderProgram, "uTime");
        isFlashingHandle = GLES20.glGetUniformLocation(shaderProgram, "uIsFlashing");
        buildTnt();
    }

    private static void buildTnt() {
        float[] v = new float[36*3]; float[] c = new float[36*4]; int cS = 0;
        float[][] fs = {{0,1,0, 0,1,1, 1,1,1, 0,1,0, 1,1,1, 1,1,0}, {0,0,1, 0,0,0, 1,0,0, 0,0,1, 1,0,0, 1,0,1}, {0,1,0, 0,0,0, 0,0,1, 0,1,0, 0,0,1, 0,1,1}, {1,1,1, 1,0,1, 1,0,0, 1,1,1, 1,0,0, 1,1,0}, {1,1,0, 1,0,0, 0,0,0, 1,1,0, 0,0,0, 0,1,0}, {0,1,1, 0,0,1, 1,0,1, 0,1,1, 1,0,1, 1,1,1}};
        for(int i=0; i<6; i++) { for(int j=0; j<18; j++) v[cS*3 + j] = fs[i][j]; for(int j=0; j<6; j++) { c[cS*4+j*4]=0.9f; c[cS*4+j*4+1]=0.2f; c[cS*4+j*4+2]=0.2f; c[cS*4+j*4+3]=1.0f; } cS+=6; }
        tntVertexBuffer = ByteBuffer.allocateDirect(v.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer(); tntVertexBuffer.put(v).position(0);
        tntColorBuffer = ByteBuffer.allocateDirect(c.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer(); tntColorBuffer.put(c).position(0);
    }
}