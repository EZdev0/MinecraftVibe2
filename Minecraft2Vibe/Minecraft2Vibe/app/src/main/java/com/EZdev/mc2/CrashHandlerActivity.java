package com.EZdev.mc2;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

        sb.append("--- NETZWERK DIAGNOSE ---\n");
        sb.append("Internet Permission: ").append(checkSelfPermission(android.Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED).append("\n");
        sb.append("Network State Permission: ").append(checkSelfPermission(android.Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED).append("\n");

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm != null ? cm.getActiveNetworkInfo() : null;
        sb.append("Verbunden: ").append(ni != null && ni.isConnected()).append("\n");
        if (ni != null) sb.append("Typ: ").append(ni.getTypeName()).append("\n");
        sb.append("------------------------\n\n");

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
        if (intent != null) {
            String intentError = intent.getStringExtra("error");
            if (intentError != null) {
                rawError += "\nIntent Error: " + intentError;
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
        root.setBackgroundColor(Color.parseColor("#2c3e50"));
        root.setPadding(40, 40, 40, 40);

        TextView title = new TextView(this);
        title.setText(canContinue ? "NETZWERK / FEHLER MELDUNG" : "ABSTURZ / DISCONNECT");
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
        copyBtn.setText("KOMPLETTEN LOG KOPIEREN");
        copyBtn.setBackgroundColor(Color.WHITE);
        copyBtn.setTextColor(Color.BLACK);
        copyBtn.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Multiplayer Log", finalLog);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Log kopiert!", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(copyBtn);

        if (canContinue) {
            Button continueBtn = new Button(this);
            continueBtn.setText("ZURÜCK ZUM SPIEL");
            continueBtn.setBackgroundColor(Color.parseColor("#2ecc71"));
            continueBtn.setTextColor(Color.WHITE);
            continueBtn.setOnClickListener(v -> finish());
            root.addView(continueBtn);
        }

        Button restartBtn = new Button(this);
        restartBtn.setText("ZUM HAUPTMENÜ");
        restartBtn.setBackgroundColor(Color.WHITE);
        restartBtn.setTextColor(Color.BLACK);
        restartBtn.setOnClickListener(v -> {
            Intent i = new Intent(this, MainMenuActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
        root.addView(restartBtn);

        setContentView(root);
    }

    private String sanitize(String input) {
        if (input == null) return null;
        return input.replaceAll("[\\p{Cc}\\p{Cf}\\p{Co}\\p{Cn}&&[^\\n\\r\\t]]", "");
    }
}
