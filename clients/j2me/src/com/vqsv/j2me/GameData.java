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

    private GameData() {
    }

    public static GameData getInstance() {
        if (instance == null) {
            instance = new GameData();
        }
        return instance;
    }
}
