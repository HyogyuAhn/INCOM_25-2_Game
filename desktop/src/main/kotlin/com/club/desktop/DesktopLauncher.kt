package com.club.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.club.ClubGame

fun main() {
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("INCOM 25-2")
        setWindowedMode(1280, 720)
        useVsync(true)
        setForegroundFPS(60)
    }
    Lwjgl3Application(ClubGame(), config)
}

