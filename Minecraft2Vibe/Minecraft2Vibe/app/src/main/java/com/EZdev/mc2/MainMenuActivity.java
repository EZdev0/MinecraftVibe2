package com.EZdev.mc2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;

public class MainMenuActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
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

        setContentView(root);
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
}
