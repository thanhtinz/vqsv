package com.vqsv.j2me.screen;

import javax.microedition.lcdui.*;
import com.vqsv.j2me.GameMIDLet;
import com.vqsv.j2me.GameData;
import com.vqsv.j2me.net.ServerConn;

public class LoginScreen extends Form implements CommandListener, ServerConn.ConnListener {

    // Single configurable connection target. Change SERVER_HOST to your
    // deployed server's IP/hostname; "localhost" is a sensible local default.
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9090;

    private TextField tfUsername;
    private TextField tfPassword;
    private Command loginCmd;
    private Command registerCmd;

    public LoginScreen() {
        super("VQSV - Dang nhap");

        tfUsername = new TextField("Ten dang nhap:", "", 32, TextField.ANY);
        tfPassword = new TextField("Mat khau:", "", 32, TextField.PASSWORD);

        append(tfUsername);
        append(tfPassword);

        loginCmd = new Command("Dang nhap", Command.OK, 1);
        registerCmd = new Command("Dang ky", Command.SCREEN, 2);

        addCommand(loginCmd);
        addCommand(registerCmd);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == loginCmd) {
            final String username = tfUsername.getString().trim();
            final String password = tfPassword.getString();
            if (username.length() == 0 || password.length() == 0) {
                showAlert("Loi", "Vui long nhap ten va mat khau.");
                return;
            }
            final ServerConn conn = GameMIDLet.getConn();
            conn.setListener(this);
            new Thread(new Runnable() {
                public void run() {
                    try {
                        if (!conn.isConnected()) {
                            conn.connect(SERVER_HOST, SERVER_PORT);
                        }
                        conn.sendLogin(username, password);
                        GameData.getInstance().playerName = username;
                    } catch (Exception e) {
                        onError("Khong the ket noi: " + e.getMessage());
                    }
                }
            }).start();
        } else if (c == registerCmd) {
            showAlert("Dang ky", "Vui long truy cap website de dang ky tai khoan.");
        }
    }

    public void onAuthOk() {
        GameMIDLet.getInstance().showScreen(new MapScreen());
    }

    public void onMoveOk(int x, int y) {
        // not expected on login screen
    }

    public void onWildEnc(int x, int y, String battleId, String name, int lvl, int hp, boolean catchable, int spriteId) {
        // not expected on login screen
    }

    public void onBattleTurn(int pHp, int eHp, String status, String log) {
        // not expected on login screen
    }

    public void onChat(String name, String text) {
        // not expected on login screen
    }

    public void onPlayerNear(long playerId, boolean present, int mapId, int x, int y, String name) {
        // not expected on login screen
    }

    public void onPvpInvite(long challengerId, String name) {
        // not expected on login screen
    }

    public void onPvpStart(String battleId, String oppName, int myHp, int oppHp, int oppSpriteId) {
        // not expected on login screen
    }

    public void onPong() {
        // not expected on login screen
    }

    public void onError(final String msg) {
        showAlert("Loi", msg);
    }

    private void showAlert(String title, String text) {
        Alert alert = new Alert(title, text, null, AlertType.ERROR);
        alert.setTimeout(Alert.FOREVER);
        GameMIDLet.getInstance().showScreen(alert);
    }
}
