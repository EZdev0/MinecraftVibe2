package com.EZdev.mc2;

import android.widget.FrameLayout;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import android.app.ActivityManager;
import android.content.pm.ApplicationInfo;
import java.io.RandomAccessFile;
import android.os.Handler;
import android.os.Looper;


public class MainMenuActivity extends Activity {

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
        loadingPanel.setBackgroundColor(Color.parseColor("#000000"));
        loadingPanel.setGravity(Gravity.CENTER);

        TextView t = new TextView(this);
        t.setText("⚙️ MAGIC TUNER & SWAP NO ROOT OPTIMIERUNG LÄUFT... ⚙️\nErstelle 4GB SWAP Datei...");
        t.setTextColor(Color.GREEN);
        t.setTextSize(20);
        t.setGravity(Gravity.CENTER);
        loadingPanel.addView(t);

        mainOverlay.addView(loadingPanel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        new Thread(() -> {
            // Create 4GB SWAP file
            try {
                File swapFile = new File(getCacheDir(), "swapfile.swp");
                if (swapFile.exists()) swapFile.delete();
                RandomAccessFile raf = new RandomAccessFile(swapFile, "rw");
                raf.setLength(4L * 1024 * 1024 * 1024); // 4GB
                raf.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Try killing background apps with su
            boolean rootSuccess = false;
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "am kill-all"});
                p.waitFor();
                rootSuccess = p.exitValue() == 0;
            } catch (Exception e) {
                // Ignore
            }

            // Fallback to ActivityManager killBackgroundProcesses
            if (!rootSuccess) {
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    PackageManager pm = getPackageManager();
                    for (ApplicationInfo packageInfo : pm.getInstalledApplications(0)) {
                        if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0 && !packageInfo.packageName.equals(getPackageName())) {
                            am.killBackgroundProcesses(packageInfo.packageName);
                        }
                    }
                }
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                mainOverlay.removeView(loadingPanel);

                // Request Shizuku Permission
                if (checkSelfPermission("moe.shizuku.manager.permission.API_V23") != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{"moe.shizuku.manager.permission.API_V23"}, 100);
                }
            });
        }).start();
    }

    private void initMainMenuLayout() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#34495e"));
        root.setGravity(Gravity.CENTER);

        TextView title = new TextView(this);
        title.setText("MINECRAFT 2 VIBE");
        title.setTextColor(Color.WHITE);
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
            btnLoadS.setBackgroundColor(Color.parseColor("#f39c12"));
            btnLoadS.setOnClickListener(v -> startGame(false, true));
            root.addView(btnLoadS);

            Button btnLoadC = createMenuBtn("LADEN (KREATIV)");
            btnLoadC.setBackgroundColor(Color.parseColor("#f39c12"));
            btnLoadC.setOnClickListener(v -> startGame(true, true));
            root.addView(btnLoadC);
        }

        Button btnSettings = createMenuBtn("EINSTELLUNGEN");
        btnSettings.setBackgroundColor(Color.parseColor("#8e44ad"));
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
        b.setBackgroundColor(Color.parseColor("#7f8c8d"));
        b.setTextColor(Color.WHITE);
        b.setTextSize(20);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(600, 120);
        p.setMargins(0, 15, 0, 15);
        b.setLayoutParams(p);
        return b;
    }

    private void createSettingsMenu() {
        settingsPanel = new LinearLayout(this);
        settingsPanel.setOrientation(LinearLayout.VERTICAL);
        settingsPanel.setBackgroundColor(Color.parseColor("#E62c3e50"));
        settingsPanel.setPadding(60, 60, 60, 60);
        settingsPanel.setClickable(true);

        TextView title = new TextView(this);
        title.setText("EINSTELLUNGEN");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        settingsPanel.addView(title);

        setupRenderDistanceControls();
        setupSettingsToggles();

        Button closeBtn = createMenuBtn("SCHLIESSEN");
        closeBtn.setBackgroundColor(Color.parseColor("#95a5a6"));
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
        chunkText.setTextColor(Color.YELLOW);
        chunkText.setTextSize(20);
        chunkText.setPadding(0, 50, 0, 50);
        chunkText.setGravity(Gravity.CENTER);

        LinearLayout plusMinus = new LinearLayout(this);
        plusMinus.setOrientation(LinearLayout.HORIZONTAL);

        Button minusBtn = createMenuBtn("- WENIGER");
        minusBtn.setBackgroundColor(Color.parseColor("#e74c3c"));
        Button plusBtn = createMenuBtn("+ MEHR");
        plusBtn.setBackgroundColor(Color.parseColor("#2ecc71"));

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
        fogBtn.setBackgroundColor(Color.parseColor("#9b59b6"));
        fogBtn.setOnClickListener(v -> {
            boolean f = !prefs.getBoolean("FOG_ENABLED", true);
            prefs.edit().putBoolean("FOG_ENABLED", f).apply();
            fogBtn.setText(f ? "NEBEL: AN" : "NEBEL: AUS");
        });

        boolean isFast = prefs.getBoolean("FAST_RENDER", false);
        Button vulkanBtn = createMenuBtn(isFast ? "VULKAN (FAST RENDER): AN" : "VULKAN (FAST RENDER): AUS");
        vulkanBtn.setBackgroundColor(Color.parseColor("#e67e22"));
        vulkanBtn.setOnClickListener(v -> {
            boolean f = !prefs.getBoolean("FAST_RENDER", false);
            prefs.edit().putBoolean("FAST_RENDER", f).apply();
            vulkanBtn.setText(f ? "VULKAN (FAST RENDER): AN" : "VULKAN (FAST RENDER): AUS");
        });

        boolean isMusic = prefs.getBoolean("MUSIC_ENABLED", true);
        Button musicBtn = createMenuBtn(isMusic ? "MUSIK: AN" : "MUSIK: AUS");
        musicBtn.setBackgroundColor(Color.parseColor("#1abc9c"));
        musicBtn.setOnClickListener(v -> {
            boolean m = !prefs.getBoolean("MUSIC_ENABLED", true);
            prefs.edit().putBoolean("MUSIC_ENABLED", m).apply();
            musicBtn.setText(m ? "MUSIK: AN" : "MUSIK: AUS");
        });

        settingsPanel.addView(fogBtn);
        settingsPanel.addView(vulkanBtn);
        settingsPanel.addView(musicBtn);
    }

    private void showSettings() {
        ((View)settingsPanel.getParent()).setVisibility(View.VISIBLE);
    }
    private void hideSettings() {
        ((View) settingsPanel.getParent()).setVisibility(View.GONE);
    }
}
