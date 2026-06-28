package com.vqsv.j2me;

import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import com.vqsv.j2me.net.ServerConn;
import com.vqsv.j2me.screen.LoginScreen;

public class GameMIDLet extends MIDlet {

    private static GameMIDLet instance;
    private Display display;
    private ServerConn conn;

    public void startApp() {
        instance = this;
        display = Display.getDisplay(this);
        conn = new ServerConn();
        display.setCurrent(new LoginScreen());
    }

    public void pauseApp() {
        // nothing to do
    }

    public void destroyApp(boolean unconditional) {
        if (conn != null) {
            try {
                conn.disconnect();
            } catch (Exception e) {
                // ignore on destroy
            }
        }
    }

    public void showScreen(Displayable d) {
        display.setCurrent(d);
    }

    public static GameMIDLet getInstance() {
        return instance;
    }

    public static ServerConn getConn() {
        return instance.conn;
    }
}
