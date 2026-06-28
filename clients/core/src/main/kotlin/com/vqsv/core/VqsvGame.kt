package com.vqsv.core

import com.badlogic.gdx.Game
import com.vqsv.core.net.TcpClient
import com.vqsv.core.net.RestClient
import com.vqsv.core.screen.LoginScreen

class VqsvGame(
    val serverHost: String = "localhost",
    val tcpPort: Int = 9090,
    val restPort: Int = 8080
) : Game() {
    lateinit var tcp: TcpClient
    lateinit var rest: RestClient

    override fun create() {
        tcp = TcpClient()
        rest = RestClient("http://$serverHost:$restPort")
        setScreen(LoginScreen(this))
    }

    override fun dispose() {
        screen?.dispose()
        tcp.disconnect()
    }
}
