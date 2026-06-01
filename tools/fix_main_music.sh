#!/bin/bash
cat << 'INNER_EOF' > Minecraft2Vibe/Minecraft2Vibe/app/src/main/java/com/EZdev/mc2/MainActivity.java
package com.EZdev.mc2;

import android.app.Activity;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.widget.FrameLayout;
import java.io.File;

public class MainActivity extends Activity {

    private GLSurfaceView surfaceView;
    public MyGdxGame engine;
    public UIManager uiManager;
    public MusicManager musicManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean loadWorld = getIntent().getBooleanExtra("LOAD_WORLD", false);
        boolean isCreative = getIntent().getBooleanExtra("CREATIVE_MODE", false);

        if (!loadWorld) {
            // Delete old save directory to "Create New World"
            File dir = new File(getFilesDir(), "world1");
            if(dir.exists()) {
                String[] children = dir.list();
                for (int i = 0; i < children.length; i++) {
                    new File(dir, children[i]).delete();
                }
            }
        }

        surfaceView = new GLSurfaceView(this);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        engine = new MyGdxGame(this);
        // Set Creative mode
        engine.gameplay.isFlying = isCreative;

        surfaceView.setRenderer(engine);

        FrameLayout root = new FrameLayout(this);
        root.addView(surfaceView);

        uiManager = new UIManager(this, engine);
        uiManager.setupUI(root);

        setContentView(root);

        musicManager = new MusicManager(this);
        musicManager.startMusic();
    }

    @Override protected void onPause() { super.onPause(); surfaceView.onPause(); if (musicManager != null) musicManager.stopMusic(); }
    @Override protected void onResume() { super.onResume(); surfaceView.onResume(); if (musicManager != null && musicManager.isEnabled()) musicManager.startMusic(); }
}
INNER_EOF
