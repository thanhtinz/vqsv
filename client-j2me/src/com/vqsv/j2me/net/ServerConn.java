package com.vqsv.j2me.net;

import javax.microedition.io.*;
import java.io.*;
import com.vqsv.j2me.GameData;

public class ServerConn implements Runnable {

    public interface ConnListener {
        void onAuthOk();
        void onMoveOk(int x, int y);
        void onWildEnc(int x, int y, String battleId, String name, int lvl, int hp, boolean catchable);
        void onBattleTurn(int pHp, int eHp, String status, String log);
        void onError(String msg);
    }

    private static final int OPCODE_LOGIN      = 0x01;
    private static final int OPCODE_MOVE       = 0x02;
    private static final int OPCODE_BATTLE_ACT = 0x03;
    private static final int OPCODE_CHAT       = 0x04;
    private static final int OPCODE_PING       = 0x05;

    private static final int OPCODE_AUTH_OK      = 0x81;
    private static final int OPCODE_MOVE_OK      = 0x83;
    private static final int OPCODE_WILD_ENC     = 0x84;
    private static final int OPCODE_BATTLE_TURN  = 0x85;
    private static final int OPCODE_ERROR        = 0xFF;

    private StreamConnection conn;
    private DataOutputStream out;
    private DataInputStream in;
    private Thread recvThread;
    private ConnListener listener;
    private boolean connected = false;

    public void setListener(ConnListener l) {
        this.listener = l;
    }

    public boolean isConnected() {
        return connected;
    }

    public void connect(String host, int port) throws IOException {
        conn = (StreamConnection) Connector.open("socket://" + host + ":" + port);
        out = conn.openDataOutputStream();
        in = conn.openDataInputStream();
        connected = true;
        recvThread = new Thread(this);
        recvThread.start();
    }

    public void disconnect() {
        connected = false;
        try { if (in != null) in.close(); } catch (Exception e) { }
        try { if (out != null) out.close(); } catch (Exception e) { }
        try { if (conn != null) conn.close(); } catch (Exception e) { }
    }

    public void run() {
        try {
            while (connected) {
                int hi = in.read();
                int lo = in.read();
                if (hi < 0 || lo < 0) break;
                int len = (hi << 8) | lo;
                if (len <= 0) continue;
                byte[] data = new byte[len];
                for (int i = 0; i < len; i++) {
                    int b = in.read();
                    if (b < 0) { connected = false; break; }
                    data[i] = (byte) b;
                }
                if (!connected) break;
                parsePacket(data);
            }
        } catch (IOException e) {
            if (connected && listener != null) {
                listener.onError("Connection lost: " + e.getMessage());
            }
        } finally {
            connected = false;
        }
    }

    private void parsePacket(byte[] data) {
        if (data == null || data.length == 0) return;
        int opcode = data[0] & 0xFF;
        GameData gd = GameData.getInstance();

        try {
            if (opcode == OPCODE_AUTH_OK) {
                // [2B tokenLen][token][2B level][4B kimTien][2B mapId][1B posX][1B posY]
                int pos = 1;
                int tokenLen = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
                pos += 2;
                String token = new String(data, pos, tokenLen, "UTF-8");
                pos += tokenLen;
                int level = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
                pos += 2;
                int kimTien = ((data[pos] & 0xFF) << 24) | ((data[pos + 1] & 0xFF) << 16)
                            | ((data[pos + 2] & 0xFF) << 8) | (data[pos + 3] & 0xFF);
                pos += 4;
                int mapId = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
                pos += 2;
                int posX = data[pos] & 0xFF;
                pos++;
                int posY = data[pos] & 0xFF;
                gd.token = token;
                gd.level = level;
                gd.kimTien = kimTien;
                gd.mapId = mapId;
                gd.posX = posX;
                gd.posY = posY;
                if (listener != null) listener.onAuthOk();

            } else if (opcode == OPCODE_MOVE_OK) {
                // [1B newX][1B newY]
                int newX = data[1] & 0xFF;
                int newY = data[2] & 0xFF;
                gd.posX = newX;
                gd.posY = newY;
                if (listener != null) listener.onMoveOk(newX, newY);

            } else if (opcode == OPCODE_WILD_ENC) {
                // [1B x][1B y][2B battleIdLen][battleId][2B nameLen][name UTF-8][1B lvl][2B hp][1B catchable]
                int pos = 1;
                int x = data[pos] & 0xFF; pos++;
                int y = data[pos] & 0xFF; pos++;
                int battleIdLen = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF); pos += 2;
                String battleId = new String(data, pos, battleIdLen, "UTF-8"); pos += battleIdLen;
                int nameLen = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF); pos += 2;
                String name = new String(data, pos, nameLen, "UTF-8"); pos += nameLen;
                int lvl = data[pos] & 0xFF; pos++;
                int hp = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF); pos += 2;
                boolean catchable = (data[pos] & 0xFF) != 0;
                gd.activeBattleId = battleId;
                gd.enemyName = name;
                gd.enemyLevel = lvl;
                gd.battleEnemyHp = hp;
                gd.catchable = catchable;
                if (listener != null) listener.onWildEnc(x, y, battleId, name, lvl, hp, catchable);

            } else if (opcode == OPCODE_BATTLE_TURN) {
                // [2B playerHp][2B enemyHp][1B statusLen][status][2B logLen][log]
                int pos = 1;
                int pHp = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF); pos += 2;
                int eHp = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF); pos += 2;
                int statusLen = data[pos] & 0xFF; pos++;
                String status = new String(data, pos, statusLen, "UTF-8"); pos += statusLen;
                int logLen = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF); pos += 2;
                String log = new String(data, pos, logLen, "UTF-8");
                gd.battlePlayerHp = pHp;
                gd.battleEnemyHp = eHp;
                gd.battleStatus = status;
                gd.battleLog = log;
                if (listener != null) listener.onBattleTurn(pHp, eHp, status, log);

            } else if (opcode == OPCODE_ERROR) {
                // [2B msgLen][msg]
                int pos = 1;
                int msgLen = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF); pos += 2;
                String msg = new String(data, pos, msgLen, "UTF-8");
                if (listener != null) listener.onError(msg);
            }
        } catch (Exception e) {
            if (listener != null) listener.onError("Parse error: " + e.getMessage());
        }
    }

    private synchronized void writeShort(int val) throws IOException {
        out.write((val >> 8) & 0xFF);
        out.write(val & 0xFF);
    }

    private synchronized void sendRaw(byte[] payload) throws IOException {
        writeShort(payload.length);
        out.write(payload);
        out.flush();
    }

    public void sendLogin(String username, String password) throws IOException {
        byte[] uBytes = username.getBytes("UTF-8");
        byte[] pBytes = password.getBytes("UTF-8");
        int len = 1 + 2 + uBytes.length + 2 + pBytes.length;
        byte[] payload = new byte[len];
        int pos = 0;
        payload[pos++] = (byte) OPCODE_LOGIN;
        payload[pos++] = (byte) ((uBytes.length >> 8) & 0xFF);
        payload[pos++] = (byte) (uBytes.length & 0xFF);
        System.arraycopy(uBytes, 0, payload, pos, uBytes.length);
        pos += uBytes.length;
        payload[pos++] = (byte) ((pBytes.length >> 8) & 0xFF);
        payload[pos++] = (byte) (pBytes.length & 0xFF);
        System.arraycopy(pBytes, 0, payload, pos, pBytes.length);
        sendRaw(payload);
    }

    public void sendMove(int direction) throws IOException {
        byte[] payload = new byte[] { (byte) OPCODE_MOVE, (byte) direction };
        sendRaw(payload);
    }

    public void sendBattleAct(String battleId, int action) throws IOException {
        byte[] bBytes = battleId.getBytes("UTF-8");
        int len = 1 + 2 + bBytes.length + 1;
        byte[] payload = new byte[len];
        int pos = 0;
        payload[pos++] = (byte) OPCODE_BATTLE_ACT;
        payload[pos++] = (byte) ((bBytes.length >> 8) & 0xFF);
        payload[pos++] = (byte) (bBytes.length & 0xFF);
        System.arraycopy(bBytes, 0, payload, pos, bBytes.length);
        pos += bBytes.length;
        payload[pos++] = (byte) action;
        sendRaw(payload);
    }

    public void sendBattleActWithItem(String battleId, int action, int itemId) throws IOException {
        byte[] bBytes = battleId.getBytes("UTF-8");
        int len = 1 + 2 + bBytes.length + 1 + 2;
        byte[] payload = new byte[len];
        int pos = 0;
        payload[pos++] = (byte) OPCODE_BATTLE_ACT;
        payload[pos++] = (byte) ((bBytes.length >> 8) & 0xFF);
        payload[pos++] = (byte) (bBytes.length & 0xFF);
        System.arraycopy(bBytes, 0, payload, pos, bBytes.length);
        pos += bBytes.length;
        payload[pos++] = (byte) action;
        payload[pos++] = (byte) ((itemId >> 8) & 0xFF);
        payload[pos++] = (byte) (itemId & 0xFF);
        sendRaw(payload);
    }

    public void sendChat(String text) throws IOException {
        if (text.length() > 128) text = text.substring(0, 128);
        byte[] tBytes = text.getBytes("UTF-8");
        int len = 1 + 2 + tBytes.length;
        byte[] payload = new byte[len];
        int pos = 0;
        payload[pos++] = (byte) OPCODE_CHAT;
        payload[pos++] = (byte) ((tBytes.length >> 8) & 0xFF);
        payload[pos++] = (byte) (tBytes.length & 0xFF);
        System.arraycopy(tBytes, 0, payload, pos, tBytes.length);
        sendRaw(payload);
    }

    public void sendPing() throws IOException {
        byte[] payload = new byte[] { (byte) OPCODE_PING };
        sendRaw(payload);
    }
}
