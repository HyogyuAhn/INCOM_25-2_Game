package com.club

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.club.assets.AssetRegistry
import com.club.debug.DebugOverlay
import com.club.screens.MenuScreen
import com.club.ScreenRouter
import com.club.settings.Settings
import com.club.settings.SettingsLoader
import com.club.ui.UITheme

class ClubGame : Game() {
    lateinit var spriteBatch: SpriteBatch
        private set

    lateinit var font: BitmapFont
        private set

    lateinit var assets: AssetRegistry
        private set

    lateinit var settings: Settings
        private set

    lateinit var debugOverlay: DebugOverlay
        private set

    lateinit var router: ScreenRouter
        private set

    override fun create() {
        spriteBatch = SpriteBatch()
        
        assets = AssetRegistry()
        assets.preload()
        assets.finishLoading()
        
        font = assets.koreanFont
        settings = SettingsLoader.load()
        debugOverlay = DebugOverlay(font)
        router = ScreenRouter(this)

        Gdx.app.log("ClubGame", "Settings loaded: ${'$'}settings")
        setScreen(MenuScreen(this))
    }

    fun updateSettings(mutator: (Settings) -> Settings) {
        settings = mutator(settings)
        Gdx.app.log("Settings", settings.toString())
    }

    override fun dispose() {
        screen?.dispose()
        assets.dispose()
        UITheme.dispose()
        spriteBatch.dispose()
    }
}

