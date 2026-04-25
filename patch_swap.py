import re

with open('./Minecraft2Vibe/Minecraft2Vibe/app/src/main/java/com/EZdev/mc2/MainMenuActivity.java', 'r') as f:
    content = f.read()

# Update showLoadingScreenAndOptimize
search = """    private void showLoadingScreenAndOptimize() {
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
            });
        }).start();
    }"""

replace = """    private void showLoadingScreenAndOptimize() {
        loadingPanel = new LinearLayout(this);
        loadingPanel.setOrientation(LinearLayout.VERTICAL);
        loadingPanel.setBackgroundColor(Color.parseColor("#000000"));
        loadingPanel.setGravity(Gravity.CENTER);

        TextView t = new TextView(this);
        t.setText("⚙️ MAGIC TUNER & SWAP NO ROOT OPTIMIERUNG LÄUFT... ⚙️\\nClear Cache & Allocate Virtual Memory...");
        t.setTextColor(Color.GREEN);
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

                // Create SWAP file logic safely
                File swapFile = new File(getCacheDir(), "swapfile.swp");
                RandomAccessFile raf = new RandomAccessFile(swapFile, "rw");

                // Real space check vs 4GB. Using 4GB swap or max available
                long spaceToAllocate = 4L * 1024 * 1024 * 1024; // 4GB
                long freeSpace = getCacheDir().getFreeSpace();
                if (freeSpace < spaceToAllocate) {
                    spaceToAllocate = Math.max(0, freeSpace - (100L * 1024 * 1024)); // leave 100mb
                }

                if (spaceToAllocate > 0) {
                    raf.setLength(spaceToAllocate);

                    // Actually force allocation by writing chunk at end
                    raf.seek(spaceToAllocate - 1);
                    raf.write(0);
                }
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
            });
        }).start();
    }"""

content = content.replace(search, replace)

with open('./Minecraft2Vibe/Minecraft2Vibe/app/src/main/java/com/EZdev/mc2/MainMenuActivity.java', 'w') as f:
    f.write(content)
