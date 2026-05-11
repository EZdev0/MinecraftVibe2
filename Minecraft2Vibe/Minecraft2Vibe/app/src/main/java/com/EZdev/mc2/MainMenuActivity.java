package com.EZdev.mc2;

import android.widget.FrameLayout;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;


public class MainMenuActivity extends Activity {

    private static final String TAG = "MainMenuActivity";
    private LinearLayout root, settingsPanel;
    private SharedPreferences prefs;
    private FrameLayout mainOverlay;
    private LinearLayout loadingPanel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("McPrefs", Context.MODE_PRIVATE);

        initMainMenuLayout();
        createSettingsMenu();

        showLoadingScreenAndOptimize();
    }

    private void showLoadingScreenAndOptimize() {
        loadingPanel = new LinearLayout(this);
        loadingPanel.setOrientation(LinearLayout.VERTICAL);
        loadingPanel.setBackgroundColor(getColor(R.color.black));
        loadingPanel.setGravity(Gravity.CENTER);

        TextView t = new TextView(this);
        t.setText("⚙️ MAGIC TUNER OPTIMIERUNG LÄUFT... ⚙️\nClear Cache...");
        t.setTextColor(getColor(R.color.green));
        t.setTextSize(20);
        t.setGravity(Gravity.CENTER);
        loadingPanel.addView(t);

        mainOverlay.addView(loadingPanel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        new Thread(() -> {
            try {
                // Clear cache directory
                File[] cacheFiles = getCacheDir().listFiles();
                if (cacheFiles != null) {
                    for (File f : cacheFiles) {
                        f.delete();
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to optimize", e);
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                mainOverlay.removeView(loadingPanel);
            });
        }).start();
    }

    private void initMainMenuLayout() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(R.color.menu_background));
        root.setGravity(Gravity.CENTER);

        TextView title = new TextView(this);
        title.setText("MINECRAFT 2 VIBE");
        title.setTextColor(getColor(R.color.white));
        title.setTextSize(40);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 80);
        root.addView(title);

        Button btnSurvival = createMenuBtn("NEUE WELT (SURVIVAL)");
        btnSurvival.setOnClickListener(v -> startGame(false, false));
        root.addView(btnSurvival);

        Button btnCreative = createMenuBtn("NEUE WELT (KREATIV)");
        btnCreative.setOnClickListener(v -> startGame(true, false));
        root.addView(btnCreative);

        File dir = new File(getFilesDir(), "world1");
        if (dir.exists() && dir.listFiles() != null && dir.listFiles().length > 0) {
            Button btnLoadS = createMenuBtn("LADEN (SURVIVAL)");
            btnLoadS.setBackgroundColor(getColor(R.color.button_load));
            btnLoadS.setOnClickListener(v -> startGame(false, true));
            root.addView(btnLoadS);

            Button btnLoadC = createMenuBtn("LADEN (KREATIV)");
            btnLoadC.setBackgroundColor(getColor(R.color.button_load));
            btnLoadC.setOnClickListener(v -> startGame(true, true));
            root.addView(btnLoadC);
        }

        Button btnSettings = createMenuBtn("EINSTELLUNGEN");
        btnSettings.setBackgroundColor(getColor(R.color.button_settings));
        btnSettings.setOnClickListener(v -> showSettings());
        root.addView(btnSettings);
    }

    private void startGame(boolean isCreative, boolean loadWorld) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("CREATIVE_MODE", isCreative);
        intent.putExtra("LOAD_WORLD", loadWorld);
        startActivity(intent);
    }

    private Button createMenuBtn(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setBackgroundColor(getColor(R.color.button_default));
        b.setTextColor(getColor(R.color.white));
        b.setTextSize(20);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(600, 120);
        p.setMargins(0, 15, 0, 15);
        b.setLayoutParams(p);
        return b;
    }

    private void createSettingsMenu() {
        settingsPanel = new LinearLayout(this);
        settingsPanel.setOrientation(LinearLayout.VERTICAL);
        settingsPanel.setBackgroundColor(getColor(R.color.settings_panel_background));
        settingsPanel.setPadding(60, 60, 60, 60);
        settingsPanel.setClickable(true);

        TextView title = new TextView(this);
        title.setText("EINSTELLUNGEN");
        title.setTextColor(getColor(R.color.white));
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        settingsPanel.addView(title);

        setupRenderDistanceControls();
        setupSettingsToggles();

        Button closeBtn = createMenuBtn("SCHLIESSEN");
        closeBtn.setBackgroundColor(getColor(R.color.button_secondary));
        closeBtn.setOnClickListener(v -> hideSettings());
        settingsPanel.addView(closeBtn);

        // Add to root but hide
        mainOverlay = new FrameLayout(this);
        mainOverlay.addView(root);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(settingsPanel);

        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        scrollParams.setMargins(50, 50, 50, 50);
        scrollParams.gravity = Gravity.CENTER;

        scroll.setVisibility(View.GONE);

        mainOverlay.addView(scroll, scrollParams);

        setContentView(mainOverlay);
    }

    private void setupRenderDistanceControls() {
        final TextView chunkText = new TextView(this);
        int currentDist = prefs.getInt("RENDER_DISTANCE", 2);
        chunkText.setText("Sichtweite (Chunks): " + currentDist);
        chunkText.setTextColor(getColor(R.color.yellow));
        chunkText.setTextSize(20);
        chunkText.setPadding(0, 50, 0, 50);
        chunkText.setGravity(Gravity.CENTER);

        LinearLayout plusMinus = new LinearLayout(this);
        plusMinus.setOrientation(LinearLayout.HORIZONTAL);

        Button minusBtn = createMenuBtn("- WENIGER");
        minusBtn.setBackgroundColor(getColor(R.color.button_minus));
        Button plusBtn = createMenuBtn("+ MEHR");
        plusBtn.setBackgroundColor(getColor(R.color.button_plus));

        LinearLayout.LayoutParams btnP = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        btnP.setMargins(10, 10, 10, 10);
        minusBtn.setLayoutParams(btnP);
        plusBtn.setLayoutParams(btnP);

        plusMinus.addView(minusBtn);
        plusMinus.addView(plusBtn);

        minusBtn.setOnClickListener(v -> {
            int d = prefs.getInt("RENDER_DISTANCE", 2);
            if (d > 1) {
                d--;
                prefs.edit().putInt("RENDER_DISTANCE", d).apply();
                chunkText.setText("Sichtweite (Chunks): " + d);
            }
        });
        plusBtn.setOnClickListener(v -> {
            int d = prefs.getInt("RENDER_DISTANCE", 2);
            if (d < 6) {
                d++;
                prefs.edit().putInt("RENDER_DISTANCE", d).apply();
                chunkText.setText("Sichtweite (Chunks): " + d);
            }
        });

        settingsPanel.addView(chunkText);
        settingsPanel.addView(plusMinus);
    }

    private void setupSettingsToggles() {
        boolean isFog = prefs.getBoolean("FOG_ENABLED", true);
        Button fogBtn = createMenuBtn(isFog ? "NEBEL: AN" : "NEBEL: AUS");
        fogBtn.setBackgroundColor(getColor(R.color.button_purple));
        fogBtn.setOnClickListener(v -> {
            boolean f = !prefs.getBoolean("FOG_ENABLED", true);
            prefs.edit().putBoolean("FOG_ENABLED", f).apply();
            fogBtn.setText(f ? "NEBEL: AN" : "NEBEL: AUS");
        });

        boolean isFast = prefs.getBoolean("FAST_RENDER", false);
        Button vulkanBtn = createMenuBtn(isFast ? "VULKAN (FAST RENDER): AN" : "VULKAN (FAST RENDER): AUS");
        vulkanBtn.setBackgroundColor(getColor(R.color.button_orange));
        vulkanBtn.setOnClickListener(v -> {
            boolean f = !prefs.getBoolean("FAST_RENDER", false);
            prefs.edit().putBoolean("FAST_RENDER", f).apply();
            vulkanBtn.setText(f ? "VULKAN (FAST RENDER): AN" : "VULKAN (FAST RENDER): AUS");
        });

        boolean isMusic = prefs.getBoolean("MUSIC_ENABLED", true);
        Button musicBtn = createMenuBtn(isMusic ? "MUSIK: AN" : "MUSIK: AUS");
        musicBtn.setBackgroundColor(getColor(R.color.button_teal));
        musicBtn.setOnClickListener(v -> {
            boolean m = !prefs.getBoolean("MUSIC_ENABLED", true);
            prefs.edit().putBoolean("MUSIC_ENABLED", m).apply();
            musicBtn.setText(m ? "MUSIK: AN" : "MUSIK: AUS");
        });

        settingsPanel.addView(fogBtn);
        settingsPanel.addView(vulkanBtn);
        settingsPanel.addView(musicBtn);

        boolean isSfx = prefs.getBoolean("SFX_ENABLED", true);
        Button sfxBtn = createMenuBtn(isSfx ? "EFFEKTE: AN" : "EFFEKTE: AUS");
        sfxBtn.setBackgroundColor(getColor(R.color.button_blue));
        sfxBtn.setOnClickListener(v -> {
            boolean s = !prefs.getBoolean("SFX_ENABLED", true);
            prefs.edit().putBoolean("SFX_ENABLED", s).apply();
            sfxBtn.setText(s ? "EFFEKTE: AN" : "EFFEKTE: AUS");
        });
        settingsPanel.addView(sfxBtn);

        boolean isDebug = prefs.getBoolean("SHOW_DEBUG", false);
        Button debugBtn = createMenuBtn(isDebug ? "DEBUG INFO: AN" : "DEBUG INFO: AUS");
        debugBtn.setBackgroundColor(getColor(R.color.button_purple));
        debugBtn.setOnClickListener(v -> {
            boolean d = !prefs.getBoolean("SHOW_DEBUG", false);
            prefs.edit().putBoolean("SHOW_DEBUG", d).apply();
            debugBtn.setText(d ? "DEBUG INFO: AN" : "DEBUG INFO: AUS");
        });
        settingsPanel.addView(debugBtn);

        boolean isGLWarn = prefs.getBoolean("GL_WARN", false);
        Button glWarnBtn = createMenuBtn(isGLWarn ? "GL-WARNUNGEN: AN" : "GL-WARNUNGEN: AUS");
        glWarnBtn.setBackgroundColor(getColor(R.color.button_settings));
        glWarnBtn.setOnClickListener(v -> {
            boolean g = !prefs.getBoolean("GL_WARN", false);
            prefs.edit().putBoolean("GL_WARN", g).apply();
            glWarnBtn.setText(g ? "GL-WARNUNGEN: AN" : "GL-WARNUNGEN: AUS");
        });
        settingsPanel.addView(glWarnBtn);
    }

    private void showSettings() {
        ((View)settingsPanel.getParent()).setVisibility(View.VISIBLE);
    }
    private void hideSettings() {
        ((View) settingsPanel.getParent()).setVisibility(View.GONE);
    }
}
