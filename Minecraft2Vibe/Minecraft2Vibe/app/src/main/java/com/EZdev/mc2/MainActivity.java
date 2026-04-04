package com.EZdev.mc2;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.widget.FrameLayout;
import java.io.File;

public class MainActivity extends Activity {

    private GLSurfaceView surfaceView;
    public MyGdxGame engine;
    public UIManager uiManager;
    public MusicManager musicManager;
    public SoundManager soundManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean loadWorld = getIntent().getBooleanExtra("LOAD_WORLD", false);
        boolean isCreative = getIntent().getBooleanExtra("CREATIVE_MODE", false);

        if (!loadWorld) {
            File dir = new File(getFilesDir(), "world1");
            if(dir.exists() && dir.listFiles() != null) {
                for (File f : dir.listFiles()) {
                    f.delete();
                }
            }
        }

        surfaceView = new GLSurfaceView(this);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        engine = new MyGdxGame(this);
        engine.gameplay.isCreative = isCreative;
        if(isCreative) engine.gameplay.isFlying = true;

        surfaceView.setRenderer(engine);

        FrameLayout root = new FrameLayout(this);
        root.addView(surfaceView);

        uiManager = new UIManager(this, engine);
        uiManager.setupUI(root);

        setContentView(root);

        musicManager = new MusicManager(this);
        musicManager.startMusic();

        soundManager = new SoundManager(this);
    }

    @Override protected void onPause() { super.onPause(); surfaceView.onPause(); if (musicManager != null) musicManager.stopMusic(); }
    @Override protected void onResume() { super.onResume(); surfaceView.onResume(); if (musicManager != null && musicManager.isEnabled()) musicManager.startMusic(); }
    @Override protected void onDestroy() { super.onDestroy(); if (soundManager != null) soundManager.release(); }
}
