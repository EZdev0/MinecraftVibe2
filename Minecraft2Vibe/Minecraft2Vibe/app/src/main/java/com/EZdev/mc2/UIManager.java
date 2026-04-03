package com.EZdev.mc2;

import android.content.Context;
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

public class UIManager {
    private MainActivity activity;
    private MyGdxGame engine;
    private SharedPreferences prefs;

    public TouchOverlay touchOverlay;
    private LinearLayout settingsPanel;
    private TextView chunkText;

    // Arrays für die Hotbar
    private Button[] hotbarButtons = new Button[6];
    private byte[] blockIds = {1, 2, 3, 4, 5, 6}; // Erde, Stein, Holz, Blätter, TNT, Feuer(zeug)

    public UIManager(MainActivity activity, FrameLayout root, MyGdxGame engine) {
        this.activity = activity;
        this.engine = engine;
        this.prefs = activity.getSharedPreferences("MC_VIBE_PREFS", Context.MODE_PRIVATE);

        engine.world.renderDistance = prefs.getInt("RENDER_DISTANCE", 2);
        engine.world.fogEnabled = prefs.getBoolean("FOG_ENABLED", true);

        touchOverlay = new TouchOverlay(activity);
        root.addView(touchOverlay);

        createHotbar(root);
        createActionButtons(root);
        createSettingsMenu(root);

        updateHotbarUI(); // Setzt die erste Markierung
    }

    private void createHotbar(FrameLayout root) {
        LinearLayout hotbar = new LinearLayout(activity);
        hotbar.setOrientation(LinearLayout.HORIZONTAL);
        hotbar.setBackgroundColor(Color.parseColor("#80000000"));
        hotbar.setPadding(15, 15, 15, 15);

        String[] icons = {"🟫", "🪨", "🪵", "🍃", "🧨", "🔥"};

        for (int i = 0; i < icons.length; i++) {
            Button btn = new Button(activity);
            btn.setText(icons[i]);
            btn.setTextSize(26);

            // EXAKTE QUADRATE FÜR MINECRAFT-LOOK
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(120, 120);
            p.setMargins(10, 0, 10, 0);
            btn.setLayoutParams(p);
            btn.setPadding(0, 0, 0, 0); // Kein Text-Quetschen

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

    // Von außen aufrufbar (z.B. wenn TNT platziert wird und auf Feuerzeug gewechselt wird)
    public void updateHotbarUI() {
        for (int i = 0; i < hotbarButtons.length; i++) {
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setColor(Color.parseColor("#555555"));

            // Wenn ausgewählt: Dicker gelber Rahmen!
            if (engine.gameplay.activeBlock == blockIds[i]) {
                shape.setStroke(8, Color.YELLOW);
            } else {
                shape.setStroke(4, Color.parseColor("#222222"));
            }
            hotbarButtons[i].setBackground(shape);
        }
    }

    private void createActionButtons(FrameLayout root) {
        LinearLayout btnLayout = new LinearLayout(activity);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button jumpBtn = createBtn("⬆️", "#3498db");
        jumpBtn.setOnTouchListener((v, e) -> { if(e.getAction() == MotionEvent.ACTION_DOWN) engine.gameplay.jump(); return true; });

        Button breakBtn = createBtn("⛏️", "#e74c3c");
        breakBtn.setOnTouchListener((v, e) -> { if(e.getAction() == MotionEvent.ACTION_DOWN) engine.world.interact(engine.gameplay, false, this); return true; });

        Button placeBtn = createBtn("🧱", "#2ecc71");
        placeBtn.setOnTouchListener((v, e) -> { if(e.getAction() == MotionEvent.ACTION_DOWN) engine.world.interact(engine.gameplay, true, this); return true; });

        btnLayout.addView(jumpBtn); btnLayout.addView(breakBtn); btnLayout.addView(placeBtn);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM | Gravity.END;
        params.setMargins(0, 0, 50, 50);
        root.addView(btnLayout, params);
    }

    private void createSettingsMenu(FrameLayout root) {
        settingsPanel = new LinearLayout(activity);
        settingsPanel.setOrientation(LinearLayout.VERTICAL);
        settingsPanel.setBackgroundColor(Color.parseColor("#E62c3e50"));
        settingsPanel.setPadding(60, 60, 60, 60);
        settingsPanel.setVisibility(View.GONE);
        settingsPanel.setClickable(true);

        TextView title = new TextView(activity);
        title.setText("EINSTELLUNGEN"); title.setTextColor(Color.WHITE); title.setTextSize(26); title.setGravity(Gravity.CENTER);

        chunkText = new TextView(activity);
        chunkText.setText("Sichtweite (Chunks): " + engine.world.renderDistance);
        chunkText.setTextColor(Color.YELLOW); chunkText.setTextSize(20); chunkText.setPadding(0, 50, 0, 50); chunkText.setGravity(Gravity.CENTER);

        LinearLayout plusMinus = new LinearLayout(activity);
        plusMinus.setOrientation(LinearLayout.HORIZONTAL);
        Button minusBtn = createBtn("- WENIGER", "#e74c3c"); Button plusBtn = createBtn("+ MEHR", "#2ecc71");
        plusMinus.addView(minusBtn); plusMinus.addView(plusBtn);

        Button fogBtn = createBtn(engine.world.fogEnabled ? "NEBEL: AN" : "NEBEL: AUS", "#9b59b6");
        fogBtn.setOnClickListener(v -> {
            engine.world.fogEnabled = !engine.world.fogEnabled;
            prefs.edit().putBoolean("FOG_ENABLED", engine.world.fogEnabled).apply();
            fogBtn.setText(engine.world.fogEnabled ? "NEBEL: AN" : "NEBEL: AUS");
        });

        minusBtn.setOnClickListener(v -> { if(engine.world.renderDistance > 1) { engine.world.renderDistance--; updatePrefs(); } });
        plusBtn.setOnClickListener(v -> { if(engine.world.renderDistance < 6) { engine.world.renderDistance++; updatePrefs(); } });

        Button closeBtn = createBtn("SCHLIESSEN", "#95a5a6");
        closeBtn.setOnClickListener(v -> { settingsPanel.setVisibility(View.GONE); touchOverlay.setVisibility(View.VISIBLE); });

        settingsPanel.addView(title); settingsPanel.addView(chunkText); settingsPanel.addView(plusMinus); settingsPanel.addView(fogBtn); settingsPanel.addView(closeBtn);

        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(800, ViewGroup.LayoutParams.WRAP_CONTENT);
        panelParams.gravity = Gravity.CENTER;
        root.addView(settingsPanel, panelParams);

        Button gearBtn = createBtn("⚙️", "#34495e");
        gearBtn.setOnClickListener(v -> { settingsPanel.setVisibility(View.VISIBLE); touchOverlay.setVisibility(View.GONE); });

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
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(10, 10, 10, 10); b.setLayoutParams(p); return b;
    }

    public class TouchOverlay extends View {
        private Paint p = new Paint(); private Paint textPaint = new Paint();
        private int joyId = -1, lookId = -1;
        private float joyBaseX, joyBaseY, joyKnobX, joyKnobY, lastLookX, lastLookY;

        public TouchOverlay(Context c) {
            super(c);
            textPaint.setColor(Color.WHITE); textPaint.setTextSize(40); textPaint.setFakeBoldText(true); textPaint.setShadowLayer(5, 2, 2, Color.BLACK);
        }

        @Override protected void onDraw(Canvas canvas) {
            String coords = "X: " + (int)engine.gameplay.camX + " Y: " + (int)engine.gameplay.camY + " Z: " + (int)engine.gameplay.camZ;
            canvas.drawText(coords, 30, 60, textPaint);

            p.setColor(Color.WHITE); p.setStrokeWidth(5);
            canvas.drawLine(getWidth()/2f - 20, getHeight()/2f, getWidth()/2f + 20, getHeight()/2f, p);
            canvas.drawLine(getWidth()/2f, getHeight()/2f - 20, getWidth()/2f, getHeight()/2f + 20, p);

            if(joyId != -1) { p.setColor(Color.argb(100,0,0,0)); canvas.drawCircle(joyBaseX, joyBaseY, 150, p); p.setColor(Color.WHITE); canvas.drawCircle(joyKnobX, joyKnobY, 70, p); }
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
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