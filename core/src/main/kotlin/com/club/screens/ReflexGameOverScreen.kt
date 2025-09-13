package com.club.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.utils.Align
import com.club.ClubGame
import com.club.ScreenId
import com.club.ui.UIComponents
import com.club.ui.UITheme

class ReflexGameOverScreen(game: ClubGame, private val finalScore: Int) : BaseScreen(game) {
    override fun setupUI() {
        val root = Table().apply {
            setFillParent(true)
            center()
        }
        val panel = UIComponents.createPanel(32f)
        panel.background = UITheme.panelBg

        val title = UIComponents.createTitleWithShadow(
            text = "게임 오버",
            font = game.assets.titleFont,
            scale = 2.4f,
            color = UITheme.accent
        )
        val scoreLabel = UIComponents.createTitleLabel("최종 점수: $finalScore", game.font).apply {
            setFontScale(1.4f)
            color = Color.WHITE
        }

        val group = VerticalGroup().apply {
            space(14f)
            align(Align.center)
        }
        val retryBtn = UIComponents.createModernButton("다시하기", game.font) {
            game.router.goTo(ScreenId.MEMORY)
        }
        val menuBtn = UIComponents.createModernButton("메인 메뉴", game.font) {
            game.router.goTo(ScreenId.MENU)
        }
        listOf(retryBtn, menuBtn).forEach { it.setSize(280f, 54f) }

        group.addActor(scoreLabel)
        group.addActor(retryBtn)
        group.addActor(menuBtn)

        panel.add(title).padBottom(16f).row()
        panel.add(group).center()

        root.add(panel)
        stage.addActor(root)
    }

    override fun update(dt: Float) {}
    override fun draw(alpha: Float) {
        Gdx.gl.glClearColor(0.04f, 0.05f, 0.09f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    }
}
