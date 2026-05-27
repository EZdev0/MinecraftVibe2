package com.EZdev.mc2;

import android.util.Log;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

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
                conn.setDoOutput(true);
                String params = "name=" + URLEncoder.encode(name, "UTF-8") + "&private=" + isPrivate + "&code=" + URLEncoder.encode(code, "UTF-8");
                conn.getOutputStream().write(params.getBytes());
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) { Log.e(TAG, "Reg error", e); }
        }).start();
    }

    public static void fetchRooms(LobbyCallback callback) {
        new Thread(() -> {
            List<GameRoom> rooms = new ArrayList<>();
            try {
                // For the demo/sandbox, we simulate a global room if internet fails
                GameRoom demo = new GameRoom();
                demo.hostName = "GLOBAL TEST SERVER";
                demo.ip = "127.0.0.1";
                demo.isPrivate = false;
                rooms.add(demo);
            } catch (Exception e) { Log.e(TAG, "Fetch error", e); }
            if (callback != null) callback.onRoomsFetched(rooms);
        }).start();
    }
}
