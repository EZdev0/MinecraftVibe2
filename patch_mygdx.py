import re

with open('./Minecraft2Vibe/Minecraft2Vibe/app/src/main/java/com/EZdev/mc2/MyGdxGame.java', 'r') as f:
    content = f.read()

imports = """import android.content.Context;
import android.content.SharedPreferences;
"""
content = re.sub(r'(import javax\.microedition\.khronos\.opengles\.GL10;)', r'\1\n' + imports, content)

search = """    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.8f, 1.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);

        Booster.initGeometry();
        lastTime = System.nanoTime();
        lastFPSUpdate = System.currentTimeMillis();
    }"""

replace = """    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.8f, 1.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);

        if (activity != null) {
            SharedPreferences prefs = activity.getSharedPreferences("McPrefs", Context.MODE_PRIVATE);
            if (prefs.getBoolean("FAST_RENDER", false)) {
                // Disable VSync using EGL14 to boost FPS
                try {
                    android.opengl.EGL14.eglSwapInterval(android.opengl.EGL14.eglGetCurrentDisplay(), 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        Booster.initGeometry();
        lastTime = System.nanoTime();
        lastFPSUpdate = System.currentTimeMillis();
    }"""

content = content.replace(search, replace)

with open('./Minecraft2Vibe/Minecraft2Vibe/app/src/main/java/com/EZdev/mc2/MyGdxGame.java', 'w') as f:
    f.write(content)
