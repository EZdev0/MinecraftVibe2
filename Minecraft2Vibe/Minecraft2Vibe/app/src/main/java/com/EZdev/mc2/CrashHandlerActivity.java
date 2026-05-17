package com.EZdev.mc2;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class CrashHandlerActivity extends Activity {
    private static final int MAX_ERROR_LOG_LENGTH = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String errorLog = "Unbekannter Fehler!";
        final boolean canContinue;

        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = openFileInput("crash_log.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            deleteFile("crash_log.txt");
        } catch (IOException e) {
            Log.e("CrashHandlerActivity", "Failed to read crash log", e);
        }

        String rawError = sb.toString().trim();
        if (rawError.isEmpty() && intent != null) {
            String intentError = intent.getStringExtra("error");
            if (intentError != null) {
                rawError = intentError;
            }
        }

        if (!rawError.isEmpty()) {
            if (rawError.length() > MAX_ERROR_LOG_LENGTH) {
                rawError = rawError.substring(0, MAX_ERROR_LOG_LENGTH) + "... [Truncated]";
            }
            errorLog = sanitize(rawError);
        }

        if (intent != null) {
            canContinue = intent.getBooleanExtra("canContinue", false);
        } else {
            canContinue = false;
        }

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

    private String sanitize(String input) {
        if (input == null) return null;
        // Strip potential malicious control characters, format characters,
        // private use and unassigned characters while keeping newlines and tabs.
        // This prevents UI spoofing (e.g. RTL override) and other character-based attacks.
        return input.replaceAll("[\\p{Cc}\\p{Cf}\\p{Co}\\p{Cn}&&[^\\n\\r\\t]]", "");
    }
}
