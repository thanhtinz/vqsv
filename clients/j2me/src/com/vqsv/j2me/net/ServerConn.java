package com.vqsv.j2me.net;

import javax.microedition.io.*;
import java.io.*;
import com.vqsv.j2me.GameData;

public class ServerConn implements Runnable {

    public interface ConnListener {
        void onAuthOk();
        void onMoveOk(int x, int y);
        void onWildEnc(int x, int y, String battleId, String name, int lvl, int hp, boolean catchable, int spriteId);
        void onBattleTurn(int pHp, int eHp, String status, String log);
        void onChat(String name, String text);
        void onPlayerNear(long playerId, boolean present, int mapId, int x, int y, String name);
        void onPvpInvite(long challengerId, String name);
        void onPvpStart(String battleId, String oppName, int myHp, int oppHp, int oppSpriteId);
        void onPong();
        void onError(String msg);
    }

    // ---- Client -> Server opcodes ----
    private static final int OPCODE_LOGIN         = 0x01;
    private static final int OPCODE_MOVE          = 0x02;
    private static final int OPCODE_BATTLE_ACT    = 0x03;
    private static final int OPCODE_CHAT          = 0x04;
    private static final int OPCODE_PING          = 0x05;
    private static final int OPCODE_PVP_CHALLENGE = 0x08;
    private static final int OPCODE_PVP_RESPOND   = 0x09;
    private static final int OPCODE_START_TRAINER = 0x0A;

    // ---- Server -> Client opcodes ----
    private static final int OPCODE_AUTH_OK      = 0x81;
    private static final int OPCODE_MOVE_OK      = 0x83;
    private static final int OPCODE_WILD_ENC     = 0x84;
    private static final int OPCODE_BATTLE_TURN  = 0x85;
    private static final int OPCODE_PLAYER_NEAR  = 0x86;
    private static final int OPCODE_CHAT_MSG     = 0x87;
    private static final int OPCODE_PONG         = 0x88;
    private static final int OPCODE_PVP_INVITE   = 0x89;
    private static final int OPCODE_PVP_START    = 0x8A;
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
                int read = 0;
                while (read < len) {
                    int b = in.read();
                    if (b < 0) { connected = false; break; }
                    data[read++] = (byte) b;
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

    // ---- big-endian read helpers operating on a byte[] with a position cursor ----
    private static int u8(byte[] d, int p) {
        return d[p] & 0xFF;
    }

    private static int u16(byte[] d, int p) {
        return ((d[p] & 0xFF) << 8) | (d[p + 1] & 0xFF);
    }

    private static int s32(byte[] d, int p) {
        return ((d[p] & 0xFF) << 24) | ((d[p + 1] & 0xFF) << 16)
             | ((d[p + 2] & 0xFF) << 8) | (d[p + 3] & 0xFF);
    }

    private void parsePacket(byte[] data) {
        if (data == null || data.length == 0) return;
        int opcode = data[0] & 0xFF;
        GameData gd = GameData.getInstance();

        try {
            if (opcode == OPCODE_AUTH_OK) {
                // [2B tokenLen][token][2B level][4B kimTien][2B mapId][1B posX][1B posY]
                int pos = 1;
                int tokenLen = u16(data, pos); pos += 2;
                String token = new String(data, pos, tokenLen, "UTF-8"); pos += tokenLen;
                int level = u16(data, pos); pos += 2;
                int kimTien = s32(data, pos); pos += 4;
                int mapId = u16(data, pos); pos += 2;
                int posX = u8(data, pos); pos++;
                int posY = u8(data, pos); pos++;
                gd.token = token;
                gd.level = level;
                gd.kimTien = kimTien;
                gd.mapId = mapId;
                gd.posX = posX;
                gd.posY = posY;
                if (listener != null) listener.onAuthOk();

            } else if (opcode == OPCODE_MOVE_OK) {
                // [1B x][1B y]
                int newX = u8(data, 1);
                int newY = u8(data, 2);
                gd.posX = newX;
                gd.posY = newY;
                if (listener != null) listener.onMoveOk(newX, newY);

            } else if (opcode == OPCODE_WILD_ENC) {
                // [1B x][1B y][2B bidLen][battleId][2B nameLen][name][1B level][2B hp][1B catchable][2B spriteId]
                int pos = 1;
                int x = u8(data, pos); pos++;
                int y = u8(data, pos); pos++;
                int battleIdLen = u16(data, pos); pos += 2;
                String battleId = new String(data, pos, battleIdLen, "UTF-8"); pos += battleIdLen;
                int nameLen = u16(data, pos); pos += 2;
                String name = new String(data, pos, nameLen, "UTF-8"); pos += nameLen;
                int lvl = u8(data, pos); pos++;
                int hp = u16(data, pos); pos += 2;
                boolean catchable = u8(data, pos) != 0; pos++;
                int spriteId = u16(data, pos); pos += 2;
                gd.activeBattleId = battleId;
                gd.enemyName = name;
                gd.enemyLevel = lvl;
                gd.battleEnemyHp = hp;
                gd.catchable = catchable;
                gd.enemySpriteId = spriteId;
                if (listener != null) listener.onWildEnc(x, y, battleId, name, lvl, hp, catchable, spriteId);

            } else if (opcode == OPCODE_BATTLE_TURN) {
                // [2B playerHp][2B enemyHp][1B statusLen][status][2B logLen][log]
                int pos = 1;
                int pHp = u16(data, pos); pos += 2;
                int eHp = u16(data, pos); pos += 2;
                int statusLen = u8(data, pos); pos++;
                String status = new String(data, pos, statusLen, "UTF-8"); pos += statusLen;
                int logLen = u16(data, pos); pos += 2;
                String log = new String(data, pos, logLen, "UTF-8"); pos += logLen;
                gd.battlePlayerHp = pHp;
                gd.battleEnemyHp = eHp;
                gd.battleStatus = status;
                gd.battleLog = log;
                if (listener != null) listener.onBattleTurn(pHp, eHp, status, log);

            } else if (opcode == OPCODE_PLAYER_NEAR) {
                // [4B playerId][1B present][2B mapId][1B x][1B y][2B nameLen][name]
                int pos = 1;
                long playerId = ((long) s32(data, pos)) & 0xFFFFFFFFL; pos += 4;
                boolean present = u8(data, pos) != 0; pos++;
                int mapId = u16(data, pos); pos += 2;
                int x = u8(data, pos); pos++;
                int y = u8(data, pos); pos++;
                int nameLen = u16(data, pos); pos += 2;
                String name = new String(data, pos, nameLen, "UTF-8"); pos += nameLen;
                if (listener != null) listener.onPlayerNear(playerId, present, mapId, x, y, name);

            } else if (opcode == OPCODE_CHAT_MSG) {
                // [2B nameLen][name][2B textLen][text]
                int pos = 1;
                int nameLen = u16(data, pos); pos += 2;
                String name = new String(data, pos, nameLen, "UTF-8"); pos += nameLen;
                int textLen = u16(data, pos); pos += 2;
                String text = new String(data, pos, textLen, "UTF-8"); pos += textLen;
                if (listener != null) listener.onChat(name, text);

            } else if (opcode == OPCODE_PONG) {
                // no payload
                if (listener != null) listener.onPong();

            } else if (opcode == OPCODE_PVP_INVITE) {
                // [4B challengerId][2B nameLen][name]
                int pos = 1;
                long challengerId = ((long) s32(data, pos)) & 0xFFFFFFFFL; pos += 4;
                int nameLen = u16(data, pos); pos += 2;
                String name = new String(data, pos, nameLen, "UTF-8"); pos += nameLen;
                if (listener != null) listener.onPvpInvite(challengerId, name);

            } else if (opcode == OPCODE_PVP_START) {
                // [2B bidLen][battleId][2B oppNameLen][oppName][2B myHp][2B oppHp][2B oppSpriteId]
                int pos = 1;
                int bidLen = u16(data, pos); pos += 2;
                String battleId = new String(data, pos, bidLen, "UTF-8"); pos += bidLen;
                int oppNameLen = u16(data, pos); pos += 2;
                String oppName = new String(data, pos, oppNameLen, "UTF-8"); pos += oppNameLen;
                int myHp = u16(data, pos); pos += 2;
                int oppHp = u16(data, pos); pos += 2;
                int oppSpriteId = u16(data, pos); pos += 2;
                gd.activeBattleId = battleId;
                gd.enemyName = oppName;
                gd.battlePlayerHp = myHp;
                gd.battleEnemyHp = oppHp;
                gd.enemySpriteId = oppSpriteId;
                gd.catchable = false;
                if (listener != null) listener.onPvpStart(battleId, oppName, myHp, oppHp, oppSpriteId);

            } else if (opcode == OPCODE_ERROR) {
                // [2B msgLen][msg]
                int pos = 1;
                int msgLen = u16(data, pos); pos += 2;
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

    public void sendPvpChallenge(long targetPlayerId) throws IOException {
        int t = (int) targetPlayerId;
        byte[] payload = new byte[] {
            (byte) OPCODE_PVP_CHALLENGE,
            (byte) ((t >> 24) & 0xFF),
            (byte) ((t >> 16) & 0xFF),
            (byte) ((t >> 8) & 0xFF),
            (byte) (t & 0xFF)
        };
        sendRaw(payload);
    }

    public void sendPvpRespond(long challengerId, boolean accept) throws IOException {
        int c = (int) challengerId;
        byte[] payload = new byte[] {
            (byte) OPCODE_PVP_RESPOND,
            (byte) ((c >> 24) & 0xFF),
            (byte) ((c >> 16) & 0xFF),
            (byte) ((c >> 8) & 0xFF),
            (byte) (c & 0xFF),
            (byte) (accept ? 1 : 0)
        };
        sendRaw(payload);
    }

    public void sendStartTrainer(int npcId) throws IOException {
        byte[] payload = new byte[] {
            (byte) OPCODE_START_TRAINER,
            (byte) ((npcId >> 8) & 0xFF),
            (byte) (npcId & 0xFF)
        };
        sendRaw(payload);
    }
}
