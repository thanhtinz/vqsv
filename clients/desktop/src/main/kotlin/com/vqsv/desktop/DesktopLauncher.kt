package com.vqsv.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.vqsv.core.VqsvGame

fun main() {
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("VQSV - Vương Quốc Siêu Vật")
        setWindowedMode(800, 480)
        setForegroundFPS(60)
        useVsync(true)
    }
    Lwjgl3Application(VqsvGame("localhost"), config)
}
