import re

with open('./Minecraft2Vibe/Minecraft2Vibe/app/src/main/java/com/EZdev/mc2/MyGdxGame.java', 'r') as f:
    content = f.read()

# Update onSurfaceChanged to handle 0 values safely
surface_old = r'''    public void onSurfaceChanged\(GL10 gl, int width, int height\) \{
        GLES20\.glViewport\(0, 0, width, height\);
        screenRatio = \(float\)width/height;
        Matrix\.perspectiveM\(projectionMatrix, 0, 70f, screenRatio, 0\.1f, 300f\);
    \}'''

surface_new = '''    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (height == 0) height = 1;
        GLES20.glViewport(0, 0, width, height);
        screenRatio = (float)width/height;
        Matrix.perspectiveM(projectionMatrix, 0, 70f, screenRatio, 0.1f, 300f);
    }'''

content = re.sub(surface_old, surface_new, content)

with open('./Minecraft2Vibe/Minecraft2Vibe/app/src/main/java/com/EZdev/mc2/MyGdxGame.java', 'w') as f:
    f.write(content)

print("MyGdxGame patched successfully.")
