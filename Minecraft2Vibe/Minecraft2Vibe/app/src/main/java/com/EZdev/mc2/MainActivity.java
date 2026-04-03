package com.EZdev.mc2;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

    private GLSurfaceView surfaceView;
    public MyGdxGame engine;
    public UIManager uiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        surfaceView = new GLSurfaceView(this);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        engine = new MyGdxGame(this);
        surfaceView.setRenderer(engine);

        FrameLayout root = new FrameLayout(this);
        root.addView(surfaceView);

        uiManager = new UIManager(this, root, engine);

        setContentView(root);
    }

    @Override protected void onPause() { super.onPause(); surfaceView.onPause(); }
    @Override protected void onResume() { super.onResume(); surfaceView.onResume(); }
}