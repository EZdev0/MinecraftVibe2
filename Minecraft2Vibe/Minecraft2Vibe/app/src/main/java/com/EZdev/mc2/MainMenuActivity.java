package com.EZdev.mc2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class MainMenuActivity extends Activity {
    private SharedPreferences prefs;
    private FrameLayout mainOverlay;
    private LinearLayout root, multiplayerPanel, serverList;
    private ScrollView multiplayerScroll;
    private MultiplayerManager dummyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("McPrefs", Context.MODE_PRIVATE);
        dummyManager = new MultiplayerManager(prefs.getString("PLAYER_NAME", "Player"), null, null);

        initMainMenuLayout();
        createMultiplayerMenu();

        mainOverlay = new FrameLayout(this);
        mainOverlay.addView(root);
        mainOverlay.addView(multiplayerScroll);
        setContentView(mainOverlay);
    }

    private void initMainMenuLayout() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getResources().getColor(R.color.menu_background));
        root.setGravity(Gravity.CENTER);

        TextView title = new TextView(this);
        title.setText("MINECRAFT 2 VIBE");
        title.setTextColor(getResources().getColor(R.color.white));
        title.setTextSize(40);
        title.setPadding(0, 0, 0, 80);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        Button btnSurvival = createMenuBtn("NEUE WELT (SURVIVAL)");
        btnSurvival.setOnClickListener(v -> startGame(false, false, false, null));
        root.addView(btnSurvival);

        Button btnCreative = createMenuBtn("NEUE WELT (KREATIV)");
        btnCreative.setOnClickListener(v -> startGame(true, false, false, null));
        root.addView(btnCreative);

        File dir = new File(getFilesDir(), "world1");
        if (dir.exists() && dir.listFiles() != null && dir.listFiles().length > 0) {
            Button btnLoadS = createMenuBtn("LADEN (SURVIVAL)");
            btnLoadS.setBackgroundColor(getResources().getColor(R.color.button_load));
            btnLoadS.setOnClickListener(v -> startGame(false, true, false, null));
            root.addView(btnLoadS);

            Button btnLoadC = createMenuBtn("LADEN (KREATIV)");
            btnLoadC.setBackgroundColor(getResources().getColor(R.color.button_load));
            btnLoadC.setOnClickListener(v -> startGame(true, true, false, null));
            root.addView(btnLoadC);
        }

        Button btnMultiplayer = createMenuBtn("MEHRSPIELER (GLOBAL/LAN)");
        btnMultiplayer.setBackgroundColor(getResources().getColor(R.color.button_blue));
        btnMultiplayer.setOnClickListener(v -> showMultiplayer());
        root.addView(btnMultiplayer);

        Button btnSettings = createMenuBtn("EINSTELLUNGEN");
        btnSettings.setBackgroundColor(getResources().getColor(R.color.button_settings));
        btnSettings.setOnClickListener(v -> Toast.makeText(this, "Einstellungen bald verfügbar!", Toast.LENGTH_SHORT).show());
        root.addView(btnSettings);
    }

    private void createMultiplayerMenu() {
        multiplayerPanel = new LinearLayout(this);
        multiplayerPanel.setOrientation(LinearLayout.VERTICAL);
        multiplayerPanel.setBackgroundColor(getResources().getColor(R.color.settings_panel_background));
        multiplayerPanel.setPadding(60, 60, 60, 60);

        TextView title = new TextView(this);
        title.setText("MEHRSPIELER-LOBBY");
        title.setTextColor(getResources().getColor(R.color.white));
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        multiplayerPanel.addView(title);

        final EditText nameInput = new EditText(this);
        nameInput.setHint("Name");
        nameInput.setText(prefs.getString("PLAYER_NAME", "Player" + (int)(Math.random()*999)));
        nameInput.setTextColor(getResources().getColor(R.color.white));
        multiplayerPanel.addView(nameInput);

        final CheckBox publicCheck = new CheckBox(this);
        publicCheck.setText("ÖFFENTLICH GELISTET");
        publicCheck.setTextColor(getResources().getColor(R.color.white));
        publicCheck.setChecked(true);
        multiplayerPanel.addView(publicCheck);

        Button btnHost = createMenuBtn("RAUM HOSTEN");
        btnHost.setBackgroundColor(getResources().getColor(R.color.button_plus));
        btnHost.setOnClickListener(v -> {
            String name = nameInput.getText().toString();
            prefs.edit().putString("PLAYER_NAME", name).apply();
            if (publicCheck.isChecked()) GlobalLobbyClient.registerRoom(name, false, "");
            startGame(true, false, true, "HOST");
        });
        multiplayerPanel.addView(btnHost);

        serverList = new LinearLayout(this);
        serverList.setOrientation(LinearLayout.VERTICAL);
        multiplayerPanel.addView(serverList);

        Button btnRefresh = createMenuBtn("RAUM SUCHEN");
        btnRefresh.setOnClickListener(v -> refreshServers());
        multiplayerPanel.addView(btnRefresh);

        Button closeBtn = createMenuBtn("ZURÜCK");
        closeBtn.setBackgroundColor(getResources().getColor(R.color.button_secondary));
        closeBtn.setOnClickListener(v -> hideMultiplayer());
        multiplayerPanel.addView(closeBtn);

        multiplayerScroll = new ScrollView(this);
        multiplayerScroll.addView(multiplayerPanel);
        multiplayerScroll.setVisibility(View.GONE);
    }

    private void refreshServers() {
        serverList.removeAllViews();
        TextView t = new TextView(this);
        t.setText("Suche läuft...");
        t.setTextColor(getResources().getColor(R.color.white));
        serverList.addView(t);
        dummyManager.findServers();
        GlobalLobbyClient.fetchRooms(rooms -> runOnUiThread(() -> {
            serverList.removeAllViews();

            // Fix: Create a local copy to avoid ConcurrentModificationException
            List<InetAddress> lanServers;
            synchronized(dummyManager.discoveredServers) {
                lanServers = new ArrayList<>(dummyManager.discoveredServers);
            }

            for (InetAddress addr : lanServers) {
                Button s = createMenuBtn("LAN: " + addr.getHostAddress());
                s.setOnClickListener(v -> startGame(false, false, true, addr.getHostAddress()));
                serverList.addView(s);
            }
            for (GlobalLobbyClient.GameRoom room : rooms) {
                Button s = createMenuBtn("GLOBAL: " + room.hostName);
                s.setOnClickListener(v -> startGame(false, false, true, room.ip));
                serverList.addView(s);
            }
            if (serverList.getChildCount() == 0) {
                TextView empty = new TextView(this);
                empty.setText("Keine Server gefunden.");
                empty.setTextColor(getResources().getColor(R.color.white));
                serverList.addView(empty);
            }
        }));
    }

    private void startGame(boolean isCreative, boolean loadWorld, boolean isMp, String mpMode) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("CREATIVE_MODE", isCreative);
        intent.putExtra("LOAD_WORLD", loadWorld);
        intent.putExtra("MULTIPLAYER", isMp);
        intent.putExtra("MP_MODE", mpMode);
        startActivity(intent);
    }

    private Button createMenuBtn(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setBackgroundColor(getResources().getColor(R.color.button_default));
        b.setTextColor(getResources().getColor(R.color.white));
        b.setTextSize(20);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 120);
        p.setMargins(0, 15, 0, 15);
        b.setLayoutParams(p);
        return b;
    }

    private void showMultiplayer() {
        multiplayerScroll.setVisibility(View.VISIBLE);
        root.setVisibility(View.GONE);
        refreshServers();
    }
    private void hideMultiplayer() {
        multiplayerScroll.setVisibility(View.GONE);
        root.setVisibility(View.VISIBLE);
    }
}
