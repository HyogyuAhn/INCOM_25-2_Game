package com.club.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.club.ClubGame
import com.club.ScreenId
import com.club.ui.UIComponents
import com.club.ui.UITheme

class SettingsScreen(game: ClubGame) : BaseScreen(game) {
    override fun setupUI() {
        val root = Table().apply {
            setFillParent(true)
            center()
        }
        val panel = UIComponents.createPanel(28f)
        panel.background = UITheme.panelPlain
        val title = UIComponents.createTitleWithShadow(
            text = "설정",
            font = game.assets.titleFont,
            scale = 1.8f,
            color = UITheme.accent
        )

        val group = VerticalGroup().apply {
            space(14f)
            align(Align.center)
        }

        lateinit var debugHudBtn: TextButton
        fun refreshDebugLabel() {
            val state = if (game.debugOverlay.enabled) "켜짐" else "꺼짐"
            debugHudBtn.setText("FPS 표시: $state")
        }
        debugHudBtn = UIComponents.createModernButton("", game.font) {
            game.debugOverlay.enabled = !game.debugOverlay.enabled
            Gdx.app.log("DebugHUD", "enabled=${'$'}{game.debugOverlay.enabled}")
            refreshDebugLabel()
        }
        refreshDebugLabel()

        lateinit var cbBtn: TextButton
        fun refreshCbLabel() {
            val state = if (game.settings.colorBlindMode) "켜짐" else "꺼짐"
            cbBtn.setText("색약 보정: $state")
        }
        cbBtn = UIComponents.createModernButton("", game.font) {
            game.updateSettings { it.copy(colorBlindMode = !it.colorBlindMode) }
            refreshCbLabel()
        }
        refreshCbLabel()

        val backBtn = UIComponents.createModernButton("메인 메뉴", game.font) {
            game.router.goTo(ScreenId.MENU)
        }
        listOf(debugHudBtn, cbBtn, backBtn).forEach { it.setSize(360f, 52f) }

        group.addActor(debugHudBtn)
        group.addActor(cbBtn)
        group.addActor(backBtn)

        panel.add(title).padBottom(20f).row()
        panel.add(group)

        root.add(panel)
        stage.addActor(root)
    }

    override fun update(dt: Float) { }
    override fun draw(alpha: Float) {
        Gdx.gl.glClearColor(0.02f, 0.03f, 0.06f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    }
}

