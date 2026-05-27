package com.EZdev.mc2;

import android.util.Log;
import java.io.*;
import java.net.*;

/**
 * Verwaltet Netzwerk-Techniken zur Überwindung von Routern (NAT).
 * Nutzt STUN-ähnliche Anfragen zur IP-Ermittlung und bereitet UPnP vor.
 */
public class NatTraversal {
    private static final String TAG = "NatTraversal";

    /**
     * Versucht, Ports am Router via UPnP zu öffnen.
     * @param port Der zu öffnende Port (9999 für TCP, 9998 für UDP).
     */
    public static void tryOpenPort(int port) {
        new Thread(() -> {
            try {
                // Placeholder für UPnP-Bibliotheks-Logik (z.B. Cling)
                Log.i(TAG, "Starte UPnP Port-Mapping für Port: " + port);
            } catch (Exception e) { Log.e(TAG, "UPnP Fehler", e); }
        }).start();
    }

    /**
     * Ermittelt die öffentliche IP des Geräts über einen externen Dienst.
     * @return Die IP-Adresse als String oder "Unknown".
     */
    public static String getPublicIp() {
        try {
            URL url = new URL("https://api.ipify.org");
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String ip = in.readLine();
            in.close();
            return ip;
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
