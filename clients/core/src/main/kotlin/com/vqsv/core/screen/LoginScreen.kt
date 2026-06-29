package com.vqsv.core.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.vqsv.core.VqsvGame
import com.vqsv.core.model.GameState
import com.vqsv.core.net.PacketListener

class LoginScreen(private val game: VqsvGame) : Screen, PacketListener {

    private val stage = Stage(ScreenViewport())
    private val skin: Skin
    private val font = BitmapFont()
    private lateinit var usernameField: TextField
    private lateinit var passwordField: TextField
    private lateinit var errorLabel: Label

    init {
        skin = buildSkin()
        buildUi()
        Gdx.input.inputProcessor = stage
    }

    private fun buildSkin(): Skin {
        val skin = Skin()
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.fill()
        skin.add("white", Texture(pixmap))
        pixmap.dispose()
        skin.add("default-font", font)

        val labelStyle = Label.LabelStyle(font, Color.WHITE)
        skin.add("default", labelStyle)

        val yellowStyle = Label.LabelStyle(font, Color.YELLOW)
        skin.add("title", yellowStyle)

        val redStyle = Label.LabelStyle(font, Color.RED)
        skin.add("error", redStyle)

        val tfStyle = TextField.TextFieldStyle()
        tfStyle.font = font
        tfStyle.fontColor = Color.WHITE
        tfStyle.background = skin.newDrawable("white", Color.DARK_GRAY)
        tfStyle.cursor = skin.newDrawable("white", Color.WHITE)
        tfStyle.selection = skin.newDrawable("white", Color.BLUE)
        skin.add("default", tfStyle)

        val btnStyle = TextButton.TextButtonStyle()
        btnStyle.font = font
        btnStyle.up = skin.newDrawable("white", Color.GRAY)
        btnStyle.down = skin.newDrawable("white", Color.DARK_GRAY)
        btnStyle.over = skin.newDrawable("white", Color.LIGHT_GRAY)
        btnStyle.fontColor = Color.WHITE
        skin.add("default", btnStyle)

        return skin
    }

    private fun buildUi() {
        val table = Table()
        table.setFillParent(true)
        table.center()

        val title = Label("VQSV - Vuong Quoc Sieu Vat", skin, "title")
        usernameField = TextField("", skin)
        usernameField.messageText = "Ten dang nhap"
        passwordField = TextField("", skin)
        passwordField.messageText = "Mat khau"
        passwordField.isPasswordMode = true
        passwordField.setPasswordCharacter('*')
        errorLabel = Label("", skin, "error")

        val loginBtn = TextButton("DANG NHAP", skin)
        val registerBtn = TextButton("DANG KY", skin)

        loginBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                doLogin()
            }
        })

        registerBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                doRegister()
            }
        })

        table.add(title).colspan(2).padBottom(20f).row()
        table.add(Label("Username:", skin)).left().padRight(10f)
        table.add(usernameField).width(200f).row()
        table.add(Label("Password:", skin)).left().padRight(10f)
        table.add(passwordField).width(200f).row()
        table.add(errorLabel).colspan(2).padTop(10f).row()
        table.add(loginBtn).padTop(15f).width(150f)
        table.add(registerBtn).padTop(15f).width(150f)

        stage.addActor(table)
    }

    private fun doLogin() {
        val user = usernameField.text.trim()
        val pass = passwordField.text.trim()
        if (user.isEmpty() || pass.isEmpty()) {
            errorLabel.setText("Vui long nhap day du thong tin")
            return
        }
        game.tcp.listener = this
        game.tcp.connect(game.serverHost, game.tcpPort)
        // Give connect time; sendLogin called after short delay via listener pattern
        // Actually connect is async; we send login optimistically — server will error if not ready
        Thread({
            Thread.sleep(300)
            game.tcp.sendLogin(user, pass)
        }).also { it.isDaemon = true }.start()
    }

    private fun doRegister() {
        val user = usernameField.text.trim()
        val pass = passwordField.text.trim()
        if (user.isEmpty() || pass.isEmpty()) {
            errorLabel.setText("Vui long nhap day du thong tin")
            return
        }
        game.rest.register(user, pass, user) { resp, err ->
            Gdx.app.postRunnable {
                if (err != null) {
                    errorLabel.setText("Dang ky that bai: $err")
                } else {
                    // Register success, now login via TCP
                    game.tcp.listener = this
                    game.tcp.connect(game.serverHost, game.tcpPort)
                    Thread({
                        Thread.sleep(300)
                        game.tcp.sendLogin(user, pass)
                    }).also { it.isDaemon = true }.start()
                }
            }
        }
    }

    override fun onAuthOk(token: String, level: Int, kimTien: Int, mapId: Int, posX: Int, posY: Int) {
        GameState.updateFromTcpAuth(token, level, kimTien, mapId, posX, posY)
        Gdx.app.postRunnable {
            game.setScreen(MapScreen(game))
            dispose()
        }
    }

    override fun onMoveOk(x: Int, y: Int) {}
    override fun onWildEncounter(x: Int, y: Int, battleId: String, name: String, level: Int, hp: Int, catchable: Boolean, spriteId: Int) {}
    override fun onChat(name: String, text: String) {}
    override fun onPlayerNear(playerId: Long, present: Boolean, mapId: Int, x: Int, y: Int, name: String) {}
    override fun onPvpInvite(challengerId: Long, name: String) {}
    override fun onPvpStart(battleId: String, oppName: String, myHp: Int, oppHp: Int, oppSpriteId: Int) {}
    override fun onEnemySwap(name: String, hpMax: Int, spriteId: Int) {}
    override fun onBattleTurn(playerHp: Int, enemyHp: Int, status: String, log: String) {}
    override fun onPong() {}

    override fun onError(msg: String) {
        Gdx.app.postRunnable { errorLabel.setText(msg) }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.2f, 1f)
        Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) { stage.viewport.update(width, height, true) }
    override fun show() { Gdx.input.inputProcessor = stage }
    override fun hide() {}
    override fun pause() {}
    override fun resume() {}

    override fun dispose() {
        stage.dispose()
        skin.dispose()
        font.dispose()
    }
}
