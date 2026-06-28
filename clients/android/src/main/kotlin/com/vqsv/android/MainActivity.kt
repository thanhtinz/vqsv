package com.vqsv.android

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.vqsv.core.VqsvGame

class MainActivity : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            r = 8; g = 8; b = 8; a = 8
        }
        // "10.0.2.2" is the Android emulator's loopback to host machine
        initialize(VqsvGame(serverHost = "10.0.2.2"), config)
    }
}
