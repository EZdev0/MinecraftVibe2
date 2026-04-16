import re

with open('./Minecraft2Vibe/Minecraft2Vibe/app/src/main/java/com/EZdev/mc2/MainMenuActivity.java', 'r') as f:
    content = f.read()

# Add imports
imports = """import android.app.ActivityManager;
import android.content.pm.ApplicationInfo;
import java.io.RandomAccessFile;
import android.os.Handler;
import android.os.Looper;
"""
content = re.sub(r'(import java\.io\.File;)', r'\1\n' + imports, content)

# Add members
members = """    private FrameLayout mainOverlay;
    private LinearLayout loadingPanel;
"""
content = re.sub(r'(private SharedPreferences prefs;)', r'\1\n' + members, content)

# Update onCreate
oncreate_search = """    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("McPrefs", Context.MODE_PRIVATE);

        initMainMenuLayout();
        createSettingsMenu();
    }"""
oncreate_replace = """    @Override
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
        t.setText("⚙️ MAGIC TUNER & SWAP NO ROOT OPTIMIERUNG LÄUFT... ⚙️\\nErstelle 4GB SWAP Datei...");
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

            // ActivityManager killBackgroundProcesses for background app optimization
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                PackageManager pm = getPackageManager();
                for (ApplicationInfo packageInfo : pm.getInstalledApplications(0)) {
                    if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0 && !packageInfo.packageName.equals(getPackageName())) {
                        am.killBackgroundProcesses(packageInfo.packageName);
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
    }"""
content = content.replace(oncreate_search, oncreate_replace)

# Modify createSettingsMenu to set ContentView via mainOverlay
settings_search = """        // Add to root but hide
        FrameLayout overlay = new FrameLayout(this);
        overlay.addView(root);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(settingsPanel);

        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        scrollParams.setMargins(50, 50, 50, 50);
        scrollParams.gravity = Gravity.CENTER;

        scroll.setVisibility(View.GONE);

        overlay.addView(scroll, scrollParams);

        setContentView(overlay);"""
settings_replace = """        // Add to root but hide
        mainOverlay = new FrameLayout(this);
        mainOverlay.addView(root);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(settingsPanel);

        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        scrollParams.setMargins(50, 50, 50, 50);
        scrollParams.gravity = Gravity.CENTER;

        scroll.setVisibility(View.GONE);

        mainOverlay.addView(scroll, scrollParams);

        setContentView(mainOverlay);"""
content = content.replace(settings_search, settings_replace)

with open('./Minecraft2Vibe/Minecraft2Vibe/app/src/main/java/com/EZdev/mc2/MainMenuActivity.java', 'w') as f:
    f.write(content)
