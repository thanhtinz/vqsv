package com.vqsv.j2me.screen;

import javax.microedition.lcdui.*;
import com.vqsv.j2me.GameMIDLet;
import com.vqsv.j2me.GameData;
import com.vqsv.j2me.net.ServerConn;

public class BattleScreen extends Canvas implements CommandListener, ServerConn.ConnListener {

    private static final String[] ACTIONS = { "Tan cong", "Dung do", "Bat thu", "Bo chay" };

    // action indices map to protocol: 0=ATTACK 1=USE_ITEM 2=CATCH 3=RUN
    // ACTIONS[0]=Tan cong->0, ACTIONS[1]=Dung do->1, ACTIONS[2]=Bat thu->2, ACTIONS[3]=Bo chay->3

    private int selectedAction = 0;
    private boolean waiting = false;

    private static final int COLOR_BG       = 0x0A0A2A;
    private static final int COLOR_ENEMY_HP = 0xCC0000;
    private static final int COLOR_HP_BG    = 0x330000;
    private static final int COLOR_PLAYER_HP= 0x00BB00;
    private static final int COLOR_PHP_BG   = 0x003300;
    private static final int COLOR_TEXT     = 0xFFFFFF;
    private static final int COLOR_GOLD     = 0xFFD700;
    private static final int COLOR_SEL      = 0x4444FF;
    private static final int COLOR_BTN_BG   = 0x1A1A4A;
    private static final int COLOR_BTN_DIS  = 0x333333;
    private static final int COLOR_LOG_BG   = 0x111130;

    public BattleScreen() {
        setTitle("VQSV - Chien dau");
        setFullScreenMode(true);
        GameMIDLet.getConn().setListener(this);
        setCommandListener(this);
    }

    protected void paint(Graphics g) {
        GameData gd = GameData.getInstance();
        int w = getWidth();   // 360
        int h = getHeight();  // 640

        // Full background
        g.setColor(COLOR_BG);
        g.fillRect(0, 0, w, h);

        Font smallFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        Font boldFont  = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD,  Font.SIZE_MEDIUM);
        Font tinyFont  = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);

        // === Enemy info area y=0..40 ===
        g.setFont(boldFont);
        g.setColor(COLOR_GOLD);
        String enemyInfo = gd.enemyName + " Lv." + gd.enemyLevel + "  HP: " + gd.battleEnemyHp;
        g.drawString(enemyInfo, w / 2, 8, Graphics.HCENTER | Graphics.TOP);

        // === Enemy HP bar y=40..80 ===
        int eBarY = 40;
        int eBarH = 20;
        int eBarW = w - 20;
        // assume max enemy hp from first encounter - display proportionally, cap at max width
        int eHpW = (gd.battleEnemyHp > 0) ? Math.min(eBarW, gd.battleEnemyHp * eBarW / Math.max(gd.battleEnemyHp, 100)) : 0;
        // simple: fill bar proportional to current hp vs display max
        // We store no max, so just show raw scaled (0..w)
        int rawEHp = gd.battleEnemyHp;
        if (rawEHp > eBarW) rawEHp = eBarW;
        g.setColor(COLOR_HP_BG);
        g.fillRect(10, eBarY, eBarW, eBarH);
        g.setColor(COLOR_ENEMY_HP);
        g.fillRect(10, eBarY, rawEHp, eBarH);
        g.setColor(COLOR_TEXT);
        g.drawRect(10, eBarY, eBarW, eBarH);

        // === Battle log y=80..380 ===
        int logY = 80;
        int logH = 300;
        g.setColor(COLOR_LOG_BG);
        g.fillRect(0, logY, w, logH);
        g.setColor(0x333366);
        g.drawRect(0, logY, w - 1, logH - 1);

        g.setFont(tinyFont);
        g.setColor(COLOR_TEXT);
        String logText = gd.battleLog;
        if (logText != null && logText.length() > 0) {
            // split by \n and show last 4 lines
            String[] lines = splitLines(logText);
            int startLine = lines.length - 4;
            if (startLine < 0) startLine = 0;
            int lineH = tinyFont.getHeight() + 2;
            for (int i = startLine; i < lines.length; i++) {
                int lineY = logY + 8 + (i - startLine) * lineH;
                g.drawString(lines[i], 8, lineY, Graphics.LEFT | Graphics.TOP);
            }
        }

        // Status display
        if (gd.battleStatus != null && gd.battleStatus.length() > 0) {
            g.setFont(boldFont);
            g.setColor(COLOR_GOLD);
            g.drawString(gd.battleStatus, w / 2, logY + logH - 24, Graphics.HCENTER | Graphics.TOP);
        }

        // === Player HP bar y=380..420 ===
        int pBarY = 380;
        int pBarH = 20;
        int pBarW = w - 20;
        int pHp = gd.battlePlayerHp;
        int pHpMax = gd.hpMax;
        int pHpW = (pHpMax > 0) ? (pBarW * pHp / pHpMax) : 0;
        if (pHpW < 0) pHpW = 0;
        if (pHpW > pBarW) pHpW = pBarW;
        g.setColor(COLOR_PHP_BG);
        g.fillRect(10, pBarY, pBarW, pBarH);
        g.setColor(COLOR_PLAYER_HP);
        g.fillRect(10, pBarY, pHpW, pBarH);
        g.setColor(COLOR_TEXT);
        g.drawRect(10, pBarY, pBarW, pBarH);

        // === Pet HP text y=420..440 ===
        g.setFont(smallFont);
        g.setColor(COLOR_GOLD);
        g.drawString("Pet HP: " + gd.battlePlayerHp + "/" + gd.hpMax, 10, 422, Graphics.LEFT | Graphics.TOP);

        // === Action grid y=480..640, 2x2, each cell 160x80 ===
        int gridY = 480;
        int cellW = 160;
        int cellH = 80;
        for (int i = 0; i < 4; i++) {
            int col = i % 2;
            int row = i / 2;
            int cx = col * cellW + (w - 2 * cellW) / 2;
            int cy = gridY + row * cellH;

            // Cell background
            if (waiting) {
                g.setColor(COLOR_BTN_DIS);
            } else if (i == selectedAction) {
                g.setColor(COLOR_SEL);
            } else {
                g.setColor(COLOR_BTN_BG);
            }
            g.fillRect(cx + 2, cy + 2, cellW - 4, cellH - 4);

            // Border: blue highlight for selected
            if (i == selectedAction) {
                g.setColor(COLOR_SEL);
                g.drawRect(cx + 1, cy + 1, cellW - 2, cellH - 2);
                g.drawRect(cx, cy, cellW, cellH);
            } else {
                g.setColor(0x444488);
                g.drawRect(cx + 2, cy + 2, cellW - 4, cellH - 4);
            }

            // Action label
            g.setFont(boldFont);
            g.setColor(waiting ? 0x888888 : COLOR_TEXT);
            g.drawString(ACTIONS[i], cx + cellW / 2, cy + cellH / 2 - boldFont.getHeight() / 2,
                         Graphics.HCENTER | Graphics.TOP);
        }

        // Waiting indicator
        if (waiting) {
            g.setFont(smallFont);
            g.setColor(COLOR_GOLD);
            g.drawString("Dang cho...", w / 2, gridY - 20, Graphics.HCENTER | Graphics.TOP);
        }
    }

    protected void keyPressed(int keyCode) {
        int gameAction;
        try {
            gameAction = getGameAction(keyCode);
        } catch (Exception e) {
            gameAction = 0;
        }

        if (gameAction == UP) {
            if (selectedAction >= 2) selectedAction -= 2;
        } else if (gameAction == DOWN) {
            if (selectedAction < 2) selectedAction += 2;
        } else if (gameAction == LEFT) {
            if (selectedAction % 2 == 1) selectedAction--;
        } else if (gameAction == RIGHT) {
            if (selectedAction % 2 == 0) selectedAction++;
        } else if (gameAction == FIRE || keyCode == KEY_NUM5) {
            confirmAction();
        } else if (keyCode == KEY_NUM2) {
            if (selectedAction >= 2) selectedAction -= 2;
        } else if (keyCode == KEY_NUM8) {
            if (selectedAction < 2) selectedAction += 2;
        } else if (keyCode == KEY_NUM4) {
            if (selectedAction % 2 == 1) selectedAction--;
        } else if (keyCode == KEY_NUM6) {
            if (selectedAction % 2 == 0) selectedAction++;
        }
        repaint();
    }

    private void confirmAction() {
        if (waiting) return;
        final GameData gd = GameData.getInstance();
        final int action = selectedAction;
        waiting = true;
        repaint();
        new Thread(new Runnable() {
            public void run() {
                try {
                    GameMIDLet.getConn().sendBattleAct(gd.activeBattleId, action);
                } catch (Exception e) {
                    waiting = false;
                    onError("Loi gui lenh: " + e.getMessage());
                }
            }
        }).start();
    }

    public void commandAction(Command c, Displayable d) {
        // no extra commands needed
    }

    public void onAuthOk() { }

    public void onMoveOk(int x, int y) { }

    public void onWildEnc(int x, int y, String battleId, String name, int lvl, int hp, boolean catchable) { }

    public void onBattleTurn(int pHp, int eHp, String status, String log) {
        GameData gd = GameData.getInstance();
        gd.battlePlayerHp = pHp;
        gd.hp = pHp;
        gd.battleEnemyHp = eHp;
        gd.battleStatus = status;
        gd.battleLog = log;
        waiting = false;
        repaint();
        if ("VICTORY".equals(status) || "DEFEAT".equals(status) || "ESCAPED".equals(status)) {
            GameMIDLet.getInstance().showScreen(new MapScreen());
        }
    }

    public void onError(final String msg) {
        waiting = false;
        repaint();
        Alert alert = new Alert("Loi", msg, null, AlertType.ERROR);
        alert.setTimeout(3000);
        GameMIDLet.getInstance().showScreen(alert);
    }

    /**
     * Split a string by newline characters.
     * Avoids String.split() which is not available in CLDC 1.1.
     */
    private String[] splitLines(String text) {
        int count = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') count++;
        }
        String[] result = new String[count];
        int idx = 0;
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                result[idx++] = text.substring(start, i);
                start = i + 1;
            }
        }
        result[idx] = text.substring(start);
        return result;
    }
}
