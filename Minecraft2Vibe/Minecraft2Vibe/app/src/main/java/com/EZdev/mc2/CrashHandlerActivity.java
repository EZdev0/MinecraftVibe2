package com.EZdev.mc2;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class CrashHandlerActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String errorLog = getIntent().getStringExtra("error");
        if (errorLog == null) errorLog = "Unbekannter Fehler!";

        boolean canContinue = getIntent().getBooleanExtra("canContinue", false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#c0392b"));
        root.setPadding(40, 40, 40, 40);

        TextView title = new TextView(this);
        title.setText(canContinue ? "FEHLER ERKANNT" : "MINECRAFT 2 VIBE IST ABGESTÜRZT :(");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        ScrollView scroll = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        scrollParams.setMargins(0, 40, 0, 40);
        scroll.setLayoutParams(scrollParams);

        TextView logView = new TextView(this);
        logView.setText(errorLog);
        logView.setTextColor(Color.parseColor("#ecf0f1"));
        logView.setTextSize(14);
        scroll.addView(logView);
        root.addView(scroll);

        final String finalLog = errorLog;
        Button copyBtn = new Button(this);
        copyBtn.setText("LOG KOPIEREN");
        copyBtn.setBackgroundColor(Color.WHITE);
        copyBtn.setTextColor(Color.BLACK);
        copyBtn.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Crash Log", finalLog);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Log kopiert!", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(copyBtn);

        if (canContinue) {
            Button continueBtn = new Button(this);
            continueBtn.setText("FORTFAHREN");
            continueBtn.setBackgroundColor(Color.parseColor("#2ecc71"));
            continueBtn.setTextColor(Color.WHITE);
            continueBtn.setOnClickListener(v -> {
                finish();
            });
            root.addView(continueBtn);
        }

        Button restartBtn = new Button(this);
        restartBtn.setText("NEUSTART (HAUPTMENÜ)");
        restartBtn.setBackgroundColor(Color.WHITE);
        restartBtn.setTextColor(Color.BLACK);
        restartBtn.setOnClickListener(v -> {
            Intent i = new Intent(this, MainMenuActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            if (!canContinue) {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }
        });
        root.addView(restartBtn);

        setContentView(root);
    }
}
