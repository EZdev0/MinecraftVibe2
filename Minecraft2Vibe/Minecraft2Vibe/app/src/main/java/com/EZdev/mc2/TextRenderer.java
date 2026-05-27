package com.EZdev.mc2;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class TextRenderer {
    public static void init() {}

    public static int createTextTexture(String text) {
        Bitmap bitmap = Bitmap.createBitmap(256, 64, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        bitmap.eraseColor(0);
        Paint paint = new Paint();
        paint.setTextSize(32); paint.setAntiAlias(true); paint.setARGB(255, 255, 255, 255);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, 128, 45, paint);
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        int id = textures[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();
        return id;
    }

    public static void deleteTexture(int id) {
        if (id != -1) GLES20.glDeleteTextures(1, new int[]{id}, 0);
    }
}
