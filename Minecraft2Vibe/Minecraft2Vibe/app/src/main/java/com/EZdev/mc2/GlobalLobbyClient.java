package com.EZdev.mc2;

import android.util.Log;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class GlobalLobbyClient {
    private static final String TAG = "GlobalLobby";
    private static final String LOBBY_URL = "https://mcvibe2-lobby.glitch.me";

    public static class GameRoom {
        public String hostName;
        public String ip;
        public boolean isPrivate;
        public String roomCode;
    }

    public interface LobbyCallback {
        void onRoomsFetched(List<GameRoom> rooms);
    }

    public static void registerRoom(String name, boolean isPrivate, String code) {
        new Thread(() -> {
            try {
                URL url = new URL(LOBBY_URL + "/register");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(5000);
                conn.setDoOutput(true);
                String params = "name=" + URLEncoder.encode(name, "UTF-8") +
                                "&private=" + isPrivate +
                                "&code=" + URLEncoder.encode(code, "UTF-8");
                conn.getOutputStream().write(params.getBytes());
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) { Log.e(TAG, "Reg error: " + e.getMessage()); }
        }).start();
    }

    public static void fetchRooms(LobbyCallback callback) {
        new Thread(() -> {
            List<GameRoom> rooms = new ArrayList<>();
            try {
                URL url = new URL(LOBBY_URL + "/rooms");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) response.append(line);
                    in.close();

                    JSONArray arr = new JSONArray(response.toString());
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        GameRoom room = new GameRoom();
                        room.hostName = obj.optString("name", "Unknown");
                        room.ip = obj.optString("ip", "0.0.0.0");
                        room.isPrivate = obj.optBoolean("private", false);
                        rooms.add(room);
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Fetch error: " + e.getMessage());
                // Fallback demo room
                GameRoom demo = new GameRoom();
                demo.hostName = "GLOBAL TEST SERVER (FALLBACK)";
                demo.ip = "127.0.0.1";
                demo.isPrivate = false;
                rooms.add(demo);
            }
            if (callback != null) callback.onRoomsFetched(rooms);
        }).start();
    }
}
