package com.vqsv.j2me;

public class GameData {

    private static GameData instance;

    public String token = "";
    public String playerName = "";
    public int level = 1;
    public int kimTien = 0;
    public int mapId = 1;
    public int posX = 0;
    public int posY = 0;
    public int hp = 100;
    public int hpMax = 100;
    public String activeBattleId = "";
    public int battlePlayerHp = 0;
    public int battleEnemyHp = 0;
    public String battleStatus = "";
    public String battleLog = "";
    public String enemyName = "";
    public int enemyLevel = 1;
    public boolean catchable = false;
    public int enemySpriteId = 0;

    // ---- Nearby players (presence) ----
    // Parallel arrays, fixed capacity; entry is "in use" when nearIds[i] != 0.
    public static final int MAX_NEAR = 16;
    public long[] nearIds = new long[MAX_NEAR];
    public int[] nearMapId = new int[MAX_NEAR];
    public int[] nearX = new int[MAX_NEAR];
    public int[] nearY = new int[MAX_NEAR];
    public String[] nearNames = new String[MAX_NEAR];

    // ---- Chat log (most recent lines, ring buffer) ----
    public static final int MAX_CHAT = 6;
    public String[] chatLog = new String[MAX_CHAT];
    public int chatCount = 0;

    // ---- Pending PvP invite ----
    public long pendingChallengerId = 0;
    public String pendingChallengerName = "";

    private GameData() {
    }

    // Insert/update/remove a nearby player by id.
    public synchronized void updateNear(long id, boolean present, int mapId, int x, int y, String name) {
        // Find existing slot.
        int slot = -1;
        for (int i = 0; i < MAX_NEAR; i++) {
            if (nearIds[i] == id) { slot = i; break; }
        }
        if (!present) {
            if (slot >= 0) { nearIds[slot] = 0; nearNames[slot] = null; }
            return;
        }
        if (slot < 0) {
            for (int i = 0; i < MAX_NEAR; i++) {
                if (nearIds[i] == 0) { slot = i; break; }
            }
        }
        if (slot < 0) return; // table full; drop silently
        nearIds[slot] = id;
        nearMapId[slot] = mapId;
        nearX[slot] = x;
        nearY[slot] = y;
        nearNames[slot] = name;
    }

    // Append a chat line to the ring buffer.
    public synchronized void addChat(String line) {
        if (chatCount < MAX_CHAT) {
            chatLog[chatCount++] = line;
        } else {
            for (int i = 1; i < MAX_CHAT; i++) chatLog[i - 1] = chatLog[i];
            chatLog[MAX_CHAT - 1] = line;
        }
    }

    public static GameData getInstance() {
        if (instance == null) {
            instance = new GameData();
        }
        return instance;
    }
}
