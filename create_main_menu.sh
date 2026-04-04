#!/bin/bash
cat << 'INNER_EOF' > Minecraft2Vibe/Minecraft2Vibe/app/src/main/java/com/EZdev/mc2/MainMenuActivity.java
package com.EZdev.mc2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainMenuActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#34495e")); // Dark blueish
        root.setGravity(Gravity.CENTER);

        TextView title = new TextView(this);
        title.setText("MINECRAFT 2 VIBE");
        title.setTextColor(Color.WHITE);
        title.setTextSize(40);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 100);
        root.addView(title);

        Button btnSurvival = createMenuBtn("NEUE WELT (SURVIVAL)");
        btnSurvival.setOnClickListener(v -> startGame(false));
        root.addView(btnSurvival);

        Button btnCreative = createMenuBtn("NEUE WELT (KREATIV)");
        btnCreative.setOnClickListener(v -> startGame(true));
        root.addView(btnCreative);

        Button btnLoad = createMenuBtn("WELT LADEN");
        btnLoad.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("LOAD_WORLD", true);
            startActivity(intent);
        });
        root.addView(btnLoad);

        setContentView(root);
    }

    private void startGame(boolean isCreative) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("CREATIVE_MODE", isCreative);
        intent.putExtra("LOAD_WORLD", false);
        startActivity(intent);
    }

    private Button createMenuBtn(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setBackgroundColor(Color.parseColor("#7f8c8d"));
        b.setTextColor(Color.WHITE);
        b.setTextSize(20);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(600, 150);
        p.setMargins(0, 20, 0, 20);
        b.setLayoutParams(p);
        return b;
    }
}
INNER_EOF
