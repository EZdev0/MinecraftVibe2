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
    private float dX, dY;

    private Button[] hotbarButtons = new Button[6];
    public byte[] blockIds = {Blocks.GRASS, Blocks.STONE, Blocks.WOOD, Blocks.LEAVES, Blocks.TNT, Blocks.FIRE};
    public int[] inventory = {0, 0, 0, 0, 0, 0}; // Count per slot

    private Button sneakBtn, sprintBtn, flyBtn;

    public boolean showDebug = true;
        public boolean fastRender = false;
    public boolean uiEditorMode = false;
    public boolean showGLWarnings = true;
    public String currentGLError = null;

    public void reportGLError(String errorMsg) {
        if (!showGLWarnings) return;
        currentGLError = errorMsg;
        activity.runOnUiThread(() -> {
            McApp.triggerCrashHandler(activity, "OpenGL Error: " + errorMsg, true);
        });
    }

    public boolean tntUnlocked = false;

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
            final int slotIndex = i;
            btn.setOnClickListener(v -> {
                if(uiEditorMode) return;
                engine.gameplay.activeBlock = id;
                engine.gameplay.activeSlot = slotIndex;
                updateHotbarUI();
            });

            hotbarButtons[i] = btn;
            hotbar.addView(btn);
        }

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.setMargins(0, 0, 0, 50);

        float hX = prefs.getFloat("hotbar_X", -1); float hY = prefs.getFloat("hotbar_Y", -1);
        if(hX != -1) { hotbar.setX(hX); hotbar.setY(hY); }
        hotbar.setOnTouchListener((v, e) -> handleTouch(v, e, "hotbar"));

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
        // Jump Button
        Button jumpBtn = createBtn("⬆️", "#3498db");
        float jX = prefs.getFloat("jumpBtn_X", -1); float jY = prefs.getFloat("jumpBtn_Y", -1);
        FrameLayout.LayoutParams lpJump = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if(jX != -1) {
            lpJump.gravity = Gravity.TOP | Gravity.START;
            jumpBtn.setX(jX); jumpBtn.setY(jY);
        } else {
            lpJump.gravity = Gravity.BOTTOM | Gravity.END;
            lpJump.setMargins(0, 0, 50, 250);
        }
        jumpBtn.setOnTouchListener((v, e) -> {
            if(handleTouch(v, e, "jumpBtn")) return true;
            if(e.getAction() == MotionEvent.ACTION_DOWN) engine.gameplay.wantsToJump = true;
            else if(e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) engine.gameplay.wantsToJump = false;
            return true;
        });
        root.addView(jumpBtn, lpJump);

        // Break Button
        Button breakBtn = createBtn("⛏️", "#e74c3c");
        float bX = prefs.getFloat("breakBtn_X", -1); float bY = prefs.getFloat("breakBtn_Y", -1);
        FrameLayout.LayoutParams lpBreak = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if(bX != -1) {
            lpBreak.gravity = Gravity.TOP | Gravity.START;
            breakBtn.setX(bX); breakBtn.setY(bY);
        } else {
            lpBreak.gravity = Gravity.BOTTOM | Gravity.END;
            lpBreak.setMargins(0, 0, 200, 50);
        }
        breakBtn.setOnTouchListener((v, e) -> {
            if(handleTouch(v, e, "breakBtn")) return true;
            if(e.getAction() == MotionEvent.ACTION_DOWN) {
                if (engine.gameplay.isCreative) engine.world.interact(engine.gameplay, false, this);
                else engine.gameplay.isBreaking = true;
            } else if(e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                engine.gameplay.isBreaking = false;
                engine.gameplay.breakTimer = 0f;
            }
            return true;
        });
        root.addView(breakBtn, lpBreak);

        // Place Button
        Button placeBtn = createBtn("🧱", "#2ecc71");
        float pX = prefs.getFloat("placeBtn_X", -1); float pY = prefs.getFloat("placeBtn_Y", -1);
        FrameLayout.LayoutParams lpPlace = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if(pX != -1) {
            lpPlace.gravity = Gravity.TOP | Gravity.START;
            placeBtn.setX(pX); placeBtn.setY(pY);
        } else {
            lpPlace.gravity = Gravity.BOTTOM | Gravity.END;
            lpPlace.setMargins(0, 0, 50, 50);
        }
        placeBtn.setOnTouchListener((v, e) -> {
            if(handleTouch(v, e, "placeBtn")) return true;
            if(e.getAction() == MotionEvent.ACTION_DOWN) engine.world.interact(engine.gameplay, true, this);
            return true;
        });
        root.addView(placeBtn, lpPlace);

        if (!engine.gameplay.isCreative) {
            // Attack Button
            Button attackBtn = createBtn("⚔️", "#e67e22");
            float aX = prefs.getFloat("attackBtn_X", -1); float aY = prefs.getFloat("attackBtn_Y", -1);
            FrameLayout.LayoutParams lpAttack = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if(aX != -1) {
                lpAttack.gravity = Gravity.TOP | Gravity.START;
                attackBtn.setX(aX); attackBtn.setY(aY);
            } else {
                lpAttack.gravity = Gravity.BOTTOM | Gravity.END;
                lpAttack.setMargins(0, 0, 200, 250);
            }
            attackBtn.setOnTouchListener((v, e) -> {
                if(handleTouch(v, e, "attackBtn")) return true;
                if(e.getAction() == android.view.MotionEvent.ACTION_DOWN) engine.world.interact(engine.gameplay, false, this);
                return true;
            });
            root.addView(attackBtn, lpAttack);
        }
    }
    private void createToggles(FrameLayout root) {
        // Sneak Button
        sneakBtn = createBtn("Sneak", "#95a5a6");
        float snX = prefs.getFloat("sneakBtn_X", -1); float snY = prefs.getFloat("sneakBtn_Y", -1);
        FrameLayout.LayoutParams lpSneak = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if(snX != -1) {
            lpSneak.gravity = Gravity.TOP | Gravity.START;
            sneakBtn.setX(snX); sneakBtn.setY(snY);
        } else {
            lpSneak.gravity = Gravity.TOP | Gravity.START;
            lpSneak.setMargins(50, 150, 0, 0);
        }
        sneakBtn.setOnTouchListener((v, e) -> {
            if(handleTouch(v, e, "sneakBtn")) return true;
            if(e.getAction() == MotionEvent.ACTION_UP) {
                engine.gameplay.isSneaking = !engine.gameplay.isSneaking;
                sneakBtn.setBackgroundColor(Color.parseColor(engine.gameplay.isSneaking ? "#2ecc71" : "#95a5a6"));
            }
            return true;
        });
        root.addView(sneakBtn, lpSneak);

        // Sprint Button
        sprintBtn = createBtn("Sprint", "#95a5a6");
        float spX = prefs.getFloat("sprintBtn_X", -1); float spY = prefs.getFloat("sprintBtn_Y", -1);
        FrameLayout.LayoutParams lpSprint = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if(spX != -1) {
            lpSprint.gravity = Gravity.TOP | Gravity.START;
            sprintBtn.setX(spX); sprintBtn.setY(spY);
        } else {
            lpSprint.gravity = Gravity.TOP | Gravity.START;
            lpSprint.setMargins(50, 300, 0, 0);
        }
        sprintBtn.setOnTouchListener((v, e) -> {
            if(handleTouch(v, e, "sprintBtn")) return true;
            if(e.getAction() == MotionEvent.ACTION_UP) {
                engine.gameplay.isSprinting = !engine.gameplay.isSprinting;
                sprintBtn.setBackgroundColor(Color.parseColor(engine.gameplay.isSprinting ? "#2ecc71" : "#95a5a6"));
            }
            return true;
        });
        root.addView(sprintBtn, lpSprint);

        if (engine.gameplay.isCreative) {
            // Fly Button
            flyBtn = createBtn("Fly", "#95a5a6");
            float fX = prefs.getFloat("flyBtn_X", -1); float fY = prefs.getFloat("flyBtn_Y", -1);
            FrameLayout.LayoutParams lpFly = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if(fX != -1) {
                lpFly.gravity = Gravity.TOP | Gravity.START;
                flyBtn.setX(fX); flyBtn.setY(fY);
            } else {
                lpFly.gravity = Gravity.TOP | Gravity.START;
                lpFly.setMargins(50, 450, 0, 0);
            }
            flyBtn.setOnTouchListener((v, e) -> {
                if(handleTouch(v, e, "flyBtn")) return true;
                if(e.getAction() == MotionEvent.ACTION_UP) {
                    engine.gameplay.isFlying = !engine.gameplay.isFlying;
                    flyBtn.setBackgroundColor(Color.parseColor(engine.gameplay.isFlying ? "#2ecc71" : "#95a5a6"));
                }
                return true;
            });
            root.addView(flyBtn, lpFly);
        }
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

        Button sfxBtn = createBtn(prefs.getBoolean("SFX_ENABLED", true) ? "EFFEKTE: AN" : "EFFEKTE: AUS", "#3498db");
        sfxBtn.setOnClickListener(v -> {
            if (activity.soundManager != null) {
                activity.soundManager.toggleSFX();
                sfxBtn.setText(activity.soundManager.isEnabled() ? "EFFEKTE: AN" : "EFFEKTE: AUS");
            }
        });
        settingsPanel.addView(sfxBtn);

        settingsPanel.addView(createHeading("--- SYSTEM ---"));

                Button uiEditorBtn = createBtn(uiEditorMode ? "UI EDITOR: AN" : "UI EDITOR: AUS", "#f1c40f");
        uiEditorBtn.setOnClickListener(v -> {
            uiEditorMode = !uiEditorMode;
            uiEditorBtn.setText(uiEditorMode ? "UI EDITOR: AN" : "UI EDITOR: AUS");
            if(uiEditorMode) {
                android.widget.Toast.makeText(activity, "Verschiebe Buttons mit Touch. Druecke SCHLIESSEN zum Speichern.", android.widget.Toast.LENGTH_LONG).show();
            }
        });
        settingsPanel.addView(uiEditorBtn);

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
        private final StringBuilder sb = new StringBuilder(128);

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
                sb.setLength(0);
                sb.append("X: ").append((int)engine.gameplay.camX).append(" Y: ").append((int)engine.gameplay.camY).append(" Z: ").append((int)engine.gameplay.camZ);
                canvas.drawText(sb, 0, sb.length(), 30, 60, textPaint);

                sb.setLength(0);
                sb.append("FPS: ").append(engine.currentFPS);
                if(fastRender) sb.append(" [VULKAN ENABLED]");
                canvas.drawText(sb, 0, sb.length(), 30, 110, fpsPaint);
            }

            if (!engine.gameplay.isCreative) {
                // Heart Display ABOVE Hotbar! Hotbar is ~200px from bottom.
                sb.setLength(0);
                int hearts = (int)engine.gameplay.health / 2;
                for(int h=0; h<10; h++) {
                    if(h < hearts) sb.append("❤️");
                    else sb.append("🖤");
                }

                // Draw in center, above bottom
                canvas.drawText(sb, 0, sb.length(), getWidth() / 2f - 220, getHeight() - 200, healthPaint);

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

    private boolean handleTouch(View v, MotionEvent e, String idKey) {
        if(uiEditorMode) {
            if(e.getAction() == MotionEvent.ACTION_DOWN) {
                dX = v.getX() - e.getRawX();
                dY = v.getY() - e.getRawY();
            } else if(e.getAction() == MotionEvent.ACTION_MOVE) {
                v.setX(e.getRawX() + dX);
                v.setY(e.getRawY() + dY);
            } else if(e.getAction() == MotionEvent.ACTION_UP) {
                prefs.edit().putFloat(idKey + "_X", v.getX()).putFloat(idKey + "_Y", v.getY()).apply();
            }
            return true;
        }
        return false;
    }

}
