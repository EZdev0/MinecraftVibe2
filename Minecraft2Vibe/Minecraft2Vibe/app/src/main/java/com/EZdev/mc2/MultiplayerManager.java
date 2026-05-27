package com.EZdev.mc2;

import android.util.Log;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

public class MultiplayerManager {
    private static final String TAG = "MultiplayerManager";
    public static final int DISCOVERY_PORT = 8888;
    public static final int GAME_PORT_TCP = 9999;
    public static final int GAME_PORT_UDP = 9998;

    public static class RemotePlayer {
        public String name = "Guest";
        public float x, y, z, yaw, pitch;
        public float targetX, targetY, targetZ; // For interpolation
        public long lastUpdate;
        public int id, nameTagTexture = -1;
    }

    private DatagramSocket discoverySocket;
    private ServerSocket serverSocket;
    private DatagramSocket gameUdpSocket;
    private boolean isHost = false;
    private volatile boolean running = false;
    public final Map<Integer, RemotePlayer> players = new ConcurrentHashMap<>();
    private final Map<Integer, DataOutputStream> clientTCPOuts = new ConcurrentHashMap<>();
    private final Map<Integer, InetSocketAddress> clientUDPAddrs = new ConcurrentHashMap<>();
    public final List<InetAddress> discoveredServers = Collections.synchronizedList(new ArrayList<>());
    private final ExecutorService networkExecutor = Executors.newCachedThreadPool();
    private String playerName = "Player";
    private Gameplay gameplay;
    private WorldLogic world;
    private Socket clientSocket;
    private DataOutputStream clientOut;
    private InetAddress serverAddr;
    private int myClientId = -1;

    public MultiplayerManager(String name, Gameplay gameplay, WorldLogic world) {
        this.playerName = name; this.gameplay = gameplay; this.world = world;
    }

    public void startDiscoveryServer() {
        if (running && discoverySocket != null) return;
        running = true;
        networkExecutor.execute(() -> {
            try {
                discoverySocket = new DatagramSocket(DISCOVERY_PORT); discoverySocket.setBroadcast(true);
                byte[] buffer = new byte[1024];
                while (running) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    discoverySocket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength());
                    if (msg.startsWith("MCVIBE_QUERY")) {
                        byte[] resp = ("MCVIBE_HOST:" + playerName).getBytes();
                        discoverySocket.send(new DatagramPacket(resp, resp.length, packet.getAddress(), packet.getPort()));
                    }
                }
            } catch (Exception e) {}
        });
    }

    public void findServers() {
        discoveredServers.clear();
        networkExecutor.execute(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true); socket.setSoTimeout(2000);
                byte[] query = "MCVIBE_QUERY".getBytes();
                socket.send(new DatagramPacket(query, query.length, InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT));
                byte[] buf = new byte[1024]; long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < 2000) {
                    try {
                        DatagramPacket resp = new DatagramPacket(buf, buf.length); socket.receive(resp);
                        String msg = new String(resp.getData(), 0, resp.getLength());
                        if (msg.startsWith("MCVIBE_HOST:") && !discoveredServers.contains(resp.getAddress())) discoveredServers.add(resp.getAddress());
                    } catch (SocketTimeoutException e) { break; }
                }
            } catch (Exception e) {}
        });
    }

    public void startHost() {
        this.isHost = true; this.running = true; this.myClientId = 0;
        NatTraversal.tryOpenPort(GAME_PORT_TCP); NatTraversal.tryOpenPort(GAME_PORT_UDP);
        networkExecutor.execute(this::serverLoop); networkExecutor.execute(this::udpReceiverLoop);
        startDiscoveryServer();
    }

    private void serverLoop() {
        try {
            serverSocket = new ServerSocket(GAME_PORT_TCP);
            while (running) handleNewClient(serverSocket.accept());
        } catch (Exception e) {}
    }

    private void handleNewClient(Socket socket) {
        networkExecutor.execute(() -> {
            int id = socket.getPort();
            try (DataInputStream in = new DataInputStream(socket.getInputStream());
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                out.writeInt(id); out.writeUTF(playerName);
                String remoteName = in.readUTF();
                RemotePlayer rp = new RemotePlayer(); rp.name = remoteName; rp.id = id;
                players.put(id, rp); clientTCPOuts.put(id, out);
                clientUDPAddrs.put(id, new InetSocketAddress(socket.getInetAddress(), GAME_PORT_UDP));
                while (running) {
                    byte type = in.readByte();
                    if (type == 1) {
                        int bx=in.readInt(), by=in.readInt(), bz=in.readInt(); byte b=in.readByte();
                        if (world != null) world.setBlock(bx, by, bz, b, false);
                        broadcastBlockChange(bx, by, bz, b, id);
                    } else if (type == 2) { // Chat
                        String msg = in.readUTF();
                        broadcastChat(remoteName + ": " + msg, id);
                    }
                }
            } catch (Exception e) { players.remove(id); clientTCPOuts.remove(id); clientUDPAddrs.remove(id); }
        });
    }

    public void connect(InetAddress addr) {
        this.serverAddr = addr; this.isHost = false; this.running = true;
        networkExecutor.execute(() -> {
            try {
                clientSocket = new Socket(serverAddr, GAME_PORT_TCP);
                DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                clientOut = new DataOutputStream(clientSocket.getOutputStream());
                myClientId = in.readInt(); String hostName = in.readUTF(); clientOut.writeUTF(playerName);
                RemotePlayer host = new RemotePlayer(); host.name = hostName; host.id = 0;
                players.put(0, host); networkExecutor.execute(this::udpReceiverLoop);
                while (running) {
                    byte type = in.readByte();
                    if (type == 1) {
                        int bx=in.readInt(), by=in.readInt(), bz=in.readInt(); byte b=in.readByte();
                        if (world != null) world.setBlock(bx, by, bz, b, false);
                    } else if (type == 2) {
                        String msg = in.readUTF(); Log.i(TAG, "CHAT: " + msg);
                    }
                }
            } catch (Exception e) { running = false; }
        });
    }

    private void udpReceiverLoop() {
        try {
            gameUdpSocket = new DatagramSocket(isHost ? GAME_PORT_UDP : 0);
            byte[] buf = new byte[256];
            while (running) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length); gameUdpSocket.receive(packet);
                ByteBuffer bb = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
                int id = bb.getInt(); if (id == myClientId) continue;
                float x=bb.getFloat(), y=bb.getFloat(), z=bb.getFloat(), yaw=bb.getFloat(), pitch=bb.getFloat();
                if (isHost && !clientUDPAddrs.containsKey(id)) clientUDPAddrs.put(id, new InetSocketAddress(packet.getAddress(), packet.getPort()));
                RemotePlayer rp = players.get(id);
                if (rp == null) { rp = new RemotePlayer(); rp.id = id; players.put(id, rp); }
                rp.targetX = x; rp.targetY = y; rp.targetZ = z; rp.yaw = yaw; rp.pitch = pitch;
                rp.lastUpdate = System.currentTimeMillis();
                if (isHost) relayUdpPacket(packet.getData(), packet.getLength(), id);
            }
        } catch (Exception e) {}
    }

    public void updateInterpolation(float dt) {
        for (RemotePlayer rp : players.values()) {
            float lerp = 5.0f * dt;
            rp.x += (rp.targetX - rp.x) * lerp;
            rp.y += (rp.targetY - rp.y) * lerp;
            rp.z += (rp.targetZ - rp.z) * lerp;
        }
    }

    public void sendPositionUpdate() {
        if (!running || myClientId == -1 || gameplay == null) return;
        networkExecutor.execute(() -> {
            try {
                ByteBuffer bb = ByteBuffer.allocate(24);
                bb.putInt(myClientId); bb.putFloat(gameplay.camX); bb.putFloat(gameplay.camY); bb.putFloat(gameplay.camZ); bb.putFloat(gameplay.yaw); bb.putFloat(gameplay.pitch);
                byte[] data = bb.array();
                if (isHost) { for (InetSocketAddress addr : clientUDPAddrs.values()) gameUdpSocket.send(new DatagramPacket(data, data.length, addr)); }
                else if (serverAddr != null) gameUdpSocket.send(new DatagramPacket(data, data.length, serverAddr, GAME_PORT_UDP));
            } catch (Exception e) {}
        });
    }

    public void sendBlockChange(int x, int y, int z, byte block) {
        if (!running) return;
        networkExecutor.execute(() -> {
            try {
                if (isHost) broadcastBlockChange(x, y, z, block, 0);
                else if (clientOut != null) {
                    synchronized (clientOut) { clientOut.writeByte(1); clientOut.writeInt(x); clientOut.writeInt(y); clientOut.writeInt(z); clientOut.writeByte(block); clientOut.flush(); }
                }
            } catch (Exception e) {}
        });
    }

    public void sendChat(String msg) {
        if (!running) return;
        networkExecutor.execute(() -> {
            try {
                if (isHost) broadcastChat(playerName + ": " + msg, 0);
                else if (clientOut != null) {
                    synchronized (clientOut) { clientOut.writeByte(2); clientOut.writeUTF(msg); clientOut.flush(); }
                }
            } catch (Exception e) {}
        });
    }

    private void broadcastBlockChange(int x, int y, int z, byte b, int exc) {
        for (Map.Entry<Integer, DataOutputStream> e : clientTCPOuts.entrySet()) {
            if (e.getKey() == exc) continue;
            try { DataOutputStream o = e.getValue(); synchronized (o) { o.writeByte(1); o.writeInt(x); o.writeInt(y); o.writeInt(z); o.writeByte(b); o.flush(); } }
            catch (Exception ex) { clientTCPOuts.remove(e.getKey()); }
        }
    }

    private void broadcastChat(String m, int exc) {
        for (Map.Entry<Integer, DataOutputStream> e : clientTCPOuts.entrySet()) {
            if (e.getKey() == exc) continue;
            try { DataOutputStream o = e.getValue(); synchronized (o) { o.writeByte(2); o.writeUTF(m); o.flush(); } }
            catch (Exception ex) { clientTCPOuts.remove(e.getKey()); }
        }
    }

    private void relayUdpPacket(byte[] d, int l, int s) {
        for (Map.Entry<Integer, InetSocketAddress> e : clientUDPAddrs.entrySet()) {
            if (e.getKey() == s) continue;
            try { gameUdpSocket.send(new DatagramPacket(d, l, e.getValue())); } catch (Exception ex) {}
        }
    }

    public void stop() {
        running = false;
        try {
            if (discoverySocket != null) discoverySocket.close(); if (serverSocket != null) serverSocket.close();
            if (gameUdpSocket != null) gameUdpSocket.close(); if (clientSocket != null) clientSocket.close();
        } catch (Exception e) {}
        players.clear(); clientTCPOuts.clear(); clientUDPAddrs.clear(); networkExecutor.shutdownNow();
    }
}
