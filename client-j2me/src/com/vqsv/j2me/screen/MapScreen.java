package com.vqsv.j2me.screen;

import javax.microedition.lcdui.*;
import com.vqsv.j2me.GameMIDLet;
import com.vqsv.j2me.GameData;
import com.vqsv.j2me.net.ServerConn;

public class MapScreen extends Canvas implements CommandListener, ServerConn.ConnListener {

    private static final int TILE_W = 34;
    private static final int TILE_H = 36;
    private static final int TILES_X = 10;
    private static final int TILES_Y = 14;

    private static final int COLOR_BG      = 0x1A4A1A; // dark green
    private static final int COLOR_TILE     = 0x2D6B2D;
    private static final int COLOR_GRID     = 0x1A3A1A;
    private static final int COLOR_PLAYER   = 0x4444FF; // blue
    private static final int COLOR_HP_BG    = 0x550000;
    private static final int COLOR_HP_FG    = 0x00CC00;
    private static final int COLOR_TEXT     = 0xFFD700; // gold
    private static final int COLOR_WHITE    = 0xFFFFFF;

    private Command chatCmd;

    public MapScreen() {
        setTitle("VQSV - Ban do");
        setFullScreenMode(true);
        GameMIDLet.getConn().setListener(this);
        chatCmd = new Command("Chat", Command.SCREEN, 1);
        addCommand(chatCmd);
        setCommandListener(this);
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        GameData gd = GameData.getInstance();

        // Background
        g.setColor(COLOR_BG);
        g.fillRect(0, 0, w, h);

        // Draw tile grid
        for (int tx = 0; tx < TILES_X; tx++) {
            for (int ty = 0; ty < TILES_Y; ty++) {
                int px = tx * TILE_W;
                int py = ty * TILE_H;
                g.setColor(COLOR_TILE);
                g.fillRect(px + 1, py + 1, TILE_W - 2, TILE_H - 2);
                g.setColor(COLOR_GRID);
                g.drawRect(px, py, TILE_W, TILE_H);
            }
        }

        // Draw player
        int px = (gd.posX % TILES_X) * TILE_W;
        int py = (gd.posY % TILES_Y) * TILE_H;
        g.setColor(COLOR_PLAYER);
        g.fillRect(px + 4, py + 4, TILE_W - 8, TILE_H - 8);
        g.setColor(COLOR_WHITE);
        g.drawRect(px + 4, py + 4, TILE_W - 8, TILE_H - 8);

        // HP bar at bottom (y = h - 60 to h - 40)
        int barY = h - 60;
        int barW = w - 20;
        g.setColor(COLOR_HP_BG);
        g.fillRect(10, barY, barW, 12);
        int hpW = (gd.hpMax > 0) ? (barW * gd.hp / gd.hpMax) : 0;
        g.setColor(COLOR_HP_FG);
        g.fillRect(10, barY, hpW, 12);
        g.setColor(COLOR_WHITE);
        g.drawRect(10, barY, barW, 12);

        // Gold text: position and stats
        g.setColor(COLOR_TEXT);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));
        g.drawString("HP: " + gd.hp + "/" + gd.hpMax, 10, barY + 16, Graphics.LEFT | Graphics.TOP);
        g.drawString("Kim: " + gd.kimTien, w / 2, barY + 16, Graphics.HCENTER | Graphics.TOP);
        g.drawString("Lv." + gd.level + " (" + gd.posX + "," + gd.posY + ")",
                     w - 10, barY + 16, Graphics.RIGHT | Graphics.TOP);
        g.drawString("Map: " + gd.mapId, 10, barY + 30, Graphics.LEFT | Graphics.TOP);
    }

    protected void keyPressed(int keyCode) {
        ServerConn conn = GameMIDLet.getConn();
        try {
            int gameAction = getGameAction(keyCode);
            if (gameAction == UP || keyCode == KEY_NUM2) {
                conn.sendMove(0);
            } else if (gameAction == DOWN || keyCode == KEY_NUM8) {
                conn.sendMove(1);
            } else if (gameAction == LEFT || keyCode == KEY_NUM4) {
                conn.sendMove(2);
            } else if (gameAction == RIGHT || keyCode == KEY_NUM6) {
                conn.sendMove(3);
            }
        } catch (Exception e) {
            showAlert("Loi", "Loi gui lenh: " + e.getMessage());
        }
        repaint();
    }

    public void commandAction(Command c, Displayable d) {
        // Chat command placeholder
        showAlert("Chat", "Tinh nang chat se som ra mat!");
    }

    public void onAuthOk() {
        // not expected here
    }

    public void onMoveOk(int x, int y) {
        GameData gd = GameData.getInstance();
        gd.posX = x;
        gd.posY = y;
        repaint();
    }

    public void onWildEnc(int x, int y, String battleId, String name, int lvl, int hp, boolean catchable) {
        GameData gd = GameData.getInstance();
        gd.activeBattleId = battleId;
        gd.enemyName = name;
        gd.enemyLevel = lvl;
        gd.battleEnemyHp = hp;
        gd.catchable = catchable;
        GameMIDLet.getInstance().showScreen(new BattleScreen());
    }

    public void onBattleTurn(int pHp, int eHp, String status, String log) {
        // not expected on map screen
    }

    public void onError(final String msg) {
        showAlert("Loi", msg);
    }

    private void showAlert(String title, String text) {
        Alert alert = new Alert(title, text, null, AlertType.ERROR);
        alert.setTimeout(3000);
        GameMIDLet.getInstance().showScreen(alert);
    }
}
