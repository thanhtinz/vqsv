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
    private static final int COLOR_OTHER    = 0xCC4444; // red (other players)
    private static final int COLOR_HP_BG    = 0x550000;
    private static final int COLOR_HP_FG    = 0x00CC00;
    private static final int COLOR_TEXT     = 0xFFD700; // gold
    private static final int COLOR_WHITE    = 0xFFFFFF;

    private Command chatCmd;
    private Command chatSendCmd;
    private Command chatBackCmd;
    private Command pvpAcceptCmd;
    private Command pvpDeclineCmd;
    private TextBox chatBox;

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

        // Draw nearby players (same map only), in red, behind our own marker.
        Font tiny = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        g.setFont(tiny);
        for (int i = 0; i < GameData.MAX_NEAR; i++) {
            if (gd.nearIds[i] == 0) continue;
            if (gd.nearMapId[i] != gd.mapId) continue;
            int ox = (gd.nearX[i] % TILES_X) * TILE_W;
            int oy = (gd.nearY[i] % TILES_Y) * TILE_H;
            g.setColor(COLOR_OTHER);
            g.fillRect(ox + 6, oy + 6, TILE_W - 12, TILE_H - 12);
            g.setColor(COLOR_WHITE);
            g.drawRect(ox + 6, oy + 6, TILE_W - 12, TILE_H - 12);
            if (gd.nearNames[i] != null) {
                g.drawString(gd.nearNames[i], ox + TILE_W / 2, oy - 2,
                             Graphics.HCENTER | Graphics.BOTTOM);
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

        // Chat log: last few lines at top of screen.
        Font chatFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        g.setFont(chatFont);
        int lineH = chatFont.getHeight() + 1;
        for (int i = 0; i < gd.chatCount; i++) {
            String line = gd.chatLog[i];
            if (line == null) continue;
            g.setColor(COLOR_WHITE);
            g.drawString(line, 4, 2 + i * lineH, Graphics.LEFT | Graphics.TOP);
        }
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
        if (c == chatCmd) {
            // Open a text box to compose a chat message.
            chatBox = new TextBox("Chat", "", 128, TextField.ANY);
            if (chatSendCmd == null) {
                chatSendCmd = new Command("Gui", Command.OK, 1);
                chatBackCmd = new Command("Huy", Command.BACK, 2);
            }
            chatBox.addCommand(chatSendCmd);
            chatBox.addCommand(chatBackCmd);
            chatBox.setCommandListener(this);
            GameMIDLet.getInstance().showScreen(chatBox);
        } else if (c == chatSendCmd) {
            final String text = (chatBox != null) ? chatBox.getString().trim() : "";
            GameMIDLet.getInstance().showScreen(this);
            if (text.length() > 0) {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            GameMIDLet.getConn().sendChat(text);
                        } catch (Exception e) {
                            onError("Loi gui chat: " + e.getMessage());
                        }
                    }
                }).start();
            }
            repaint();
        } else if (c == chatBackCmd) {
            GameMIDLet.getInstance().showScreen(this);
            repaint();
        } else if (c == pvpAcceptCmd || c == pvpDeclineCmd) {
            final boolean accept = (c == pvpAcceptCmd);
            final long challengerId = GameData.getInstance().pendingChallengerId;
            GameMIDLet.getInstance().showScreen(this);
            new Thread(new Runnable() {
                public void run() {
                    try {
                        GameMIDLet.getConn().sendPvpRespond(challengerId, accept);
                    } catch (Exception e) {
                        onError("Loi PvP: " + e.getMessage());
                    }
                }
            }).start();
            repaint();
        }
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

    public void onWildEnc(int x, int y, String battleId, String name, int lvl, int hp, boolean catchable, int spriteId) {
        GameData gd = GameData.getInstance();
        gd.activeBattleId = battleId;
        gd.enemyName = name;
        gd.enemyLevel = lvl;
        gd.battleEnemyHp = hp;
        gd.catchable = catchable;
        gd.enemySpriteId = spriteId;
        GameMIDLet.getInstance().showScreen(new BattleScreen());
    }

    public void onBattleTurn(int pHp, int eHp, String status, String log) {
        // not expected on map screen
    }

    public void onChat(String name, String text) {
        GameData.getInstance().addChat(name + ": " + text);
        repaint();
    }

    public void onPlayerNear(long playerId, boolean present, int mapId, int x, int y, String name) {
        GameData.getInstance().updateNear(playerId, present, mapId, x, y, name);
        repaint();
    }

    public void onPvpInvite(long challengerId, String name) {
        GameData gd = GameData.getInstance();
        gd.pendingChallengerId = challengerId;
        gd.pendingChallengerName = name;
        // Prompt accept/decline.
        Alert alert = new Alert("Thach dau PvP",
            name + " muon thach dau ban!", null, AlertType.CONFIRMATION);
        alert.setTimeout(Alert.FOREVER);
        if (pvpAcceptCmd == null) {
            pvpAcceptCmd = new Command("Dong y", Command.OK, 1);
            pvpDeclineCmd = new Command("Tu choi", Command.CANCEL, 2);
        }
        alert.addCommand(pvpAcceptCmd);
        alert.addCommand(pvpDeclineCmd);
        alert.setCommandListener(this);
        GameMIDLet.getInstance().showScreen(alert);
    }

    public void onPvpStart(String battleId, String oppName, int myHp, int oppHp, int oppSpriteId) {
        // PVP_START already populated GameData; enter the battle screen.
        GameMIDLet.getInstance().showScreen(new BattleScreen());
    }

    public void onPong() {
        // no-op
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
