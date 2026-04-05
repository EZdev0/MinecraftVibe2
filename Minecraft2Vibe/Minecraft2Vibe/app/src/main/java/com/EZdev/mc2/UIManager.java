package com.EZdev.mc2;

import android.content.Context;
import android.widget.ScrollView;
import android.graphics.Typeface;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class UIManager {
    private MainActivity activity;
    private MyGdxGame engine;
    private SharedPreferences prefs;

    public TouchOverlay touchOverlay;
    private LinearLayout settingsPanel;
    private TextView chunkText;

    private Button[] hotbarButtons = new Button[6];
    public byte[] blockIds = {1, 2, 3, 4, 5, 6};
    public int[] inventory = {0, 0, 0, 0, 0, 0}; // Count per slot

    private Button sneakBtn, sprintBtn, flyBtn;

    public boolean showDebug = true;
        public boolean fastRender = false;
    public boolean showGLWarnings = true;
    public String currentGLError = null;

    public void reportGLError(String errorMsg) {
        if (!showGLWarnings) return;
        currentGLError = errorMsg;
        activity.runOnUiThread(() -> {
            android.content.Intent intent = new android.content.Intent(activity, CrashHandlerActivity.class);
            intent.putExtra("error", "OpenGL Error: " + errorMsg);
            intent.putExtra("canContinue", true);
            activity.startActivity(intent);
        });
    }

    private boolean tntUnlocked = false;

    public UIManager(MainActivity activity, MyGdxGame engine) {
        this.activity = activity;
        this.engine = engine;
        this.prefs = activity.getSharedPreferences("McPrefs", Context.MODE_PRIVATE);
        engine.world.renderDistance = prefs.getInt("RENDER_DISTANCE", 2);
        engine.world.fogEnabled = prefs.getBoolean("FOG_ENABLED", true);
        showDebug = prefs.getBoolean("SHOW_DEBUG", true);
                fastRender = prefs.getBoolean("FAST_RENDER", false);
        showGLWarnings = prefs.getBoolean("GL_WARN", true);
        tntUnlocked = prefs.getBoolean("TNT_UNLOCKED", false);

        if (engine.gameplay.isCreative) {
            for(int i=0; i<6; i++) inventory[i] = 999;
        } else {
            // Start survival with nothing except maybe a dirt block
            inventory[0] = 5;
            // Fire (Flint&Steel) is unlimited if unlocked
            if (prefs.getBoolean("FIRE_UNLOCKED", false)) inventory[5] = 999;
        }
    }

    public int addToInventory(byte type, int amount) {
        for(int i=0; i<blockIds.length; i++) {
            if(blockIds[i] == type) {
                inventory[i] += amount;
                int rest = 0;
                if(inventory[i] > 64 && inventory[i] != 999) {
                    rest = inventory[i] - 64;
                    inventory[i] = 64;
                }

                if (type == 6 && !prefs.getBoolean("FIRE_UNLOCKED", false)) {
                    prefs.edit().putBoolean("FIRE_UNLOCKED", true).apply();
                    inventory[5] = 999;
                    activity.runOnUiThread(() -> android.widget.Toast.makeText(activity, "ACHIEVEMENT: FEUERZEUG FREIGESCHALTET!", android.widget.Toast.LENGTH_LONG).show());
                }
                if (type == 5 && !tntUnlocked) {
                    tntUnlocked = true;
                    prefs.edit().putBoolean("TNT_UNLOCKED", true).apply();
                    activity.runOnUiThread(() -> android.widget.Toast.makeText(activity, "ACHIEVEMENT: TNT GEFUNDEN!", android.widget.Toast.LENGTH_LONG).show());
                }

                activity.runOnUiThread(this::updateHotbarUI);
                return rest;
            }
        }
        return amount;
    }
    public void setupUI(FrameLayout root) {
        touchOverlay = new TouchOverlay(activity);
        root.addView(touchOverlay);

        createCrosshair(root);
        createHotbar(root);
        createActionButtons(root);
        createToggles(root);
        createSettingsMenu(root);

        updateHotbarUI();
    }

    private void createCrosshair(FrameLayout root) {
        View crosshair = new View(activity);
        crosshair.setBackgroundColor(Color.WHITE);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(6, 6);
        params.gravity = Gravity.CENTER;
        root.addView(crosshair, params);
    }

    private void createHotbar(FrameLayout root) {
        LinearLayout hotbar = new LinearLayout(activity);
        hotbar.setOrientation(LinearLayout.HORIZONTAL);
        hotbar.setBackgroundColor(Color.parseColor("#80000000"));
        hotbar.setPadding(15, 15, 15, 15);

        String[] icons = {"🟫", "🪨", "🪵", "🍃", "🧨", "🔥"};

        for (int i = 0; i < icons.length; i++) {
            Button btn = new Button(activity);
            btn.setTextSize(20);

            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(120, 120);
            p.setMargins(10, 0, 10, 0);
            btn.setLayoutParams(p);
            btn.setPadding(0, 0, 0, 0);

            final byte id = blockIds[i];
            btn.setOnClickListener(v -> {
                engine.gameplay.activeBlock = id;
                updateHotbarUI();
            });

            hotbarButtons[i] = btn;
            hotbar.addView(btn);
        }

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.setMargins(0, 0, 0, 50);
        root.addView(hotbar, params);
    }

    public void updateHotbarUI() {
        String[] icons = {"🟫", "🪨", "🪵", "🍃", "🧨", "🔥"};
        for (int i = 0; i < hotbarButtons.length; i++) {
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);

            if (engine.gameplay.activeBlock == blockIds[i]) {
                shape.setStroke(8, Color.YELLOW);
            } else {
                shape.setStroke(4, Color.parseColor("#222222"));
            }

            if (engine.gameplay.isCreative) {
                shape.setColor(Color.parseColor("#555555"));
                hotbarButtons[i].setText(icons[i]);
            } else {
                if (inventory[i] > 0) {
                    shape.setColor(Color.parseColor("#555555"));
                    if(i == 5 && inventory[i] == 999) hotbarButtons[i].setText(icons[i]); // Infinite fire
                    else hotbarButtons[i].setText(icons[i] + "\n" + inventory[i]);
                } else {
                    shape.setColor(Color.parseColor("#88111111"));
                    hotbarButtons[i].setText(icons[i] + "\n0");
                }
            }
            hotbarButtons[i].setBackground(shape);
        }
    }

    private void createActionButtons(FrameLayout root) {
        LinearLayout btnLayout = new LinearLayout(activity);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button jumpBtn = createBtn("⬆️", "#3498db");
        jumpBtn.setOnTouchListener((v, e) -> {
            if(e.getAction() == MotionEvent.ACTION_DOWN) engine.gameplay.wantsToJump = true;
            else if(e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) engine.gameplay.wantsToJump = false;
            return true;
        });

        Button breakBtn = createBtn("⛏️", "#e74c3c");
        breakBtn.setOnTouchListener((v, e) -> {
            if(e.getAction() == MotionEvent.ACTION_DOWN) {
                if (engine.gameplay.isCreative) engine.world.interact(engine.gameplay, false, this);
                else engine.gameplay.isBreaking = true;
            } else if(e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                engine.gameplay.isBreaking = false;
                engine.gameplay.breakTimer = 0f;
            }
            return true;
        });

        Button placeBtn = createBtn("🧱", "#2ecc71");
        placeBtn.setOnTouchListener((v, e) -> { if(e.getAction() == MotionEvent.ACTION_DOWN) engine.world.interact(engine.gameplay, true, this); return true; });

        btnLayout.addView(jumpBtn); btnLayout.addView(breakBtn); btnLayout.addView(placeBtn);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM | Gravity.END;
        params.setMargins(0, 0, 50, 50);
        root.addView(btnLayout, params);
    }

    private void createToggles(FrameLayout root) {
        LinearLayout btnLayout = new LinearLayout(activity);
        btnLayout.setOrientation(LinearLayout.VERTICAL);

        sneakBtn = createBtn("Sneak", "#95a5a6");
        sneakBtn.setOnClickListener(v -> {
            engine.gameplay.isSneaking = !engine.gameplay.isSneaking;
            sneakBtn.setBackgroundColor(Color.parseColor(engine.gameplay.isSneaking ? "#2ecc71" : "#95a5a6"));
        });

        sprintBtn = createBtn("Sprint", "#95a5a6");
        sprintBtn.setOnClickListener(v -> {
            engine.gameplay.isSprinting = !engine.gameplay.isSprinting;
            sprintBtn.setBackgroundColor(Color.parseColor(engine.gameplay.isSprinting ? "#2ecc71" : "#95a5a6"));
        });

        flyBtn = createBtn("Fly", "#95a5a6");
        flyBtn.setOnClickListener(v -> {
            if (engine.gameplay.isCreative) {
                engine.gameplay.isFlying = !engine.gameplay.isFlying;
                flyBtn.setBackgroundColor(Color.parseColor(engine.gameplay.isFlying ? "#2ecc71" : "#95a5a6"));
            }
        });

        if (engine.gameplay.isCreative) {
            btnLayout.addView(flyBtn);
        }
        btnLayout.addView(sprintBtn);
        btnLayout.addView(sneakBtn);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.setMargins(50, 150, 0, 0);
        root.addView(btnLayout, params);
    }

    private android.widget.TextView createHeading(String text) {
        android.widget.TextView h = new android.widget.TextView(activity);
        h.setText(text); h.setTextColor(Color.CYAN); h.setTextSize(18); h.setTypeface(null, Typeface.BOLD);
        h.setPadding(0, 40, 0, 10); h.setGravity(Gravity.CENTER);
        return h;
    }

    private void createSettingsMenu(FrameLayout root) {
        ScrollView scroll = new ScrollView(activity);
        scroll.setVisibility(View.GONE);

        settingsPanel = new LinearLayout(activity);
        settingsPanel.setOrientation(LinearLayout.VERTICAL);
        settingsPanel.setBackgroundColor(Color.parseColor("#E62c3e50"));
        settingsPanel.setPadding(60, 60, 60, 60);
        settingsPanel.setClickable(true);

        android.widget.TextView title = new android.widget.TextView(activity);
        title.setText("EINSTELLUNGEN"); title.setTextColor(Color.WHITE); title.setTextSize(26); title.setGravity(Gravity.CENTER);
        settingsPanel.addView(title);

        settingsPanel.addView(createHeading("--- GRAFIK ---"));

        chunkText = new android.widget.TextView(activity);
        chunkText.setText("Sichtweite (Chunks): " + engine.world.renderDistance);
        chunkText.setTextColor(Color.YELLOW); chunkText.setTextSize(20); chunkText.setPadding(0, 20, 0, 20); chunkText.setGravity(Gravity.CENTER);
        settingsPanel.addView(chunkText);

        LinearLayout plusMinus = new LinearLayout(activity);
        plusMinus.setOrientation(LinearLayout.HORIZONTAL);
        Button minusBtn = createBtn("- WENIGER", "#e74c3c");
        minusBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button plusBtn = createBtn("+ MEHR", "#2ecc71");
        plusBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        plusMinus.addView(minusBtn); plusMinus.addView(plusBtn);
        minusBtn.setOnClickListener(v -> { if(engine.world.renderDistance > 1) { engine.world.renderDistance--; updatePrefs(); } });
        plusBtn.setOnClickListener(v -> { if(engine.world.renderDistance < 6) { engine.world.renderDistance++; updatePrefs(); } });
        settingsPanel.addView(plusMinus);

        Button fogBtn = createBtn(engine.world.fogEnabled ? "NEBEL: AN" : "NEBEL: AUS", "#9b59b6");
        fogBtn.setOnClickListener(v -> {
            engine.world.fogEnabled = !engine.world.fogEnabled;
            prefs.edit().putBoolean("FOG_ENABLED", engine.world.fogEnabled).apply();
            fogBtn.setText(engine.world.fogEnabled ? "NEBEL: AN" : "NEBEL: AUS");
        });
        settingsPanel.addView(fogBtn);

        Button vulkanBtn = createBtn(fastRender ? "VULKAN (FAST RENDER): AN" : "VULKAN (FAST RENDER): AUS", "#e67e22");
        vulkanBtn.setOnClickListener(v -> {
            fastRender = !fastRender;
            prefs.edit().putBoolean("FAST_RENDER", fastRender).apply();
            vulkanBtn.setText(fastRender ? "VULKAN (FAST RENDER): AN" : "VULKAN (FAST RENDER): AUS");
        });
        settingsPanel.addView(vulkanBtn);

        settingsPanel.addView(createHeading("--- AUDIO ---"));

        Button musicBtn = createBtn(prefs.getBoolean("MUSIC_ENABLED", true) ? "MUSIK: AN" : "MUSIK: AUS", "#1abc9c");
        musicBtn.setOnClickListener(v -> {
            if (activity.musicManager != null) {
                activity.musicManager.toggleMusic();
                musicBtn.setText(activity.musicManager.isEnabled() ? "MUSIK: AN" : "MUSIK: AUS");
            }
        });
        settingsPanel.addView(musicBtn);

        settingsPanel.addView(createHeading("--- SYSTEM ---"));

        Button debugBtn = createBtn(showDebug ? "DEBUG INFO: AN" : "DEBUG INFO: AUS", "#9b59b6");
        debugBtn.setOnClickListener(v -> {
            showDebug = !showDebug;
            prefs.edit().putBoolean("SHOW_DEBUG", showDebug).apply();
            debugBtn.setText(showDebug ? "DEBUG INFO: AN" : "DEBUG INFO: AUS");
        });
        settingsPanel.addView(debugBtn);

        Button glWarnBtn = createBtn(showGLWarnings ? "GL-WARNUNGEN: AN" : "GL-WARNUNGEN: AUS", "#8e44ad");
        glWarnBtn.setOnClickListener(v -> {
            showGLWarnings = !showGLWarnings;
            prefs.edit().putBoolean("GL_WARN", showGLWarnings).apply();
            glWarnBtn.setText(showGLWarnings ? "GL-WARNUNGEN: AN" : "GL-WARNUNGEN: AUS");
            if (!showGLWarnings) currentGLError = null;
        });
        settingsPanel.addView(glWarnBtn);

        Button closeBtn = createBtn("SCHLIESSEN", "#95a5a6");
        closeBtn.setOnClickListener(v -> { scroll.setVisibility(View.GONE); touchOverlay.setVisibility(View.VISIBLE); });
        settingsPanel.addView(closeBtn);

        Button menuBtn = createBtn("HAUPTMENUE", "#c0392b");
        menuBtn.setOnClickListener(v -> {
            if (activity.musicManager != null) activity.musicManager.stopMusic();
            activity.finish();
        });
        settingsPanel.addView(menuBtn);

        scroll.addView(settingsPanel);

                FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        panelParams.setMargins(50, 50, 50, 50);
        panelParams.gravity = Gravity.CENTER;
        root.addView(scroll, panelParams);


        Button gearBtn = createBtn("⚙️", "#34495e");
        gearBtn.setOnClickListener(v -> { scroll.setVisibility(View.VISIBLE); touchOverlay.setVisibility(View.GONE); });

        FrameLayout.LayoutParams gearParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gearParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL; gearParams.setMargins(0, 20, 0, 0);
        root.addView(gearBtn, gearParams);
    }
    private void updatePrefs() {
        prefs.edit().putInt("RENDER_DISTANCE", engine.world.renderDistance).apply();
        chunkText.setText("Sichtweite (Chunks): " + engine.world.renderDistance);
    }

    private Button createBtn(String text, String color) {
        Button b = new Button(activity); b.setText(text); b.setBackgroundColor(Color.parseColor(color)); b.setTextColor(Color.WHITE);
        b.setPadding(30, 30, 30, 30);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f);
        p.setMargins(10, 10, 10, 10); b.setLayoutParams(p); return b;
    }

    public class TouchOverlay extends View {
        private Paint p = new Paint(); private Paint textPaint = new Paint(); private Paint fpsPaint = new Paint();
        private Paint healthPaint = new Paint(); private Paint blockBreakPaint = new Paint();
        private int joyId = -1, lookId = -1;
        private float joyBaseX, joyBaseY, joyKnobX, joyKnobY, lastLookX, lastLookY;

        public TouchOverlay(Context c) {
            super(c);
            textPaint.setColor(Color.WHITE); textPaint.setTextSize(36); textPaint.setFakeBoldText(true); textPaint.setShadowLayer(5, 2, 2, Color.BLACK);
            fpsPaint.setColor(Color.YELLOW); fpsPaint.setTextSize(36); fpsPaint.setFakeBoldText(true); fpsPaint.setShadowLayer(5, 2, 2, Color.BLACK);
            healthPaint.setColor(Color.RED); healthPaint.setTextSize(45); healthPaint.setFakeBoldText(true); healthPaint.setShadowLayer(5, 2, 2, Color.BLACK);
            blockBreakPaint.setColor(Color.WHITE); blockBreakPaint.setStrokeWidth(10); blockBreakPaint.setStyle(Paint.Style.STROKE);
        }

        @Override protected void onDraw(Canvas canvas) {
            if (engine == null || engine.gameplay == null) return;

            if (showDebug) {
                String coords = "X: " + (int)engine.gameplay.camX + " Y: " + (int)engine.gameplay.camY + " Z: " + (int)engine.gameplay.camZ;
                String fpsStr = "FPS: " + engine.currentFPS;
                if(fastRender) fpsStr += " [VULKAN ENABLED]";

                canvas.drawText(coords, 30, 60, textPaint);
                canvas.drawText(fpsStr, 30, 110, fpsPaint);
            }

            if (!engine.gameplay.isCreative) {
                // Heart Display ABOVE Hotbar! Hotbar is ~200px from bottom.
                String hpStr = "";
                int hearts = (int)engine.gameplay.health / 2;
                for(int h=0; h<10; h++) {
                    if(h < hearts) hpStr += "❤️";
                    else hpStr += "🖤";
                }

                // Draw in center, above bottom
                canvas.drawText(hpStr, getWidth() / 2f - 220, getHeight() - 200, healthPaint);

                // Fire Overlay effect if taking fire damage
                if (engine.gameplay.fireDamageTimer > 0) {
                    Paint fireOverlay = new Paint();
                    fireOverlay.setColor(Color.argb(100, 255, 100, 0));
                    canvas.drawRect(0, 0, getWidth(), getHeight(), fireOverlay);
                }

                // Block Breaking Progress Circle in middle
                if(engine.gameplay.isBreaking && engine.gameplay.breakTimer > 0.05f) {
                    float maxTime = 1.0f; // Simplified, normally check block type
                    float progress = (engine.gameplay.breakTimer / maxTime) * 360f;
                    if(progress > 360f) progress = 360f;
                    canvas.drawArc(getWidth()/2f - 40, getHeight()/2f - 40, getWidth()/2f + 40, getHeight()/2f + 40, -90, progress, false, blockBreakPaint);
                }
            }

            p.setColor(Color.WHITE); p.setStrokeWidth(5);
            canvas.drawLine(getWidth()/2f - 20, getHeight()/2f, getWidth()/2f + 20, getHeight()/2f, p);
            canvas.drawLine(getWidth()/2f, getHeight()/2f - 20, getWidth()/2f, getHeight()/2f + 20, p);

            if(joyId != -1) { p.setColor(Color.argb(100,0,0,0)); canvas.drawCircle(joyBaseX, joyBaseY, 150, p); p.setColor(Color.WHITE); canvas.drawCircle(joyKnobX, joyKnobY, 70, p); }
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (engine == null || engine.gameplay == null) return true;
            int action = e.getActionMasked(), pIndex = e.getActionIndex(), pId = e.getPointerId(pIndex);
            float x = e.getX(pIndex), y = e.getY(pIndex);

            if(action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                if(x < getWidth()/3f && joyId == -1) { joyId = pId; joyBaseX = x; joyBaseY = y; joyKnobX = x; joyKnobY = y; }
                else if(x >= getWidth()/3f && lookId == -1) { lookId = pId; lastLookX = x; lastLookY = y; }
            } else if(action == MotionEvent.ACTION_MOVE) {
                for(int i=0; i<e.getPointerCount(); i++) {
                    float currX = e.getX(i), currY = e.getY(i);
                    if(e.getPointerId(i) == joyId) {
                        float dx = currX - joyBaseX, dy = currY - joyBaseY, dist = (float)Math.sqrt(dx*dx + dy*dy);
                        if(dist > 150) { joyKnobX = joyBaseX + dx*(150/dist); joyKnobY = joyBaseY + dy*(150/dist); } else { joyKnobX = currX; joyKnobY = currY; }
                        engine.gameplay.joyMoveX = (joyKnobX - joyBaseX) / 150f; engine.gameplay.joyMoveY = (joyKnobY - joyBaseY) / 150f;
                    } else if(e.getPointerId(i) == lookId) {
                        engine.gameplay.yaw += (currX - lastLookX) * 0.2f; engine.gameplay.pitch -= (currY - lastLookY) * 0.2f;
                        if(engine.gameplay.pitch > 89) engine.gameplay.pitch = 89; if(engine.gameplay.pitch < -89) engine.gameplay.pitch = -89;
                        lastLookX = currX; lastLookY = currY;
                    }
                }
            } else if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
                if(pId == joyId) { joyId = -1; engine.gameplay.joyMoveX = 0; engine.gameplay.joyMoveY = 0; }
                if(pId == lookId) lookId = -1;
            }
            return true;
        }
    }
}
